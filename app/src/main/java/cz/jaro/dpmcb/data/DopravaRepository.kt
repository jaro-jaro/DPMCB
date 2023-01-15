package cz.jaro.dpmcb.data

import android.content.Context
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.naJihu.DetailSpoje
import cz.jaro.dpmcb.data.naJihu.SpojNaMape
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
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.isActive

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

    private val spojeFlow: SharedFlow<List<SpojNaMape>> = flow<List<SpojNaMape>> {
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

    fun seznamSpojuKterePraveJedou() =
        spojeFlow

    private val detailSpojeFlowMap = mutableMapOf<String, SharedFlow<DetailSpoje?>>()

    fun detailSpoje(spojId: String) =
        detailSpojeFlowMap.getOrPut(spojId) {
            flow<DetailSpoje?> {
                while (currentCoroutineContext().isActive) {
                    emit(api.ziskatData("/servicedetail?id=$spojId"))
                    delay(5000)
                }
            }.shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )
        }

//    suspend fun seznamVsechZastavek(): List<DetailZastavky> = withContext(Dispatchers.IO) {
//        api.ziskatData("/station") ?: emptyList()
//    }
//
//    suspend fun blizkeOdjezdyZeZastavky(zastavkaId: String): List<OdjezdSpoje> = withContext(Dispatchers.IO) {
//        api.ziskatData("/station/$zastavkaId/nextservices") ?: emptyList()
//    }

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
                    listOf(
                        spojNaMape.dep.upravit() == zastavkySpoje.first().nazevZastavky.upravit(),
                        spojNaMape.dest.upravit() == zastavkySpoje.last().nazevZastavky.upravit(),
                    ).all { it }
                }
                .find { spojNaMape ->
                    listOf(
                        spojNaMape.depTime.toCas() == zastavkySpoje.first().cas,
                        spojNaMape.destTime.toCas() == zastavkySpoje.last().cas,
                    ).all { it }
                }
        }

    fun spojNaMapePodleSpojeNeboUlozenehoId(spoj: Spoj?, zastavkySpoje: List<ZastavkaSpoje>) =
        if (spoj == null) flowOf(null)
        else if (repo.idSpoju.containsKey(spoj.id)) seznamSpojuKterePraveJedou().map { spojeNaMape ->
            spojeNaMape.find {
                it.id == repo.idSpoju[spoj.id]
            }
        } else spojNaMapePodleSpoje(spoj, zastavkySpoje).onEach {
            it?.also {
                repo.idSpoju += spoj.id to it.id
            }
        }

    fun detailSpojePodleUlozenehoId(spoj: Spoj?): Flow<DetailSpoje?> =
        if (spoj == null || !repo.idSpoju.containsKey(spoj.id)) flowOf(null)
        else detailSpoje(repo.idSpoju[spoj.id]!!)

    fun spojPodleSpojeNeboUlozenehoId(spoj: Spoj?, zastavkySpoje: List<ZastavkaSpoje>): Flow<Pair<SpojNaMape?, DetailSpoje?>> =
        if (spoj == null || !repo.idSpoju.containsKey(spoj.id)) flowOf(null to null)
        else spojNaMapePodleSpojeNeboUlozenehoId(spoj, zastavkySpoje)
            .zip(detailSpoje(repo.idSpoju[spoj.id]!!)) { spojNaMape, detailSpoje ->
                spojNaMape to detailSpoje
            }

}
