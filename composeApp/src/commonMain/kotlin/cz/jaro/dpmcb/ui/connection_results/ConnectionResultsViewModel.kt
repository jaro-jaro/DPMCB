package cz.jaro.dpmcb.ui.connection_results

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.Connection
import cz.jaro.dpmcb.data.ConnectionSearcher
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.helperclasses.async
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.isTypeOf
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.ui.connection.toConnectionDefinition
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.minutes

private const val loadStep = 5

class ConnectionResultsViewModel(
    private val repo: SpojeRepository,
    args: Route.ConnectionResults,
) : ViewModel() {
    lateinit var navigator: Navigator

    private val settings = args.settings
    private val results = MutableStateFlow(emptyList<Connection>())
    private val loading = MutableStateFlow(false)
    private val loadingPast = MutableStateFlow(false)
    private val loaded = MutableStateFlow(0)
    private val loadedBack = MutableStateFlow(0)

    val searcher = async {
        ConnectionSearcher(
            settings = settings,
            repo = repo,
        )
    }

    fun loadMore() {
        loading.value = true
        launch(Dispatchers.IO) {
            searcher.await()
                .search(
                    firstOffset = loaded.value,
                    count = loadStep,
                )
                .collect { connection ->
                    loaded.value += 1
                    if (connection != null)
                        results.value = (results.value + listOf(connection)).tidy()
                }
            loading.value = false
        }
    }

    fun List<Connection>.tidy(): List<Connection> = distinctBy { connection ->
        connection.map { it.bus }
    }.let { results ->
        if (settings.showInefficientConnections) results
        else results.groupBy { it.last().arrival }.map { (_, connections) ->
            connections.maxBy { it.first().departure }
        }
    }.sortedWith(compareBy<Connection> { it.first().departure }.thenBy { it.last().arrival })

    fun loadPast() {
        loadingPast.value = true
        launch(Dispatchers.IO) {
            searcher.await()
                .searchBack(
                    firstOffset = loadedBack.value,
                    count = loadStep,
                )
                .collect { connection ->
                    loadedBack.value += 1
                    if (connection != null)
                        results.value = (listOf(connection) + results.value).tidy()
                }
            loadingPast.value = false
        }
    }

    init {
        loadMore()
    }

    fun onEvent(e: ConnectionResultsEvent) = when (e) {
        is ConnectionResultsEvent.LoadPast -> {
            if (loadingPast.value) Unit
            else loadPast()
        }

        is ConnectionResultsEvent.LoadMore -> {
            if (loading.value) Unit
            else loadMore()
        }

        is ConnectionResultsEvent.SelectConnection -> navigator.navigate(
            Route.Connection(date = e.startDate, def = e.def)
        )
    }

    val filteredResults = results/*.mapState { connections ->
        connections
    }*/

    val buses = filteredResults.mapState { connections ->
        connections.flatMap { parts ->
            parts.map { it.bus to it.departure.date }
        }.groupBy({ it.second }, { it.first }).mapValues { (date, buses) ->
            buses.toSet()
        }
    }

    val connections = filteredResults.mapState { connections ->
        connections.map { parts ->
            val start = parts.first()
            val end = parts.last()
            ConnectionResult(
                parts = parts.windowed(2, partialWindows = true) { parts ->
                    val part = parts[0]
                    val nextPart = parts.getOrNull(1)
                    val lineTraction = part.vehicleType?.let { repo.lineTraction(part.bus.line(), it) }
                    val transferTime = nextPart?.departure?.let { it - part.arrival }
                    val samePlatforms = nextPart?.departurePlatform == part.arrivalPlatform
                    ConnectionResultBus(
                        line = part.bus.shortLine(),
                        isTrolleybus = lineTraction?.isTypeOf(Traction.Trolleybus) ?: false,
                        transferTime = transferTime,
                        length = part.arrival - part.departure,
                        transferTight = transferTime != null &&
                                (samePlatforms && transferTime < 1.minutes || !samePlatforms && transferTime < 2.minutes),
                        transferLong = transferTime != null && transferTime >= 15.minutes,
                    )
                },
                length = end.arrival - start.departure,
                departure = start.departure,
                arrival = end.arrival,
                startStop = start.startStop,
                endStop = end.endStop,
                transfers = parts.drop(1).map { it.startStop },
                def = parts.toConnectionDefinition()
            )
        }
    }

    val state = combineStates(
        connections, loading, loadingPast
    ) { connections, loading, loadingPast ->
        ConnectionResultState(
            settings = settings,
            results = connections,
            loading = loading,
            loadingPast = loadingPast,
        )
    }
}