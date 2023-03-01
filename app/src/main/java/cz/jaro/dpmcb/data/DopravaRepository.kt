package cz.jaro.dpmcb.data

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import cz.jaro.dpmcb.data.naJihu.DetailSpoje
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
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.isActive

class DopravaRepository(
    app: Application,
) {
    companion object {
        fun String.upravit() = this
            .removePrefix("České Budějovice, ")
            .replace(Regex("[ ,-]"), "")
            .replace("SrubecTočnaMHD", "SrubecTočna")
            .replace("NáměstíPřemyslaOtakaraII.", "Nám.PřemyslaOtakaraII.")
            .replace("DobráVodauČ.Budějovic", "DobráVoda")
            .replace("KněžskéDv.", "KněžskéDvory")
            .lowercase()
    }

    private var lock = true

    init {
        app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                lock = false
            }

            override fun onActivityPaused(activity: Activity) {
                lock = true
            }
        })
    }

    private val scope = MainScope()

    private val api = DopravaApi(
        ctx = app,
        baseUrl = "https://www.dopravanajihu.cz/idspublicservices/api"
    )

    private val spojeFlow: SharedFlow<List<SpojNaMape>> = flow<List<SpojNaMape>> {
        while (currentCoroutineContext().isActive) {
            while (lock) Unit
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
                    while (lock) Unit
                    emit(api.ziskatData("/servicedetail?id=$spojId"))
                    delay(5000)
                }
            }
                .flowOn(Dispatchers.IO)
                .shareIn(
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

    fun List<SpojNaMape>.spojeDPMCB() = filter {
        it.id.drop(2).startsWith("325")
    }

    fun spojeDPMCBNaMape() =
        seznamSpojuKterePraveJedou().map { spojeNaMape ->
            spojeNaMape.spojeDPMCB()
        }

    private fun List<SpojNaMape>.spojPodleId(id: String) = find { it.id == id }

    fun List<SpojNaMape>.spojDPMCBPodleId(id: String) = spojeDPMCB().spojPodleId(id)

    private fun spojDPMCBNaMapePodleId(id: String) =
        spojeDPMCBNaMape().map { spojeNaMape ->
            spojeNaMape.spojPodleId(id)
        }

    fun spojPodleId(id: String): Flow<Pair<SpojNaMape?, DetailSpoje?>> =
        spojDPMCBNaMapePodleId(id).zip(detailSpoje(id)) { spojNaMape, detailSpoje ->
            spojNaMape to detailSpoje
        }

    @JvmName("spojPodleIdAllowNull")
    fun spojPodleId(id: String?): Flow<Pair<SpojNaMape?, DetailSpoje?>> = id?.let {
        spojDPMCBNaMapePodleId(id).zip(detailSpoje(id)) { spojNaMape, detailSpoje ->
            spojNaMape to detailSpoje
        }
    } ?: flowOf(null to null)

    fun spojNaMapePodleId(id: String): Flow<SpojNaMape?> = spojDPMCBNaMapePodleId(id)
}

