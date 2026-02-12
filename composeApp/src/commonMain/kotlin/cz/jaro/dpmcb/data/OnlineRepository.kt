package cz.jaro.dpmcb.data

//import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.Ksoup
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.asRepeatingFlow
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.jikord.MapData
import cz.jaro.dpmcb.data.jikord.OnlineConn
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.jikord.OnlineTimetable
import cz.jaro.dpmcb.data.jikord.toOnlineConn
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class OnlineRepository(
    private val onlineModeManager: OnlineModeManager,
    private val repo: SpojeRepository,
) : UserOnlineManager by repo, Logger by repo, GlobalSettingsDataSource by repo {
    private val scope = MainScope()

    private val client = HttpClient()
    private val proxyUrl = "https://ygbqqztfvcnqxxbqvxwb.supabase.co/functions/v1/jikord-proxy"

    private suspend fun getAllConns() =
        if (onlineModeManager.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {
            if (!isOnline()) return@withContext null
            val response = try {
                client.get("$proxyUrl?path=/map/mapData&w=14.320215289916973&s=48.88092891115194&e=14.818033283081036&n=49.076970164143134&zoom=12&showStops=false")
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
            ?.map { it.toOnlineConn() }
            ?: emptyList()
        else emptyList()

    private val timetableFlowMap: MutableMap<BusName, SharedFlow<OnlineTimetable?>> = mutableMapOf()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

//    private suspend fun getStopIndex(busName: BusName) =
//        if (repo.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {
//
//            if (!isOnline()) return@withContext null
//            val result = LocationSearcher.search(
//                busName = busName
//            )
//            when (result) {
//                is LocationSearcher.SearchResult.FoundOne -> listOf(result.stopsFromStart)
//                is LocationSearcher.SearchResult.FoundMore -> result.options.map { it.stopsFromStart }
//                is LocationSearcher.SearchResult.NotFound,
//                is LocationSearcher.SearchResult.NoData,
//                is LocationSearcher.SearchResult.NoTransmitters,
//                    -> null
//            }
//        }
//        else null

    private suspend fun getTimetable(busName: BusName) =
        if (onlineModeManager.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {

            if (!isOnline()) return@withContext null
            val response = try {
                client.get("""$proxyUrl?path=/mapapi/timetable&num1="${busName.line()}"&num2="${busName.bus()}"&cat="2"""")
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
                    stops = stops.filterNot {
                        it.hasClass("tAlignCentre")
                    }.map(::OnlineConnStop),
                    nextStopIndex = stops.indexOfFirst { it.hasClass("tAlignCentre") }.takeUnless { it == -1 },
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

    init {
        val pushed = mutableSetOf<BusName>()
        @OptIn(ExperimentalTime::class)
        connsFlow.onEach { conns ->
            val date = SystemClock.todayHere()
            val vehiclesPerBus = conns
                .filter { it.vehicle != null }
                .associate { it.name to it.vehicle!! }
                .filterKeys { it !in pushed }
            val sequencePerBus = repo.seqOfConns(vehiclesPerBus.keys, date)
            repo.pushVehicles(
                date = date,
                vehicles = sequencePerBus.map { (bus, seq) ->
                    val vehicle = vehiclesPerBus[bus]!!
                    seq to vehicle
                }.toMap()
            )
            pushed += vehiclesPerBus.keys
        }.flowOn(Dispatchers.IO).launchIn(scope)
    }

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

//    private fun stopIndex(busName: BusName) = stopIndexFlowMap.getOrPut(busName) {
//        suspend { getStopIndex(busName) }
//            .asRepeatingFlow(5.seconds)
//            .flowOn(Dispatchers.IO)
//            .shareIn(
//                scope = scope,
//                started = SharingStarted.WhileSubscribed(),
//                replay = 1
//            )
//    }

    fun bus(name: BusName) =
        combine(onlineBus(name), timetable(name)) { onlineConn, timetable ->
            onlineConn to timetable
        }

    fun onlineBus(name: BusName) = nowRunningBuses().map { it.onlineBus(name) }
}

fun List<OnlineConn>.onlineBus(name: BusName) = find { it.name == name }