package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.compare
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.presneTed
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCas
import cz.jaro.dpmcb.data.naJihu.DetailSpoje
import cz.jaro.dpmcb.data.naJihu.SpojNaMape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.LocalDate

@Single
class DopravaRepository(
    private val repo: SpojeRepository,
    private val api: DopravaApi,
) {
    private val scope = MainScope()

    private val spojeFlow: SharedFlow<List<SpojNaMape>> = flow<List<SpojNaMape>> {
        while (currentCoroutineContext().isActive) {
            emit(
                if (repo.onlineMod.value && repo.datum.value == LocalDate.now())
                    api.ziskatData("/service/position") ?: emptyList()
                else emptyList()
            )
            delay(5000)
        }
    }
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    fun seznamSpojuKterePraveJedou() =
        spojeFlow

    private val detailSpojeFlowMap = mutableMapOf<String, SharedFlow<DetailSpoje?>>()

    fun detailSpoje(spojId: String) =
        detailSpojeFlowMap.getOrPut(spojId) {
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(
                        (if (repo.onlineMod.value && repo.datum.value == LocalDate.now())
                            api.ziskatData<DetailSpoje?>("/servicedetail?id=$spojId")
                        else null) to presneTed
                    )
                    delay(5000)
                }
            }
                .compare(null to presneTed) { (minule, _), (nove, ted) ->
                    when {
                        nove == null -> minule
                        minule == null -> nove
                        else -> {
                            spojDPMCBNaMapePodleId(nove.id).first() ?: return@compare null to ted

                            val indexMinulePristiZastavky = minule.stations.indexOfFirst { !it.passed }
                            val indexNovePristiZastavky = nove.stations.indexOfFirst { !it.passed }
                            when {
                                indexMinulePristiZastavky == indexNovePristiZastavky -> nove.copy(
                                    realneZpozdeni = minule.realneZpozdeni
                                )

                                indexNovePristiZastavky < 1 -> nove.copy(
                                    realneZpozdeni = minule.realneZpozdeni
                                )

                                else -> nove.stations[indexNovePristiZastavky - 1].let { praveOdhlasenaZastavka ->
                                    nove.copy(
                                        realneZpozdeni = Duration.between(praveOdhlasenaZastavka.departureTime.toCas(), ted).seconds / 60F
                                    )
                                }
                            }
                        }
                    } to ted
                }
                .map {
                    it.first
                }
                .flowOn(Dispatchers.IO)
                .shareIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(),
                    replay = 1
                )
        }

//    suspend fun seznamVsechZastavek(): List<DetailZastavky> = withContext(Dispatchers.IO) {
//       if (repo.onlineMod.value) api.ziskatData("/station") ?: emptyList() else emptyList()
//    }
//
//    suspend fun blizkeOdjezdyZeZastavky(zastavkaId: String): List<OdjezdSpoje> = withContext(Dispatchers.IO) {
//       if (repo.onlineMod.value) api.ziskatData("/station/$zastavkaId/nextservices") ?: emptyList() else emptyList()
//    }

    fun spojeDPMCBNaMape() =
        seznamSpojuKterePraveJedou().map { spojeNaMape ->
            spojeNaMape.spojeDPMCB()
        }

    private fun spojDPMCBNaMapePodleId(id: String) =
        spojeDPMCBNaMape().map { spojeNaMape ->
            spojeNaMape.spojPodleId(id)
        }

    fun spojPodleId(id: String): Flow<Pair<SpojNaMape?, DetailSpoje?>> =
        spojDPMCBNaMapePodleId(id).combine(detailSpoje(id)) { spojNaMape, detailSpoje ->
            spojNaMape to detailSpoje
        }

    @JvmName("spojPodleIdAllowNull")
    fun spojPodleId(id: String?): Flow<Pair<SpojNaMape?, DetailSpoje?>> = id?.let {
        spojDPMCBNaMapePodleId(id).combine(detailSpoje(id)) { spojNaMape, detailSpoje ->
            spojNaMape to detailSpoje
        }
    } ?: flowOf(null to null)

    fun spojNaMapePodleId(id: String): Flow<SpojNaMape?> = spojDPMCBNaMapePodleId(id)
}


fun List<SpojNaMape>.spojeDPMCB() = filter {
    it.id.drop(2).startsWith("325")
}

private fun List<SpojNaMape>.spojPodleId(id: String) = find { it.id == id }

fun List<SpojNaMape>.spojDPMCBPodleId(id: String) = spojeDPMCB().spojPodleId(id)