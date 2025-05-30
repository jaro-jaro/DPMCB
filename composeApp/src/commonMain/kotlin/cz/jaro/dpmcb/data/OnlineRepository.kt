package cz.jaro.dpmcb.data

import com.fleeksoft.ksoup.Ksoup
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.asRepeatingFlow
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.jikord.MapData
import cz.jaro.dpmcb.data.jikord.OnlineConn
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.jikord.OnlineTimetable
import cz.jaro.dpmcb.data.jikord.Transmitter
import cz.jaro.dpmcb.data.jikord.toOnlineConn
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

@Serializable
data class ProxyBody(
    val url: String,
    val data: String,
    val headers: Map<String, String>,
)

class OnlineRepository(
    private val repo: SpojeRepository,
    onlineManager: UserOnlineManager,
) : UserOnlineManager by onlineManager {
    private val scope = MainScope()

    private val client = HttpClient()
    private val jikordUrl = "https://mpvnet.cz/Jikord"
    private val proxyUrl = "https://ygbqqztfvcnqxxbqvxwb.supabase.co/functions/v1/cors-proxy"

    private suspend fun getAllConns() =
        if (repo.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {
            if (!isOnline()) return@withContext null
            val data = """{"w":14.320215289916973,"s":48.88092891115194,"e":14.818033283081036,"n":49.076970164143134,"zoom":12,"showStops":false}"""
            val response = try {
                client.post(proxyUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(ProxyBody(
                        url = "$jikordUrl/map/mapData",
                        data = data,
                        headers = requestHeaders,
                    )))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recordException(e)
                return@withContext null
            }

            if (response.status != HttpStatusCode.OK) return@withContext null

            val text = response.bodyAsText()
            try {
                text.fromJson<MapData>(json)
            } catch (e: SerializationException) {
                e.printStackTrace()
                recordException(e)
                null
            }
        }
            ?.transmitters
            ?.filter {
                it.cn?.startsWith("325") == true
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
        if (repo.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {

            if (!isOnline()) return@withContext null
            val result = LocationSearcher.search(
                busName = busName
            )
            when (result) {
                is LocationSearcher.SearchResult.FoundOne -> listOf(result.stopsFromStart)
                is LocationSearcher.SearchResult.FoundMore -> result.options.map { it.stopsFromStart }
                is LocationSearcher.SearchResult.NotFound,
                is LocationSearcher.SearchResult.NoData,
                is LocationSearcher.SearchResult.NoTransmitters,
                    -> null
            }
        }
        else null

    private val requestHeaders = mapOf(
        "authority" to "mpvnet.cz",
        "accept" to "application/json, text/javascript, */*; q=0.01",
        "content-type" to "application/json; charset=UTF-8",
        "origin" to "https://mpvnet.cz",
        "referer" to "https://mpvnet.cz/jikord/map",
    )

    private suspend fun getTimetable(busName: BusName) =
        if (repo.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {

            if (!isOnline()) return@withContext null
            val response = try {
                client.post(proxyUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(ProxyBody(
                        url = "$jikordUrl/mapapi/timetable",
                        data = """{"num1":"${busName.line()}","num2":"${busName.bus()}","cat":"2"}""",
                        headers = requestHeaders,
                    )))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recordException(e)
                return@withContext null
            }

            if (response.status != HttpStatusCode.OK) return@withContext null

            response.bodyAsText()
        }
            ?.ifBlank { null }
            ?.let(Ksoup::parse)
            ?.run {
                val stops = getElementsByTag("div").single()
                    .getElementsByTag("table").single()
                    .getElementsByTag("tr").drop(1)
                OnlineTimetable(
                    stops = stops.filterNot { it ->
                        it.hasClass("tAlignCentre")
                    }.map(::OnlineConnStop),
                    nextStopIndex = stops.indexOfFirst { it.hasClass("tAlignCentre") },
                )
            }
        else null

    private val connsFlow: SharedFlow<List<OnlineConn>> = ::getAllConns
        .asRepeatingFlow(5.seconds)
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.Lazily,
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