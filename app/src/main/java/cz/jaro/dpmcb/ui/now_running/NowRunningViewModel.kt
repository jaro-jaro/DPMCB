package cz.jaro.dpmcb.ui.now_running

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.Direction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.asyncMap
import cz.jaro.dpmcb.ui.main.generateRouteWithArgs
import cz.jaro.dpmcb.ui.now_running.NowRunningViewModel.RunningConnPlus.Companion.runningBuses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class NowRunningViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val filters: List<Int>,
        val type: NowRunningType,
        val navigate: NavigateFunction,
        val getNavDestination: () -> NavDestination?,
    )

    private val type = MutableStateFlow(params.type)
    private val filters = MutableStateFlow(params.filters)
    private val loading = MutableStateFlow(true)


    @SuppressLint("RestrictedApi")
    private fun changeCurrentRoute() {
        App.route = Route.NowRunning(
            filters = filters.value,
            type = type.value,
        ).generateRouteWithArgs(params.getNavDestination() ?: return)
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
            params.navigate(Route.Bus(busId = e.busId))
        }

        is NowRunningEvent.NavToSeq -> {
            params.navigate(Route.Sequence(sequence = e.seq))
        }
    }

    private val list = onlineRepo.nowRunningBuses().map { onlineConns ->
        loading.value = true
        onlineConns
            .asyncMap { onlineConn ->
                val (conn, stops) = repo.nowRunningBus(onlineConn.id, LocalDate.now())
                val middleStop = if (conn.line - 325_000 in repo.oneWayLines()) repo.findMiddleStop(stops) else null
                val indexOnLine = stops.indexOfLast { it.time == onlineConn.nextStop }
                RunningConnPlus(
                    busId = conn.id,
                    nextStopName = stops.lastOrNull { it.time == onlineConn.nextStop }?.name ?: return@asyncMap null,
                    nextStopTime = stops.lastOrNull { it.time == onlineConn.nextStop }?.time ?: return@asyncMap null,
                    delay = onlineConn.delayMin ?: return@asyncMap null,
                    indexOnLine = indexOnLine,
                    direction = conn.direction,
                    lineNumber = conn.line - 325_000,
                    destination = if (middleStop != null && indexOnLine < middleStop.index) middleStop.name else stops.last().name,
                    vehicle = onlineConn.vehicle ?: return@asyncMap null,
                    sequence = conn.sequence,
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
                .groupBy { it.lineNumber to it.destination }
                .map { (_, list) ->
                    RunningLineInDirection(
                        lineNumber = list.first().lineNumber,
                        destination = list.first().destination,
                        buses = list.runningBuses(),
                    )
                }
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
    }

    private val lineNumbers = flow {
        emit(repo.lineNumbers(LocalDate.now()))
    }

    private val nowNotRunning = combine(repo.nowRunningOrNot, nowRunning, filters) { nowRunning, result, filters ->
        val reallyRunning = when (result) {
            is NowRunningResults.RegN -> result.list.mapNotNull { it.sequence }
            is NowRunningResults.Lines -> result.list.flatMap { it.buses }.mapNotNull { it.sequence }
            is NowRunningResults.Delay -> result.list.mapNotNull { it.sequence }
        }

        nowRunning.filter { (seq, lines) ->
            seq !in reallyRunning && (lines.any { it in  filters} || filters.isEmpty())
        }.map { it.first to repo.seqName(it.first) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    val state =
        combine(repo.date, lineNumbers, filteredResult, loading, repo.hasAccessToMap, filters, type, nowNotRunning) { date, lineNumbers, result, listIsLoading, isOnline, filters, type, nowNotRunning ->
            if (date != LocalDate.now()) return@combine NowRunningState.IsNotToday

            if (!isOnline) return@combine NowRunningState.Offline

            if (lineNumbers.isEmpty()) return@combine NowRunningState.NoLines

            if (result.list.isNotEmpty()) return@combine NowRunningState.OK(lineNumbers, filters, type, nowNotRunning, result)

            if (listIsLoading) return@combine NowRunningState.Loading(lineNumbers, filters, type)

            return@combine NowRunningState.NothingRunningNow(lineNumbers, filters, type, nowNotRunning)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowRunningState.LoadingLines(params.type))

    data class RunningConnPlus(
        val busId: String,
        val nextStopName: String,
        val nextStopTime: LocalTime,
        val delay: Float,
        val indexOnLine: Int,
        val direction: Direction,
        val lineNumber: Int,
        val destination: String,
        val vehicle: Int,
        val sequence: String?,
    ) {
        fun toRunningDelayedBus() = RunningDelayedBus(
            busId = busId,
            delay = delay,
            lineNumber = lineNumber,
            destination = destination,
            sequence = sequence,
        )

        fun toRunningVehicle() = RunningVehicle(
            busId = busId,
            lineNumber = lineNumber,
            destination = destination,
            vehicle = vehicle,
            sequence = sequence,
        )

        fun toRunningBus() = RunningBus(
            busId = busId,
            nextStopName = nextStopName,
            nextStopTime = nextStopTime,
            delay = delay,
            vehicle = vehicle,
            sequence = sequence,
        )

        companion object {
            fun List<RunningConnPlus>.runningBuses() = map(RunningConnPlus::toRunningBus)
        }
    }
}
