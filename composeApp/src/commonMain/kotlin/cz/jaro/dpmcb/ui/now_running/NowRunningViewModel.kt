package cz.jaro.dpmcb.ui.now_running

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.OnlineModeManager
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.groupByPair
import cz.jaro.dpmcb.data.helperclasses.middleDestination
import cz.jaro.dpmcb.data.helperclasses.stateIn
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.jikord.OnlineConn
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NowRunningViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    onlineModeManager: OnlineModeManager,
    params: Parameters,
) : ViewModel() {

    data class Parameters(
        val filters: List<ShortLine>,
        val type: NowRunningType,
    )

    lateinit var navigator: Navigator

    private val type = MutableStateFlow(params.type)
    private val filters = MutableStateFlow(params.filters)

    private val allowedType = type.combine(onlineModeManager.hasAccessToMap) { type, isOnline ->
        if (type in NowRunningType.entries(isOnline)) type else NowRunningType.Line
    }

    private fun changeCurrentRoute() {
        try {
            AppState.route = Route.NowRunning(
                filters = filters.value,
                type = type.value,
            ).generateRouteWithArgs(navigator.getNavDestination() ?: return)
        } catch (_: IllegalStateException) {
            return
        }
    }

    fun onEvent(e: NowRunningEvent) = when (e) {
        is NowRunningEvent.ChangeFilter -> {
            if (e.lineNumber in filters.value) filters.value -= (e.lineNumber) else filters.value += (e.lineNumber)
            changeCurrentRoute()
        }

        is NowRunningEvent.ChangeType -> {
            type.value = e.type
            changeCurrentRoute()
        }

        is NowRunningEvent.NavToBus -> {
            navigator.navigate(Route.Bus(date = SystemClock.todayHere(), busName = e.busName))
        }

        is NowRunningEvent.NavToSeq -> {
            navigator.navigate(Route.Sequence(date = SystemClock.todayHere(), sequence = e.seq))
        }
    }

    private val onlineList = onlineRepo.nowRunningBuses().combine(repo.vehicleNumbersOnSequences) { onlineConns, vehicles ->
        val buses = repo.nowRunningBuses(onlineConns.map(OnlineConn::name), SystemClock.todayHere())
        onlineConns
            .mapNotNull { onlineConn ->
                val bus = buses[onlineConn.name] ?: return@mapNotNull null
                val indexOnLine = bus.stops.indexOfLast { it.time == onlineConn.nextStop }
                val vehicle = vehicles[SystemClock.todayHere()]?.get(bus.sequence)
                RunningConnPlus(
                    busName = bus.busName,
                    nextStopName = bus.stops.lastOrNull { it.time == onlineConn.nextStop }?.name ?: return@mapNotNull null,
                    nextStopTime = bus.stops.lastOrNull { it.time == onlineConn.nextStop }?.time ?: return@mapNotNull null,
                    delay = onlineConn.delayMin?.toDouble()?.minutes,
                    indexOnLine = indexOnLine,
                    direction = bus.direction,
                    lineNumber = bus.lineNumber.toShortLine(),
                    destination = repo.middleDestination(bus.lineNumber, bus.stops.map { it.name }, indexOnLine) ?: bus.stops.last().name,
                    vehicle = vehicle ?: onlineConn.vehicle ?: return@mapNotNull null,
                    sequence = bus.sequence,
                )
            }
    }

    private val filteredOnlineList = onlineList.combine(filters) { list, filters ->
        list.filter {
            filters.isEmpty() || it.lineNumber in filters
        }
    }.stateIn(SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), null)

    private val offlineList = repo.nowRunningOrNot.combine(repo.vehicleNumbersOnSequences) { busNames, vehicles ->
        repo.nowRunningBuses(busNames, SystemClock.todayHere()).values
            .map { bus ->
                val (indexOnLine, nextStop) = bus.stops.withIndex().find { SystemClock.timeHere() < it.value.time } ?: bus.stops.withIndex().last()
                val vehicle = vehicles[SystemClock.todayHere()]?.get(bus.sequence)
                RunningConnPlus(
                    busName = bus.busName,
                    nextStopName = nextStop.name,
                    nextStopTime = nextStop.time,
                    delay = (-1).minutes,
                    indexOnLine = indexOnLine,
                    direction = bus.direction,
                    lineNumber = bus.lineNumber.toShortLine(),
                    destination = repo.middleDestination(bus.lineNumber, bus.stops.map { it.name }, indexOnLine) ?: bus.stops.last().name,
                    vehicle = vehicle,
                    sequence = bus.sequence,
                )
            }
    }

    private val filteredOfflineList = offlineList.combine(filters) { list, filters ->
        list.filter {
            filters.isEmpty() || it.lineNumber in filters
        }
    }.stateIn(SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), null)

    private val result = combine(filteredOfflineList, filteredOnlineList, allowedType) { offline, online, type ->
        if (offline == null && online == null) null
        else when (type) {
            NowRunningType.RegN -> NowRunningResults.RegN(resultListRegN(online.orEmpty()), offline = resultListRegN(offline.orEmpty()))
            NowRunningType.Line -> NowRunningResults.Lines(resultListLine(online.orEmpty()), offline = resultListLine(offline.orEmpty()))
            NowRunningType.Delay -> NowRunningResults.Delay(resultListDelay(online.orEmpty()), offline = resultListDelay(offline.orEmpty()))
        }
    }

    private val lineNumbers = repo::lineNumbersToday.asFlow()

    val state = combine(
        lineNumbers, filters, allowedType, result, onlineModeManager.hasAccessToMap,
    ) { lineNumbers, filters, type, result, isOnline ->
        when {
            lineNumbers.isEmpty() -> NowRunningState.NoLines
            result == null -> NowRunningState.Loading(lineNumbers, filters, type)
            else -> NowRunningState.OK(lineNumbers, filters, type, result, isOnline)
        }
    }.stateIn(SharingStarted.WhileSubscribed(5_000), NowRunningState.LoadingLines(params.type))
}

