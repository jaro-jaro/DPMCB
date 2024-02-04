package cz.jaro.dpmcb.data

import android.content.Context
import android.util.Log
import com.gitlab.mvysny.konsumexml.KonsumerException
import com.gitlab.mvysny.konsumexml.konsumeXml
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.jikord.MapData
import cz.jaro.dpmcb.data.jikord.OnlineSpoj
import cz.jaro.dpmcb.data.jikord.SpojNaMape
import cz.jaro.dpmcb.data.jikord.ZastavkaOnlineSpoje
import cz.jaro.dpmcb.data.jikord.toOnlineSpoj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.RequestBody
import org.koin.core.annotation.Single
import java.time.LocalDate

@Single
class DopravaRepository(
    private val repo: SpojeRepository,
    private val ctx: Context,
    private val dopravaApi: DopravaApi,
) {
    private val scope = MainScope()

    private val spojeFlow: SharedFlow<List<OnlineSpoj>> = flow {
        while (currentCoroutineContext().isActive) {
            emit((
                if (repo.onlineMod.value && repo.datum.value == LocalDate.now()) withContext(Dispatchers.IO) {
                    if (!ctx.isOnline) return@withContext null
                    val data = """{"w":14.320215289916973,"s":48.88092891115194,"e":14.818033283081036,"n":49.076970164143134,"zoom":12,"showStops":false}"""
                    val response = try {
                        dopravaApi.mapData(
                            headers = headers,
                            body = RequestBody.create(
                                MediaType.parse("application/json; charset=utf-8"),
                                data
                            ),
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Firebase.crashlytics.recordException(e)
                        return@withContext null
                    }

                    if (response.code() != 200) return@withContext null

                    val text = response.body() ?: return@withContext null
                    try {
                        json.decodeFromString<MapData>(text.string())
                    } catch (e: SerializationException) {
                        e.printStackTrace()
                        Firebase.crashlytics.recordException(e)
                        null
                    }
                }
                    ?.transmitters
                    ?.filter {
                        it.cn?.startsWith("325") ?: false
                    }
                    ?.map(SpojNaMape::toOnlineSpoj)
                    ?: emptyList()
                else emptyList()
            ))
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

    private val detailSpojeFlowMap = mutableMapOf<String, SharedFlow<List<ZastavkaOnlineSpoje>?>>()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val headers = mapOf(
        "authority" to "jih.mpvnet.cz",
        "accept" to "application/json, text/javascript, */*; q=0.01",
        "content" to "type: application/json; charset=UTF-8",
        "origin" to "https://jih.mpvnet.cz",
        "referer" to "https://jih.mpvnet.cz/jikord/map",
    )

    private fun detailSpoje(spojId: String) = detailSpojeFlowMap.getOrPut(spojId) {
        flow {
            while (currentCoroutineContext().isActive) {
                emit(
                    if (repo.onlineMod.value && repo.datum.value == LocalDate.now()) withContext(Dispatchers.IO) {

                        if (!ctx.isOnline) return@withContext null
                        val response = try {
                            dopravaApi.timetable(
                                headers = headers,
                                body = RequestBody.create(
                                    MediaType.parse("application/json; charset=utf-8"),
                                    """{"num1":"${spojId.split("-")[1]}","num2":"${spojId.split("-")[2]}","cat":"2"}"""
                                ),
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Firebase.crashlytics.recordException(e)
                            return@withContext null
                        }

                        Log.d("Doprava API", ": ${response.code()} ${response.message()}")

                        if (response.code() != 200) return@withContext null

                        val text = response.body() ?: return@withContext null

                        text.string()
                    }
                        ?.ifBlank { null }
                        ?.replace("<tr class=\"tAlignCentre\"><td>&darr;</td><td><hr></td><td><hr></td><td><hr></td></tr>", "")
                        ?.konsumeXml()
                        ?.run {
                            try {
                                child("div") {
                                    child("table") {
                                        child("tr") {
                                            childrenText("th")
                                        }
                                        children("tr") {
                                            ZastavkaOnlineSpoje()
                                        }.filterNotNull()
                                    }
                                }
                            } catch (e: KonsumerException) {
                                e.printStackTrace()
                                Firebase.crashlytics.recordException(e)
                                null
                            }
                        }
                    else null
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
    }

    fun spojPodleId(id: String) =
        spojNaMapePodleId(id).combine(detailSpoje(id)) { spojNaMape, detailSpoje ->
            spojNaMape to detailSpoje
        }

    fun spojNaMapePodleId(id: String) = seznamSpojuKterePraveJedou().map { it.spojNaMapePodleId(id) }
}

fun List<OnlineSpoj>.spojNaMapePodleId(id: String) = find { it.id == id }