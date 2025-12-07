package cz.jaro.dpmcb.ui.connection_results

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.CommonConnectionSearcher
import cz.jaro.dpmcb.data.Connection
import cz.jaro.dpmcb.data.DirectConnectionSearcher
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
import cz.jaro.dpmcb.data.helperclasses.mutate
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.ui.common.toLocalTime
import cz.jaro.dpmcb.ui.connection.toConnectionDefinition
import cz.jaro.dpmcb.ui.connection_search.Favourite
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.atDate
import kotlinx.datetime.atTime
import kotlin.time.Duration.Companion.minutes

private const val loadStep = 5

class ConnectionResultsViewModel(
    private val repo: SpojeRepository,
    args: Route.ConnectionResults,
) : ViewModel() {
    lateinit var navigator: Navigator

    private val relations = args.relations
    private val showInefficientConnections = args.showInefficientConnections
    private val datetime = args.date.atTime(args.time.toLocalTime())
    private val results = MutableStateFlow(emptyList<Connection>())
    private val loading = MutableStateFlow(false)
    private val loadingPast = MutableStateFlow(false)
    private val loaded = MutableStateFlow(relations.value.map { 0 }.toMutableList())
    private val loadedBack = MutableStateFlow(relations.value.map { 0 }.toMutableList())

    val searchers = async {
        args.relations.value.map {
            (if (args.directOnly) DirectConnectionSearcher else CommonConnectionSearcher)(
                start = it.start,
                destination = it.destination,
                datetime = datetime,
                repo = repo,
            )
        }
    }

    fun loadMore() {
        loading.value = true
        launch(Dispatchers.IO) {
            searchers.await().forEachIndexed { i, searcher ->
                searcher.search(
                    firstOffset = loaded.value[i],
                    count = loadStep,
                ).collect { connection ->
                    loaded.update {
                        it[i] += 1
                        it
                    }
                    if (connection != null)
                        results.value = (results.value + listOf(connection)).tidy()
                }
            }
            loading.value = false
        }
    }

    fun List<Connection>.tidy(): List<Connection> = distinctBy { connection ->
        connection.map { it.bus }
    }.let { results ->
        if (showInefficientConnections) results
        else results.groupBy { it.last().arrival to it.last().endStop }.map { (_, connections) ->
            connections.maxBy { it.first().departure }
        }
    }.sortedWith(compareBy<Connection> { it.first().departure }.thenBy { it.last().arrival })

    fun loadPast() {
        loadingPast.value = true
        launch(Dispatchers.IO) {
            searchers.await().forEachIndexed { i, searcher ->
                searcher.searchBack(
                    firstOffset = loadedBack.value[i],
                    count = loadStep,
                ).collect { connection ->
                    loadedBack.update {
                        it[i] += 1
                        it
                    }
                    if (connection != null)
                        results.value = (listOf(connection) + results.value).tidy()
                }
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
            Route.Connection(def = e.def)
        )

        ConnectionResultsEvent.AddToFavourites -> repo.changeFavourites {
            listOf(relations.value) + it
        }

        is ConnectionResultsEvent.AddToOtherFavourite -> repo.changeFavourites {
            it.toMutableList().mutate {
                this[e.i] += relations.value
            }.distinct()
        }

        ConnectionResultsEvent.RemoveFromFavourites -> repo.changeFavourites {
            it - setOf(relations.value)
        }

        is ConnectionResultsEvent.RemoveFromOtherFavourite -> repo.changeFavourites {
            it.toMutableList().mutate {
                this[e.i] -= relations.value.single()
            }.distinct()
        }
    }

    val filteredResults = results/*.mapState { connections ->
        connections
    }*/

    val buses = filteredResults.mapState { connections ->
        connections.flatMap { parts ->
            parts.map { it.bus to it.date }
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
                departure = start.departure.atDate(start.date),
                arrival = end.arrival.atDate(end.date),
                startStop = start.startStop,
                endStop = end.endStop,
                transfers = parts.drop(1).map { it.startStop },
                def = parts.toConnectionDefinition()
            )
        }
    }

    val state = combineStates(
        connections, loading, loadingPast, repo.favourites
    ) { connections, loading, loadingPast, favourites ->
        val isComposed = relations.value.size > 1
        val relation = relations.value.first()
        val isFavourite = relations.value in favourites
        val (partOf, other) =
            if (isComposed) listOf<IndexedValue<Favourite>>() to listOf()
            else favourites.withIndex().partition { relation in it.value }
        ConnectionResultState(
            relations = relations,
            datetime = datetime,
            results = connections,
            loading = loading,
            loadingPast = loadingPast,
            isFavourite = isFavourite,
            showAdd = !isFavourite,
            showRemove = if (isComposed) isFavourite else isFavourite && partOf.size == 1,
            partOf = partOf,
            other = other,
        )
    }
}