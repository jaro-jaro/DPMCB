package cz.jaro.dpmcb.ui.now_running

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.groupByPair
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.asyncMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalTime
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class NowRunningViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val filters: List<ShortLine>,
        val type: NowRunningType,
        val navigate: NavigateFunction,
        val getNavDestination: () -> NavDestination?,
    )

    private val type = MutableStateFlow(params.type)
    private val filters = MutableStateFlow(params.filters)
    private val loading = MutableStateFlow(true)


    private fun changeCurrentRoute() {
        try {
            App.route = Route.NowRunning(
                filters = filters.value,
                type = type.value,
            ).generateRouteWithArgs(params.getNavDestination() ?: return)
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
            type.value = e.typ
            changeCurrentRoute()
        }

        is NowRunningEvent.NavToBus -> {
            params.navigate(Route.Bus(busName = e.busName))
        }

        is NowRunningEvent.NavToSeq -> {
            params.navigate(Route.Sequence(sequence = e.seq))
        }
    }

    private val list = onlineRepo.nowRunningBuses().map { onlineConns ->
        loading.value = true
        onlineConns
            .asyncMap { onlineConn ->
                val bus = repo.nowRunningBus(onlineConn.name, SystemClock.todayHere()) ?: return@asyncMap null
                val middleStop = if (bus.lineNumber in repo.oneWayLines()) repo.findMiddleStop(bus.stops) else null
                val indexOnLine = bus.stops.indexOfLast { it.time == onlineConn.nextStop }
                RunningConnPlus(
                    busName = bus.busName,
                    nextStopName = bus.stops.lastOrNull { it.time == onlineConn.nextStop }?.name ?: return@asyncMap null,
                    nextStopTime = bus.stops.lastOrNull { it.time == onlineConn.nextStop }?.time ?: return@asyncMap null,
                    delay = onlineConn.delayMin ?: return@asyncMap null,
                    indexOnLine = indexOnLine,
                    direction = bus.direction,
                    lineNumber = bus.lineNumber,
                    destination = if (middleStop != null && indexOnLine < middleStop.index) middleStop.name else bus.stops.last().name,
                    vehicle = onlineConn.vehicle ?: return@asyncMap null,
                    sequence = bus.sequence,
                )
            }
            .filterNotNull()
            .also { loading.value = false }
    }

    private val nowRunning = list.combine(type) { it, type ->
        when (type) {
            NowRunningType.RegN -> it
                .sortedWith(
                    compareBy { it.vehicle }
                )
                .map(RunningConnPlus::toRunningVehicle)
                .toResult()

            NowRunningType.Line -> it
                .sortedWith(
                    compareBy<RunningConnPlus> { it.lineNumber }
                        .thenBy { it.direction }
                        .thenBy { it.destination }
                        .thenBy { it.indexOnLine }
                        .thenByDescending { it.nextStopTime }
                )
                .groupByPair({ it.lineNumber to it.destination }, RunningConnPlus::toRunningBus)
                .map(::RunningLineInDirection)
                .toResult()

            NowRunningType.Delay -> it
                .sortedWith(
                    compareByDescending<RunningConnPlus> { it.delay }
                        .thenBy { it.lineNumber }
                        .thenBy { it.direction }
                        .thenBy { it.destination }
                        .thenBy { it.indexOnLine }
                        .thenByDescending { it.nextStopTime }
                )
                .map(RunningConnPlus::toRunningDelayedBus)
                .toResult()
        }
    }

    private val filteredResult = nowRunning.combine(filters) { result, filters ->
        when (result) {
            is NowRunningResults.RegN -> result.copy(list = result.list.filter { filters.isEmpty() || it.lineNumber in filters })
            is NowRunningResults.Lines -> result.copy(list = result.list.filter { filters.isEmpty() || it.lineNumber in filters })
            is NowRunningResults.Delay -> result.copy(list = result.list.filter { filters.isEmpty() || it.lineNumber in filters })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), null)

    private val lineNumbers = repo::lineNumbersToday.asFlow()

    private val nowNotRunningBuses = combine(repo.nowRunningOrNot, nowRunning, filters) { nowRunning, result, filters ->
        val reallyRunning = when (result) {
            is NowRunningResults.RegN -> result.list.mapNotNull { it.sequence }
            is NowRunningResults.Lines -> result.list.flatMap { it.buses }.mapNotNull { it.sequence }
            is NowRunningResults.Delay -> result.list.mapNotNull { it.sequence }
        }

        nowRunning.filter { (seq, bus) ->
            seq !in reallyRunning && (bus.shortLine() in filters || filters.isEmpty())
        }.map { it.second }
    }

    private val notRunningList = nowNotRunningBuses.map { buses ->
        buses
            .asyncMap { busName ->
                val bus = repo.nowRunningBus(busName, SystemClock.todayHere()) ?: return@asyncMap null
                val (indexOnLine, nextStop) = bus.stops.withIndex().first { it.value.time > SystemClock.timeHere() }
                val middleStop = if (bus.lineNumber in repo.oneWayLines()) repo.findMiddleStop(bus.stops) else null
                RunningConnPlus(
                    busName = bus.busName,
                    nextStopName = nextStop.name,
                    nextStopTime = nextStop.time,
                    delay = -1F,
                    indexOnLine = indexOnLine,
                    direction = bus.direction,
                    lineNumber = bus.lineNumber,
                    destination = if (middleStop != null && indexOnLine < middleStop.index) middleStop.name else bus.stops.last().name,
                    vehicle = RegistrationNumber(-1),
                    sequence = bus.sequence,
                )
            }
            .filterNotNull()
    }

    private val nowNotRunning = notRunningList.combine(type) { it, type ->
        when (type) {
            NowRunningType.RegN -> it
                .sortedWith(
                    compareBy<RunningConnPlus> { it.lineNumber }
                        .thenBy { it.direction }
                        .thenBy { it.destination }
                        .thenBy { it.indexOnLine }
                        .thenByDescending { it.nextStopTime }
                )
                .map(RunningConnPlus::toRunningVehicle)
                .toResult()

            NowRunningType.Line -> it
                .sortedWith(
                    compareBy<RunningConnPlus> { it.lineNumber }
                        .thenBy { it.direction }
                        .thenBy { it.destination }
                        .thenBy { it.indexOnLine }
                        .thenByDescending { it.nextStopTime }
                )
                .groupByPair({ it.lineNumber to it.destination }, RunningConnPlus::toRunningBus)
                .map(::RunningLineInDirection)
                .toResult()

            NowRunningType.Delay -> it
                .sortedWith(
                    compareBy<RunningConnPlus> { it.lineNumber }
                        .thenBy { it.direction }
                        .thenBy { it.destination }
                        .thenBy { it.indexOnLine }
                        .thenByDescending { it.nextStopTime }
                )
                .map(RunningConnPlus::toRunningDelayedBus)
                .toResult()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), null)

    val state =
        combine(
            repo.date, lineNumbers, filteredResult, loading,
            repo.hasAccessToMap, filters, type, nowNotRunning
        ) {
                date, lineNumbers, result, listIsLoading,
                isOnline, filters, type, nowNotRunning,
            ->
            when {
                date != SystemClock.todayHere() -> NowRunningState.IsNotToday
                !isOnline -> NowRunningState.Offline
                lineNumbers.isEmpty() -> NowRunningState.NoLines
                result == null/* || listIsLoading*/ -> NowRunningState.Loading(lineNumbers, filters, type)
                nowNotRunning == null -> NowRunningState.OK(lineNumbers, filters, type, NowRunningResults.Lines(emptyList()), result)
                result.list.isNotEmpty() -> NowRunningState.OK(lineNumbers, filters, type, nowNotRunning, result)
                nowNotRunning.list.isEmpty() -> NowRunningState.NothingRunsToday(lineNumbers, filters, type)
                else -> NowRunningState.NothingRunningNow(lineNumbers, filters, type, nowNotRunning)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowRunningState.LoadingLines(params.type))

    private fun RunningLineInDirection(it: Triple<ShortLine, String, List<RunningBus>>) = RunningLineInDirection(
        lineNumber = it.first,
        destination = it.second,
        buses = it.third,
    )

    data class RunningConnPlus(
        val busName: BusName,
        val nextStopName: String,
        val nextStopTime: LocalTime,
        val delay: Float,
        val indexOnLine: Int,
        val direction: Direction,
        val lineNumber: ShortLine,
        val destination: String,
        val vehicle: RegistrationNumber,
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
}