private fun resultListDelay(list: List<RunningConnPlus>) = list
    .sortedWith(
        compareByDescending<RunningConnPlus> { it.delay }
            .thenBy { it.lineNumber }
            .thenBy { it.direction }
            .thenBy { it.destination }
            .thenBy { it.indexOnLine }
            .thenByDescending { it.nextStopTime }
    )
    .map(RunningConnPlus::toRunningDelayedBus)

private fun resultListLine(list: List<RunningConnPlus>) = list
    .sortedWith(
        compareBy<RunningConnPlus> { it.lineNumber }
            .thenBy { it.direction }
            .thenBy { it.destination }
            .thenBy { it.indexOnLine }
            .thenByDescending { it.nextStopTime }
    )
    .groupByPair({ it.lineNumber to it.destination }, RunningConnPlus::toRunningBus)
    .map(::RunningLineInDirection)

private fun resultListRegN(list: List<RunningConnPlus>) = list
    .sortedWith(
        compareBy { it.vehicle }
    )
    .map(RunningConnPlus::toRunningVehicle)

private fun RunningLineInDirection(it: Triple<ShortLine, String, List<RunningBus>>) = RunningLineInDirection(
    lineNumber = it.first,
    destination = it.second,
    buses = it.third,
)

data class RunningConnPlus(
    val busName: BusName,
    val nextStopName: String,
    val nextStopTime: LocalTime,
    val delay: Duration?,
    val indexOnLine: Int,
    val direction: Direction,
    val lineNumber: ShortLine,
    val destination: String,
    val vehicle: RegistrationNumber?,
    val sequence: SequenceCode?,
) {
    fun toRunningDelayedBus() = RunningDelayedBus(
        busName = busName,
        delay = delay,
        lineNumber = lineNumber,
        destination = destination,
        sequence = sequence,
    )

    fun toRunningVehicle() = RunningVehicle(
        busName = busName,
        lineNumber = lineNumber,
        destination = destination,
        vehicle = vehicle,
        sequence = sequence,
    )

    fun toRunningBus() = RunningBus(
        busName = busName,
        nextStopName = nextStopName,
        nextStopTime = nextStopTime,
        delay = delay,
        vehicle = vehicle,
        sequence = sequence,
    )
}