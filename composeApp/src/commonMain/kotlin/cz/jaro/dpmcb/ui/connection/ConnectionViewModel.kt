package cz.jaro.dpmcb.ui.connection

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.CommonConnectionSearcher
import cz.jaro.dpmcb.data.Logger
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.helperclasses.async
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.middleDestination
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.data.realtions.connection.ConnectionBusInfo
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
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
) : ViewModel(), Logger by repo {
    lateinit var navigator: Navigator

    private val definition: MutableStateFlow<DefinitionData> = MutableStateFlow(
        DefinitionData(
            rootAlternatives = AlternativesDefinition(args.def.toTreeDefinition()),
            currentCoordinates = args.def.map { 0 },
        )
    )

    private suspend fun addConnection(
        newPage: Int,
        parentCoordinates: Coordinates,
        def: ConnectionDefinition,
    ) {
        loadInfo(def)
        val newTree = def.toTreeDefinition()
        val rest = newTree.next.getCoordinatesOfFirstConnection()
        definition.update {
            DefinitionData(
                rootAlternatives = it.rootAlternatives.addAt(parentCoordinates, newTree),
                currentCoordinates = parentCoordinates + newPage + rest
            )
        }
    }

    private val info =
        MutableStateFlow(emptyMap<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>())

    private suspend fun loadInfo(
        def: ConnectionDefinition,
    ) {
        def.groupBy { it.date }.forEach { (date, buses) ->
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
        context(repo as Logger) {
            info.map { info ->
                if (info.keys.isEmpty()) return@map null

                val def = args.def
                val first = def.first()
                val last = def.last()
                val (firstBus, firstStops) = info.getValue(first.busName)
                val (lastBus, lastStops) = info.getValue(last.busName)
                val start = firstStops[first.start]
                val end = lastStops[last.end]
                return@map CommonConnectionSearcher(
                    start = start.name,
                    destination = end.name,
                    datetime = start.departure!!.atDate(first.date),
                    repo = repo,
                )
            }.filterNotNull().first()
        }
    }

    private val alternatives = combine(definition, info, ::createDataFromDefinition)
        .stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), ConnectionData(Alternatives(), definition.value.currentCoordinates))

    @OptIn(ExperimentalTime::class)
    val state = alternatives.mapState { (alternatives, coordinates) ->
        if (alternatives.isEmpty()) return@mapState null

        val first = alternatives[coordinates.first()].part ?: return@mapState null
        val last = alternatives[coordinates].part ?: return@mapState null
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
            val (rootAlternatives, currentCoordinates) = definition.value
            val clickCoordinates: Coordinates = currentCoordinates.take(e.level + 1)
            val bus = rootAlternatives[clickCoordinates].part
            navigator.navigate(
                Route.Bus(bus.date, bus.busName, bus.start, bus.end)
            )
        }

        is ConnectionEvent.OnSwipe -> {
            val (rootAlternatives, currentCoordinates) = alternatives.value
            val parentCoordinates: Coordinates = currentCoordinates.take(e.level)
            val alternatives = rootAlternatives.getAlternatives(parentCoordinates)
            if (e.newPage == alternatives.size) launch {
                searchConnection(e.newPage, alternatives, parentCoordinates)
            } else {
                val rest = alternatives[e.newPage].next.getCoordinatesOfFirstConnection()
                definition.update {
                    it.copy(currentCoordinates = parentCoordinates + e.newPage + rest)
                }
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
            startOverride = bus.startStop,
            datetimeOverride = bus.departure.atDate(bus.date),
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
        previousPartEnd: Pair<LocalDateTime, Platform?>?,
    ): Pair<Pair<LocalDateTime, Platform?>?, ConnectionBus?> {
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
            length = end.arrival.atDate(date) - start.departure.atDate(date),
        )
    }

    private suspend fun createDataFromDefinition(
        def: DefinitionData,
        info: Map<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>,
    ) = ConnectionData(
        rootAlternatives = def.rootAlternatives.toAlternatives(info),
        currentCoordinates = def.currentCoordinates,
    )

    private suspend fun AlternativesDefinition.toAlternatives(
        info: Map<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>,
        coordinates: Coordinates = emptyList(),
        previousPartEnd: Pair<LocalDateTime, Platform?>? = null,
    ): Alternatives = mapIndexed { i, def ->
        val (thisPartEnd, part) =
            def.part.toConnectionBus(info, previousPartEnd)

        ConnectionTreeNode(
            part = part,
            next = def.next.toAlternatives(
                info = info,
                coordinates = coordinates + i,
                previousPartEnd = thisPartEnd,
            ),
            page = i,
            level = coordinates.size,
        )
    }
}

/**
 * @return [Coordinates] of the left-most path in this tree
 */
fun Alternatives.getCoordinatesOfFirstConnection(): Coordinates =
    if (isEmpty()) emptyList() else listOf(0) + first().next.getCoordinatesOfFirstConnection()

/**
 * @return [Coordinates] of the left-most path in this tree
 */
@JvmName("getCoordinatesOfFirstConnectionDefinition")
fun AlternativesDefinition.getCoordinatesOfFirstConnection(): Coordinates =
    if (isEmpty()) emptyList() else listOf(0) + first().next.getCoordinatesOfFirstConnection()

/**
 * Adds a child to the node at the [coordinates]
 * @return The modified tree
 */
private fun AlternativesDefinition.addAt(coordinates: Coordinates, tree: TreeDefinition): AlternativesDefinition {
    if (coordinates.isEmpty()) return this + tree
    val first = coordinates.first()
    val rest = coordinates.drop(1)
    val (before, now, after) = divide(first)
    return before + now.copy(next = now.next.addAt(rest, tree)) + after
}