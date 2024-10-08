package cz.jaro.dpmcb.data

import LocationSearcher
import android.content.Context
import com.gitlab.mvysny.konsumexml.KonsumerException
import com.gitlab.mvysny.konsumexml.konsumeXml
import com.gitlab.mvysny.konsumexml.textRecursively
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asRepeatingFlow
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.jikord.MapData
import cz.jaro.dpmcb.data.jikord.OnlineConn
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.jikord.OnlineTimetable
import cz.jaro.dpmcb.data.jikord.Transmitter
import cz.jaro.dpmcb.data.jikord.toOnlineConn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.intellij.lang.annotations.Language
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class OnlineRepository(
    private val repo: SpojeRepository,
    private val ctx: Context,
    private val onlineApi: OnlineApi,
) {
    private val scope = MainScope()

    private suspend fun getAllConns() =
        if (repo.isOnlineModeEnabled.value && repo.date.value == SystemClock.todayHere()) withContext(Dispatchers.IO) {
            if (!ctx.isOnline) return@withContext null
            val data = """{"w":14.320215289916973,"s":48.88092891115194,"e":14.818033283081036,"n":49.076970164143134,"zoom":12,"showStops":false}"""
            val response = try {
                onlineApi.mapData(
                    headers = headers,
                    body = data.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
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
            ?.map(Transmitter::toOnlineConn)
            ?: emptyList()
        else emptyList()

    private val stopIndexFlowMap: MutableMap<BusName, SharedFlow<List<Double>?>> = mutableMapOf()

    private val timetableFlowMap: MutableMap<BusName, SharedFlow<OnlineTimetable?>> = mutableMapOf()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private suspend fun getStopIndex(busName: BusName) =
        if (repo.isOnlineModeEnabled.value && repo.date.value == SystemClock.todayHere()) withContext(Dispatchers.IO) {

            if (!ctx.isOnline) return@withContext null
            val result = LocationSearcher.search(
                busName = busName
            )
            when (result) {
                is LocationSearcher.SearchResult.FoundOne -> listOf(result.stopsFromStart)
                is LocationSearcher.SearchResult.FoundMore -> result.options.map { it.stopsFromStart }
                is LocationSearcher.SearchResult.NotFound -> null
                is LocationSearcher.SearchResult.NoData -> null
                is LocationSearcher.SearchResult.NoTransmitters -> null
                else -> error("Unknown search result: $result")
            }
        }
        else null

    private val headers = mapOf(
        "authority" to "mpvnet.cz",
        "accept" to "application/json, text/javascript, */*; q=0.01",
        "content" to "type: application/json; charset=UTF-8",
        "origin" to "https://mpvnet.cz",
        "referer" to "https://mpvnet.cz/jikord/map",
    )

    private suspend fun getTimetable(busName: BusName) =
        if (repo.isOnlineModeEnabled.value && repo.date.value == SystemClock.todayHere()) withContext(Dispatchers.IO) {

            if (!ctx.isOnline) return@withContext null
            val response = try {
                onlineApi.timetable(
                    headers = headers,
                    body = json("""{"num1":"${busName.line()}","num2":"${busName.bus()}","cat":"2"}""")
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Firebase.crashlytics.recordException(e)
                return@withContext null
            }

            if (response.code() != 200) return@withContext null

            val text = response.body() ?: return@withContext null

            text.string()
        }
            ?.ifBlank { null }
            //                        ?.replace("<tr class=\"tAlignCentre\"><td>&darr;</td><td><hr></td><td><hr></td><td><hr></td></tr>", "")
            ?.replace("&darr;", "")
            ?.replace("<hr>", "")
            ?.konsumeXml()
            ?.run {
                try {
                    var line = null as Int?
                    OnlineTimetable(
                        stops = child("div") {
                            child("table") {
                                child("tr") {
                                    childrenText("th")
                                }
                                var i = -1
                                children("tr") {
                                    i++
                                    if (attributes.getValueOrNull("class") == "tAlignCentre") {
                                        line = i
                                        textRecursively()
                                        null
                                    } else
                                        OnlineConnStop()
                                }.filterNotNull()
                            }
                        },
                        nextStopIndex = line,
                    )
                } catch (e: KonsumerException) {
                    e.printStackTrace()
                    Firebase.crashlytics.recordException(e)
                    null
                }
            }
        else null

    private fun json(@Language("json") text: String) = text

    private val connsFlow: SharedFlow<List<OnlineConn>> = ::getAllConns
        .asRepeatingFlow(5.seconds)
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    fun nowRunningBuses() =
        connsFlow

    private fun timetable(busName: BusName) = timetableFlowMap.getOrPut(busName) {
        suspend { getTimetable(busName) }
            .asRepeatingFlow(5.seconds)
            .flowOn(Dispatchers.IO)
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )
    }

    private fun stopIndex(busName: BusName) = stopIndexFlowMap.getOrPut(busName) {
        suspend { getStopIndex(busName) }
            .asRepeatingFlow(5.seconds)
            .flowOn(Dispatchers.IO)
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )
    }

    fun bus(name: BusName) =
        combine(onlineBus(name), timetable(name), stopIndex(name)) { onlineConn, timetable, stopIndex ->
            Triple(onlineConn, timetable, stopIndex)
        }

    fun onlineBus(name: BusName) = nowRunningBuses().map { it.onlineBus(name) }
}

fun List<OnlineConn>.onlineBus(name: BusName) = find { it.name == name }