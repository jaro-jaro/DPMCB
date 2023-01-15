package cz.jaro.dpmcb.data

import android.content.Context
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.naJihu.DetailSpoje
import cz.jaro.dpmcb.data.naJihu.DetailZastavky
import cz.jaro.dpmcb.data.naJihu.OdjezdSpoje
import cz.jaro.dpmcb.data.naJihu.SpojNaMape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class DopravaRepository(
    ctx: Context,
) {
    companion object {
        fun String.upravit() = this
            .removePrefix("České Budějovice, ")
            .replace(Regex("[ ,-]"), "")
            .replace("SrubecTočnaMHD", "SrubecTočna")
            .replace("NáměstíPřemyslaOtakaraII.", "Nám.PřemyslaOtakaraII.")
            .replace("DobráVodauČ.BudějovicTočna", "DobráVodaTočna")
            .replace("KněžskéDv.", "KněžskéDvory")
            .lowercase()
    }

    private val scope = MainScope()

    private val api = DopravaApi(
        ctx = ctx,
        baseUrl = "https://www.dopravanajihu.cz/idspublicservices/api"
    )

    private var spojeFlow: SharedFlow<List<SpojNaMape>> = flow<List<SpojNaMape>> {
        while (currentCoroutineContext().isActive) {
            emit(api.ziskatData("/service/position") ?: emptyList())
            delay(5000)
        }
    }
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    fun seznamSpojuKterePraveJedou(): Flow<List<SpojNaMape>> {
        return spojeFlow
    }

    suspend fun detailSpoje(spojId: String): DetailSpoje? = withContext(Dispatchers.IO) {
        api.ziskatData("/servicedetail?id=$spojId")
    }

    suspend fun seznamVsechZastavek(): List<DetailZastavky> = withContext(Dispatchers.IO) {
        api.ziskatData("/station") ?: emptyList()
    }

    suspend fun blizkeOdjezdyZeZastavky(zastavkaId: String): List<OdjezdSpoje> = withContext(Dispatchers.IO) {
        api.ziskatData("/station/$zastavkaId/nextservices") ?: emptyList()
    }

    private fun spojNaMapePodleSpoje(spoj: Spoj, zastavkySpoje: List<ZastavkaSpoje>) =
        seznamSpojuKterePraveJedou().map { spojeNaMape ->
            spojeNaMape
                .filter {
                    it.id.drop(2).startsWith("325")
                }
                .filter {
                    it.id.split("-")[1].endsWith(spoj.cisloLinky.toString())
                }
                .filter { spojNaMape ->
                    println(
                        listOf(
                            spojNaMape.dep.upravit(), zastavkySpoje.first().nazevZastavky.upravit(),
                            spojNaMape.dest.upravit(), zastavkySpoje.last().nazevZastavky.upravit(),
                        )
                    )
                    listOf(
                        spojNaMape.dep.upravit() == zastavkySpoje.first().nazevZastavky.upravit(),
                        spojNaMape.dest.upravit() == zastavkySpoje.last().nazevZastavky.upravit(),
                    ).all { it }
                }
                .find { spojNaMape ->
                    println(
                        listOf(
                            spojNaMape.depTime.toCas(), zastavkySpoje.first().cas,
                            spojNaMape.destTime.toCas(), zastavkySpoje.last().cas,
                        )
                    )
                    listOf(
                        spojNaMape.depTime.toCas() == zastavkySpoje.first().cas,
                        spojNaMape.destTime.toCas() == zastavkySpoje.last().cas,
                    ).all { it }
                }
        }

    fun spojNaMapePodleSpojeNeboUlozenehoId(spoj: Spoj?, zastavkySpoje: List<ZastavkaSpoje>) =
        if (spoj == null) flowOf(null)
        else if (repo.idSpoju.containsKey(spoj.id)) {
            seznamSpojuKterePraveJedou().map { spojeNaMape ->
                spojeNaMape.find {
                    it.id == repo.idSpoju[spoj.id]
                }
            }
        } else {
            spojNaMapePodleSpoje(spoj, zastavkySpoje.reversedIf { spoj.smer == Smer.NEGATIVNI }).onEach {
                it?.also {
                    repo.idSpoju += spoj.id to it.id
                }
            }
        }

    fun detailSpojePodleSpojeNeboUlozenehoId(spoj: Spoj?, zastavkySpoje: List<ZastavkaSpoje>): Flow<DetailSpoje?> =
        if (spoj == null) flowOf(null)
        else if (repo.idSpoju.containsKey(spoj.id)) {
            flow {
                emit(detailSpoje(repo.idSpoju[spoj.id]!!))
                delay(5000)
            }

        } else {
            spojNaMapePodleSpoje(spoj, zastavkySpoje.reversedIf { spoj.smer == Smer.NEGATIVNI }).map {
                it?.let {
                    repo.idSpoju += spoj.id to it.id
                    detailSpoje(it.id)
                }
            }
        }


    fun spojPodleSpojeNeboUlozenehoId(spoj: Spoj?, zastavkySpoje: List<ZastavkaSpoje>): Flow<Pair<SpojNaMape?, DetailSpoje?>> =
        if (spoj == null) flowOf(null to null)
        else if (repo.idSpoju.containsKey(spoj.id)) {
            seznamSpojuKterePraveJedou().map { spojeNaMaoe ->
                spojeNaMaoe.find {
                    it.id == repo.idSpoju[spoj.id]
                } to detailSpoje(repo.idSpoju[spoj.id]!!)
            }
        } else {
            spojNaMapePodleSpoje(spoj, zastavkySpoje.reversedIf { spoj.smer == Smer.NEGATIVNI }).map {
                it?.let {
                    repo.idSpoju += spoj.id to it.id
                    it to detailSpoje(it.id)
                } ?: (null to null)
            }
        }

}
