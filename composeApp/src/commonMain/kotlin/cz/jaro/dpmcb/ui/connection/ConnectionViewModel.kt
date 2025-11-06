package cz.jaro.dpmcb.ui.connection

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.ConnectionSearcher
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.helperclasses.async
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.middleDestination
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.mutate
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.data.realtions.connection.ConnectionBusInfo
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
import cz.jaro.dpmcb.ui.connection.ConnectionViewModel.Def.Companion.getAlternativeCursors
import cz.jaro.dpmcb.ui.connection.ConnectionViewModel.Def.Companion.getAtLevel
import cz.jaro.dpmcb.ui.connection.ConnectionViewModel.Def.Companion.getCurrentCursorSection
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class ConnectionViewModel(
    private val repo: SpojeRepository,
    args: Route.Connection,
) : ViewModel() {
    lateinit var navigator: Navigator

    private val definitions: MutableStateFlow<List<Def>> = MutableStateFlow(
        mutableListOf(Def(startLevel = 0, args.def))
    )

    private val cursor: MutableStateFlow<Int> = MutableStateFlow(0)

    private suspend fun addConnection(
        new: Def,
    ) {
        loadInfo(new.buses)
        val all = definitions.value
        val section = all.getCurrentCursorSection(cursor.value, new.startLevel)

        val newPlace = section.last() + 1

        definitions.update {
            it.mutate {
                add(newPlace, new)
            }
        }
        cursor.value = newPlace
    }

    private val info =
        MutableStateFlow(emptyMap<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>())

    private suspend fun loadInfo(
        def: List<ConnectionPartDefinition>,
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

    data class Def(
        val startLevel: Int,
        val buses: ConnectionDefinition,
    ) {
        companion object {
            fun List<Def>.getCurrentCursorSection(
                cursor: Int,
                level: Int,
            ): IntRange {
                if (level == 0) return 0..<size
                val afterCursor = drop(cursor + 1)
                val upToCursor = take(cursor + 1)
                val startOfNext = afterCursor.indexOfFirst { it.startLevel < level }.takeUnless { it == -1 } ?: size
                val startOfThis = upToCursor.indexOfLast { it.startLevel < level }
                return (startOfThis..<startOfNext)
            }

            fun List<Def>.getAlternativeCursors(
                cursor: Int,
                level: Int,
            ) = getCurrentCursorSection(cursor, level).map {
                IndexedValue(it, this[it])
            }.filter {
                it.value.startLevel <= level
            }

            fun Def.getAtLevel(
                level: Int,
            ) = buses[level - startLevel]
        }
    }

    private val currentConnection = combineStates(cursor, definitions) { cursor, definitions ->
        val currentDef = definitions[cursor]
        val currentParents = (0..<currentDef.startLevel).map { parentLevel ->
            val parentCursor = definitions.getCurrentCursorSection(cursor, parentLevel + 1).first()
            definitions[parentCursor].getAtLevel(parentLevel) to parentCursor
        }
        val currentBuses = (currentParents + currentDef.buses.map { it to cursor })

        currentBuses.mapIndexed { level, (bus, cursor) ->
            val alternativeCursors =
                definitions.getAlternativeCursors(cursor, level)
            val beforeCursors = alternativeCursors.takeWhile { it.index < cursor }
            val afterCursors = alternativeCursors.dropWhile { it.index <= cursor }
            val before = beforeCursors.map { IndexedValue(it.index, it.value.getAtLevel(level)) }
            val after = afterCursors.map { IndexedValue(it.index, it.value.getAtLevel(level)) }
            AlternativesDefinition(before, IndexedValue(cursor, bus), after)
        }
    }

    private val buses = currentConnection.combine(info) {
            currentConnection,
            info,
        ->
        currentConnection
            .reversed().windowed(2, partialWindows = true).reversed()
            .mapIndexed { level, parts ->
                val current = parts[0]
                val previous = parts.getOrNull(1)

                val previousPartEnd = previous?.let {
                    it.now.value.busName.let(info::get)?.value?.get(it.now.value.end)?.arrival?.atDate(it.now.value.date)
                }
                Alternatives(
                    level = level,
                    before = current.before.map { createConnectionBus(info, it.value, it.index, previousPartEnd) },
                    now = createConnectionBus(info, current.now.value, current.now.index, previousPartEnd),
                    after = current.after.map { createConnectionBus(info, it.value, it.index, previousPartEnd) },
                )
            }
    }.stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), null)

    private suspend fun createConnectionBus(
        info: Map<BusName, Map.Entry<ConnectionBusInfo, List<StopNameTime>>>,
        part: ConnectionPartDefinition,
        cursor: Int,
        previousPartEnd: LocalDateTime?,
    ): ConnectionBus? {
        val (bus, stops) = info[part.busName] ?: return null
        val start = stops[part.start]
        val end = stops[part.end]
        val stopNames = stops.map { it.name }
        val middleDestination = repo.middleDestination(part.busName.line(), stopNames, part.start)
        return ConnectionBus(
            transferTime = previousPartEnd?.let { start.departure!!.atDate(part.date) - it },
            bus = part.busName,
            line = part.busName.shortLine(),
            isTrolleybus = repo.lineTraction(part.busName.line(), bus.vehicleType) == Traction.Trolleybus,
            date = part.date,
            startStop = start.name,
            departure = start.departure!!,
            startStopPlatform = start.platform,
            endStop = end.name,
            arrival = end.arrival!!,
            endStopPlatform = end.platform,
            stopCount = part.end - part.start,
            direction = middleDestination ?: stopNames.last(),
            length = end.arrival - start.departure,
            cursor = cursor,
        )
    }

    @OptIn(ExperimentalTime::class)
    val state = buses.mapState { alternatives ->
        if (alternatives.isNullOrEmpty()) return@mapState null

        val first = alternatives.firstNotNullOfOrNull { it.now } ?: return@mapState null
        val last = alternatives.reversed().firstNotNullOf { it.now }
        val start = first.departure.atDate(first.date)
        val end = last.arrival.atDate(last.date)

        ConnectionState(
            buses = alternatives,
            length = end - start,
            start = start,
        )
    }

    fun onEvent(e: ConnectionEvent) = when (e) {
        is ConnectionEvent.SelectBus -> {
            val bus = currentConnection.value.find { it.now.value.busName == e.bus }!!.now.value
            navigator.navigate(
                Route.Bus(bus.date, bus.busName, bus.start, bus.end)
            )
        }

        is ConnectionEvent.Swipe -> {
            val buses = buses.value!!
            val alternatives = buses[e.level]
            if (e.page == alternatives.count) launch {
                searchConnection(e.level)
            } else {
                val new = alternatives.all[e.page]
                if (new != null) cursor.value = new.cursor
            }
            Unit
        }
    }

    private suspend fun searchConnection(
        level: Int,
    ) {
        val c = buses.value!!
        val a = c[level]
        if (a.now == null) return
        val searcher = searcher.await()
        val searched = searcher.search(a.count, 1, a.now.startStop, a.now.departure.atDate(a.now.date) + 59.seconds)
        searched.collect {
            if (it != null)
                addConnection(Def(level, it.toConnectionDefinition()))
        }
    }
}