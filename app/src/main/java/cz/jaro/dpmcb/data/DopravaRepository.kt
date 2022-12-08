package cz.jaro.dpmcb.data

import android.content.Context
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.naJihu.DetailSpoje
import cz.jaro.dpmcb.data.naJihu.DetailZastavky
import cz.jaro.dpmcb.data.naJihu.OdjezdSpoje
import cz.jaro.dpmcb.data.naJihu.SpojNaMape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DopravaRepository(
    ctx: Context,
) {

    private val api = DopravaApi(
        ctx = ctx,
        baseUrl = "https://www.dopravanajihu.cz/idspublicservices/api"
    )

    suspend fun seznamSpojuKterePraveJedou(): List<SpojNaMape> = withContext(Dispatchers.IO) {
        api.ziskatData("/service/position") ?: emptyList()
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

    suspend fun spojNaMapePodleSpoje(spoj: Spoj, zastavkySpoje: List<ZastavkaSpoje>) =
        seznamSpojuKterePraveJedou().filter {
            it.id.drop(2).startsWith("325")
        }.filter {
            it.id.endsWith(spoj.cisloLinky.toString())
        }.filter { spojNaMape ->
            listOf(
                spojNaMape.dep.removePrefix("České Budějovice, ").lowercase() == zastavkySpoje.first().nazevZastavky.lowercase(),
                spojNaMape.dest.removePrefix("České Budějovice, ").lowercase() == zastavkySpoje.last().nazevZastavky.lowercase(),
            ).all { it }
        }.find { spojNaMape ->
            listOf(
                spojNaMape.depTime.toCas() == zastavkySpoje.first().cas,
                spojNaMape.destTime.toCas() == zastavkySpoje.last().cas,
            ).all { it }
        }

    suspend fun detailSpojePodleSpoje(spoj: Spoj, zastavkySpoje: List<ZastavkaSpoje>) =
        spojNaMapePodleSpoje(spoj = spoj, zastavkySpoje = zastavkySpoje)?.let { detailSpoje(it.id) }

    suspend fun spojNaMapePodleSpojeNeboUlozenehoId(spoj: Spoj, zastavkySpoje: List<ZastavkaSpoje>): SpojNaMape? {
        return if (repo.idSpoju.containsKey(spoj.id)) {
            seznamSpojuKterePraveJedou().find {
                it.id === repo.idSpoju[spoj.id]
            }
        } else {
            spojNaMapePodleSpoje(spoj, zastavkySpoje)?.also {
                repo.idSpoju += spoj.id to it.id
            }
        }
    }

    suspend fun detailSpojePodleSpojeNeboUlozenehoId(spoj: Spoj, zastavkySpoje: List<ZastavkaSpoje>) =
        spojNaMapePodleSpojeNeboUlozenehoId(spoj = spoj, zastavkySpoje = zastavkySpoje)?.let { detailSpoje(it.id) }
}
