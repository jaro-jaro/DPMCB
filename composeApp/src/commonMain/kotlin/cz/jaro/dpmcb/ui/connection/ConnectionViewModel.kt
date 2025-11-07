package cz.jaro.dpmcb.ui.connection

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.ConnectionSearcher
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.helperclasses.async
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.middleDestination
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.data.realtions.connection.ConnectionBusInfo
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
import cz.jaro.dpmcb.ui.connection_search.SearchSettings
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atDate
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class ConnectionViewModel(
    private val repo: SpojeRepository,
    args: Route.Connection,
) : ViewModel() {
    lateinit var navigator: Navigator

    private val definition: MutableStateFlow<AlternativesDefinition> = MutableStateFlow(
        AlternativesDefinition(args.def.toTreeDefinition())
    )

    private val coordinates: MutableStateFlow<Coordinates> = MutableStateFlow(args.def.map { 0 })

    private suspend fun addConnection(
        newPage: Int,
        parentCoordinates: Coordinates,
        def: ConnectionDefinition,
    ) {
        loadInfo(def)
        val newTree = def.toTreeDefinition()
        val rest = newTree.next.getCoordinatesOfFirstConnection()
        definition.update {
            it.addAt(parentCoordinates, newTree)
        }
        coordinates.value = parentCoordinates + newPage + rest
    }

    private val info =
        MutableStateFlow(emptyMap<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>())

    private suspend fun loadInfo(
        def: ConnectionDefinition,
    ) {
        def.groupBy { it.date }.mapValues { (date, buses) ->
            val busNames = buses.map { it.busName }.toSet()
            val new = busNames - info.value.keys
            val values = repo.connectionBusInfo(new, date).map {
                it.key.connName to it
            }
            info.update { it + values }
        }
    }

    init {
        launch {
            loadInfo(args.def)
        }
    }

    private val searcher = async {
        info.map { info ->
            if (info.keys.isEmpty()) return@map null

            val def = args.def
            val first = def.first()
            val last = def.last()
            val (firstBus, firstStops) = info.getValue(first.busName)
            val (lastBus, lastStops) = info.getValue(last.busName)
            val start = firstStops[first.start]
            val end = lastStops[last.end]
            val settings = SearchSettings(
                start = start.name,
                destination = end.name,
                directOnly = false,
                showInefficientConnections = true,
                datetime = start.departure!!.atDate(first.date),
            )
            return@map ConnectionSearcher(settings, repo)
        }.filterNotNull().first()
    }

    private val alternatives = combine(definition, info, ::createAlternatives)
        .stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), Alternatives())

    @OptIn(ExperimentalTime::class)
    val state = alternatives.combineStates(coordinates) { alternatives, coordinates ->
        if (alternatives.isEmpty()) return@combineStates null

        val first = alternatives[coordinates.first()].part ?: return@combineStates null
        val last = alternatives[coordinates].part ?: return@combineStates null
        val start = first.departure.atDate(first.date)
        val end = last.arrival.atDate(last.date)

        ConnectionState(
            buses = alternatives,
            length = end - start,
            start = start,
            coordinates = coordinates,
        )
    }

    fun onEvent(e: ConnectionEvent) = when (e) {
        is ConnectionEvent.SelectBus -> {
            val clickCoordinates: Coordinates = coordinates.value.take(e.level + 1)
            val bus = definition.value[clickCoordinates].part
            navigator.navigate(
                Route.Bus(bus.date, bus.busName, bus.start, bus.end)
            )
        }

        is ConnectionEvent.OnSwipe -> {
            val parentCoordinates: Coordinates = coordinates.value.take(e.level)
            val alternatives = alternatives.value.getAlternatives(parentCoordinates)
            if (e.newPage == alternatives.size) launch {
                searchConnection(e.newPage, alternatives, parentCoordinates)
            } else {
                val rest = alternatives[e.newPage].next.getCoordinatesOfFirstConnection()
                coordinates.value = parentCoordinates + e.newPage + rest
            }
            Unit
        }
    }

    private suspend fun searchConnection(
        newPage: Int,
        alternatives: Alternatives,
        parentCoordinates: Coordinates,
    ) {
        val searcher = searcher.await()
        val bus = alternatives.last().part ?: return
        val searched = searcher.search(
            firstOffset = alternatives.size,
            count = 1,
            start = bus.startStop,
            datetime = bus.departure.atDate(bus.date),
        )
        searched.collect {
            if (it != null)
                addConnection(newPage, parentCoordinates, it.toConnectionDefinition())
        }
    }

    private fun ConnectionDefinition.toTreeDefinition(): TreeDefinition =
        TreeDefinition(
            part = first(),
            next = if (size == 1) AlternativesDefinition()
            else AlternativesDefinition(drop(1).toTreeDefinition()),
        )

    private suspend fun ConnectionPartDefinition.toConnectionBus(
        info: Map<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>,
        previousPartEnd: Pair<LocalDateTime, Platform>?,
    ): Pair<Pair<LocalDateTime, Platform>?, ConnectionBus?> {
        val (bus, stops) = info[busName] ?: return (null to null)
        val start = stops[start]
        val end = stops[end]
        val stopNames = stops.map { it.name }
        val middleDestination = repo.middleDestination(busName.line(), stopNames, this.start)
        val transferTime = previousPartEnd?.first?.let { start.departure!!.atDate(date) - it }
        val samePlatforms = previousPartEnd?.second == start.platform
        val thisPartEnd = end.arrival!!.atDate(date) to end.platform

        return thisPartEnd to ConnectionBus(
            transferTime = transferTime,
            transferTight = transferTime != null &&
                    (samePlatforms && transferTime < 1.minutes || !samePlatforms && transferTime < 2.minutes),
            bus = busName,
            line = busName.shortLine(),
            isTrolleybus = repo.lineTraction(busName.line(), bus.vehicleType) == Traction.Trolleybus,
            date = date,
            startStop = start.name,
            departure = start.departure!!,
            startStopPlatform = start.platform,
            endStop = end.name,
            arrival = end.arrival,
            endStopPlatform = end.platform,
            stopCount = this.end - this.start,
            direction = middleDestination ?: stopNames.last(),
            length = end.arrival - start.departure,
        )
    }

    private suspend fun createAlternatives(
        def: AlternativesDefinition,
        info: Map<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>,
        coordinates: Coordinates = emptyList(),
        previousPartEnd: Pair<LocalDateTime, Platform>? = null,
    ): Alternatives = def.mapIndexed { i, def ->
        val (thisPartEnd, part) =
            def.part.toConnectionBus(info, previousPartEnd)

        ConnectionTree(
            part = part,
            next = createAlternatives(
                def = def.next,
                info = info,
                coordinates = coordinates + i,
                previousPartEnd = thisPartEnd,
            ),
            page = i,
            level = coordinates.size,
        )
    }
}

fun Alternatives.getCoordinatesOfFirstConnection(): Coordinates =
    if (isEmpty()) emptyList() else listOf(0) + first().next.getCoordinatesOfFirstConnection()

@JvmName("getCoordinatesOfFirstConnectionDefinition")
fun AlternativesDefinition.getCoordinatesOfFirstConnection(): Coordinates =
    if (isEmpty()) emptyList() else listOf(0) + first().next.getCoordinatesOfFirstConnection()

private fun AlternativesDefinition.addAt(coordinates: Coordinates, tree: TreeDefinition): AlternativesDefinition {
    if (coordinates.isEmpty()) return this + tree
    val first = coordinates.first()
    val rest = coordinates.drop(1)
    val (before, now, after) = divide(first)
    return before + now.copy(next = now.next.addAt(rest, tree)) + after
}