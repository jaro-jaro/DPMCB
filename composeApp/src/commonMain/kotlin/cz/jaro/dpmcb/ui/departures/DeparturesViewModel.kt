package cz.jaro.dpmcb.ui.departures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.OnlineModeManager
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.combine
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.compare
import cz.jaro.dpmcb.data.helperclasses.exactTime
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.middleDestination
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.stateIn
import cz.jaro.dpmcb.data.helperclasses.timeFlow
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.data.onlineBus
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.tuples.Quadruple
import cz.jaro.dpmcb.data.vehicleTraction
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.SimpleTime
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.common.orInvalid
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.collections.filterNot as remove

@OptIn(ExperimentalTime::class)
class DeparturesViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    onlineModeManager: OnlineModeManager,
    private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val stop: String,
        val time: LocalTime?,
        val date: LocalDate,
        val line: ShortLine?,
        val via: String?,
        val onlyDepartures: Boolean?,
        val simple: Boolean?,
    )

    private lateinit var scroll: suspend (Int, animate: Boolean) -> Unit
    lateinit var navigator: Navigator

    private val _info = MutableStateFlow(
        DeparturesInfo(
            stop = params.stop,
            time = params.time,
            date = params.date,
            lineFilter = params.line,
            stopFilter = params.via,
            justDepartures = params.onlyDepartures != false,
            compactMode = params.simple ?: (params.time == null),
        )
    )
    val info = _info.asStateFlow()

    private fun changeCurrentRoute(info: DeparturesInfo) {
        AppState.route = Route.Departures(
            stop = info.stop,
            time = info.time?.toSimpleTime().orInvalid(),
            line = info.lineFilter,
            via = info.stopFilter,
            onlyDepartures = info.justDepartures,
            simple = info.compactMode,
            date = info.date,
        ).generateRouteWithArgs(navigator.getNavDestination() ?: return)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val onlineConns = info
        .flatMapConcat { info ->
            if (info.date == SystemClock.todayHere()) onlineRepo.nowRunningBuses() else flowOf(emptyList())
        }

    private val date = info.map { it.date }.distinctUntilChanged()

    private val departures = date.combine(info) { date, info ->
        repo.departures(date, info.stop)
    }.distinctUntilChanged()

    private val departuresWithOnline =
        departures.combine(onlineConns, date) { departures, onlineConns, date ->
            departures
                .map {
                    it to if (date == SystemClock.todayHere()) onlineConns.onlineBus(it.busName) else null
                }
        }.distinctUntilChanged()

    private val list = combine(departuresWithOnline, date, repo.vehicleNumbersOnSequences) { departures, date, vehicles ->
        departures
            .map { (stop, onlineConn) ->
                val busStops = stop.busStops
                val stopNames = busStops.map { it.name }

                val thisStopIndex = busStops.indexOfFirst { it.stopIndexOnLine == stop.stopIndexOnLine }
                val currentNextStop = onlineConn?.nextStop?.let { nextStop ->
                    busStops
                        .findLast { it.time == nextStop }
                        ?.let { it.name to it.time }
                } ?: busStops
                    .takeIf { date == SystemClock.todayHere() }
                    ?.find { SystemClock.timeHere() < it.time }
                    ?.takeIf { it.time > busStops.first().time }
                    ?.let { it.name to it.time }
                val lastIndexOfThisStop = stopNames.lastIndexOf(stop.name).let {
                    if (it == thisStopIndex) busStops.lastIndex else it
                }

                val destination = repo.middleDestination(stop.line, stopNames, thisStopIndex)
                val lineTraction = repo.lineTraction(stop.line, stop.vehicleType)
                val registrationNumber = vehicles[date]?.get(stop.sequence)
                DepartureState(
                    destination = destination ?: stopNames.last(),
                    lineNumber = stop.line.toShortLine(),
                    time = stop.time,
                    currentNextStop = currentNextStop,
                    busName = stop.busName,
                    lineTraction = lineTraction,
                    vehicleTraction = registrationNumber?.let { repo.vehicleTraction(it) ?: lineTraction },
                    delay = (onlineConn?.delayMin?.toDouble() ?: .0).minutes,
                    runsVia = stopNames.slice((thisStopIndex + 1)..lastIndexOfThisStop),
                    directionIfNotLast = if (destination != null) Direction.NEGATIVE
                    else stop.direction.takeUnless { thisStopIndex == busStops.lastIndex },
                    stopType = stop.stopType,
                )
            }
    }
        .flowOn(Dispatchers.IO)

    private val finalList = combine(list, info, timeFlow) { list, info, now ->
        list
            .sortedBy {
                if (it.delay != null && (it.time + it.delay) > now) it.time + it.delay else it.time
            }
            .filter {
                info.lineFilter?.let { filter -> it.lineNumber == filter } != false
            }
            .filter {
                info.stopFilter?.let { filter -> it.runsVia.contains(filter) } != false
            }
            .remove {
                info.justDepartures && (it.directionIfNotLast == null || it.stopType == StopType.GetOffOnly)
            }
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .stateIn(SharingStarted.WhileSubscribed(5_000), null)

    init {
        combine(info, finalList, timeFlow) { info, list, now ->
            Quadruple(info, list, list?.indexOfNext(info.time ?: now, now), list?.map { it.busName }?.toSet())
        }.compare(Quadruple(null, null, null, null)) {
                (lastInfo, lastList, lastScrollIndex, lastBuses),
                (info, list, scrollIndex, buses),
            ->
            if (lastInfo == null || lastList.isNullOrEmpty() || list.isNullOrEmpty() || scrollIndex == null) return@compare
            if (
                lastInfo.stop == info.stop &&
                lastBuses == buses &&
                (info.time != null && lastInfo.time == info.time || info.time == null && scrollIndex == lastScrollIndex)
            ) return@compare
            withContext(Dispatchers.Main) {
                scroll(scrollIndex, true)
            }
        }
            .flowOn(Dispatchers.IO)
            .launch()
    }

    val state = finalList
        .combineStates(info, onlineModeManager.hasAccessToMap) { filteredList, info, isOnline ->
            when {
                filteredList == null -> DeparturesState.Loading(
                    info = info,
                    isOnline = isOnline,
                )

                filteredList.isEmpty() -> DeparturesState.NothingRuns(
                    reason = when {
                        info.lineFilter == null && info.stopFilter == null -> DeparturesState.NothingRunsReason.NothingRunsAtAll
                        info.lineFilter == null -> DeparturesState.NothingRunsReason.NothingRunsHere
                        info.stopFilter == null -> DeparturesState.NothingRunsReason.LineDoesNotRun
                        else -> DeparturesState.NothingRunsReason.LineDoesNotRunHere
                    },
                    info = info,
                    isOnline = isOnline,
                )

                else -> DeparturesState.Runs(
                    departures = filteredList,
                    info = info,
                    isOnline = isOnline,
                )
            }
        }

    fun onEvent(e: DeparturesEvent) = when (e) {
        is DeparturesEvent.GoToBus -> {
            navigator.navigate(
                Route.Bus(
                    info.value.date,
                    e.bus.busName,
                )
            )
        }

        is DeparturesEvent.GoToTimetable -> {
            e.bus.directionIfNotLast?.let {
                navigator.navigate(
                    Route.Timetable(
                        lineNumber = e.bus.lineNumber,
                        stop = info.value.stop,
                        direction = e.bus.directionIfNotLast,
                        date = info.value.date,
                    )
                )
            }
        }

        is DeparturesEvent.ChangeTime -> {
            viewModelScope.launch(Dispatchers.Main) {
                _info.update { oldState ->
                    oldState.copy(
                        time = e.time,
                    ).also(::changeCurrentRoute)
                }
            }
            Unit
        }

        is DeparturesEvent.Scroll -> {
            viewModelScope.launch(Dispatchers.Main) {
                _info.update {
                    it.copy(
                        scrollIndex = e.i,
                    )
                }
            }
            Unit
        }

        is DeparturesEvent.WentBack -> {
            viewModelScope.launch(Dispatchers.Main) {
                _info.update { oldState ->
                    when (e.result.chooserType) {
                        ChooserType.ReturnLine -> oldState.copy(lineFilter = e.result.value.toShortLine())
                        ChooserType.ReturnStop -> oldState.copy(stopFilter = e.result.value)
                        ChooserType.ReturnStopVia -> oldState.copy(stop = e.result.value)
                        else -> return@launch
                    }.also(::changeCurrentRoute)
                }
            }
            Unit
        }

        is DeparturesEvent.Canceled -> {
            viewModelScope.launch(Dispatchers.IO) {
                _info.update { oldState ->
                    when (e.chooserType) {
                        ChooserType.ReturnLine -> oldState.copy(lineFilter = null)
                        ChooserType.ReturnStop -> oldState.copy(stopFilter = null)
                        else -> return@launch
                    }
                }
            }
            Unit
        }

        DeparturesEvent.ChangeCompactMode -> {
            _info.update {
                it.copy(
                    compactMode = !it.compactMode
                )
            }
        }

        DeparturesEvent.ChangeJustDepartures -> {
            _info.update {
                it.copy(
                    justDepartures = !it.justDepartures
                )
            }
        }

        DeparturesEvent.ScrollToHome -> {
            viewModelScope.launch(Dispatchers.Main) {
                val state = state.value
                if (state !is DeparturesState.Runs) return@launch
                scroll(state.departures.indexOfNext(state.info.time ?: exactTime, exactTime), true)
            }
            Unit
        }

        DeparturesEvent.NextDay -> navigator.navigate(
            Route.Departures(
                stop = info.value.stop,
                time = SimpleTime(0, 0),
                line = info.value.lineFilter,
                via = info.value.stopFilter,
                onlyDepartures = info.value.justDepartures,
                simple = info.value.compactMode,
                date = info.value.date + 1.days,
            )
        )

        DeparturesEvent.PreviousDay -> navigator.navigate(
            Route.Departures(
                stop = info.value.stop,
                time = SimpleTime(23, 59),
                line = info.value.lineFilter,
                via = info.value.stopFilter,
                onlyDepartures = info.value.justDepartures,
                simple = info.value.compactMode,
                date = info.value.date - 1.days,
            )
        )

        is DeparturesEvent.ChangeDate -> navigator.navigate(
            Route.Departures(
                stop = info.value.stop,
                time = info.value.time?.toSimpleTime().orInvalid(),
                line = info.value.lineFilter,
                via = info.value.stopFilter,
                onlyDepartures = info.value.justDepartures,
                simple = info.value.compactMode,
                date = e.date,
            )
        )

        DeparturesEvent.ChangeLine -> navigator.navigate(Route.Chooser(info.value.date, ChooserType.ReturnLine))
        DeparturesEvent.ChangeStop -> navigator.navigate(Route.Chooser(info.value.date, ChooserType.ReturnStop))
        DeparturesEvent.ChangeVia -> navigator.navigate(Route.Chooser(info.value.date, ChooserType.ReturnStopVia))
    }

    fun setScroll(scroll: suspend (Int, animate: Boolean) -> Unit) {
        this.scroll = scroll
        state.takeWhile {
            when (state.value) {
                is DeparturesState.Loading -> true
                is DeparturesState.NothingRuns -> false
                is DeparturesState.Runs -> withContext(Dispatchers.Main) {
                    val list = (state.value as DeparturesState.Runs).departures
                    scroll(list.indexOfNext(params.time ?: exactTime, exactTime), false)

                    false
                }
            }
        }.flowOn(Dispatchers.IO).launch()
    }
}
