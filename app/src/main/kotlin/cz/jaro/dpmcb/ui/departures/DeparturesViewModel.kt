package cz.jaro.dpmcb.ui.departures

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import androidx.navigation.navOptions
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.busOnMapByName
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.NavigateWithOptionsFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.minus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.now
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.SimpleTime
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.Route.Favourites.date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.collections.filterNot as remove

@KoinViewModel
class DeparturesViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val stop: String,
        val time: LocalTime,
        val date: LocalDate,
        val line: ShortLine?,
        val via: String?,
        val onlyDepartures: Boolean?,
        val simple: Boolean?,
        val getNavDestination: () -> NavDestination?,
        val scroll: suspend (Int) -> Unit,
        val navigate: NavigateWithOptionsFunction,
    )

    private val info = MutableStateFlow(
        DeparturesInfo(
            time = params.time,
            date = params.date,
            lineFilter = params.line,
            stopFilter = params.via,
            justDepartures = params.onlyDepartures ?: false,
            compactMode = params.simple ?: false,
        )
    )

    init {
        viewModelScope.launch {
            repo.showDeparturesOnly.collect {
                if (params.onlyDepartures == null) info.update { i ->
                    i.copy(
                        justDepartures = it
                    )
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun changeCurrentRoute(info: DeparturesInfo) {
        App.route = Route.Departures(
            stop = params.stop,
            time = info.time.toSimpleTime(),
            line = info.lineFilter,
            via = info.stopFilter,
            onlyDepartures = info.justDepartures,
            simple = info.compactMode,
            date = info.date,
        ).generateRouteWithArgs(params.getNavDestination() ?: return)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val onlineConns = info
        .flatMapConcat { info ->
            if (info.date == SystemClock.todayHere()) onlineRepo.nowRunningBuses() else flowOf(emptyList())
        }

    private val list = onlineConns.combine(info) { onlineConns, info ->
        repo.departures(info.date, params.stop)
            .map {
                val onlineConn = onlineConns.busOnMapByName(it.busName)
                it to onlineConn
            }
            .sortedBy { (stop, onlineConn) ->
                stop.time + (onlineConn?.delayMin?.toDouble() ?: .0).minutes
            }
            .map { (stop, onlineConn) ->
                val thisStopIndex = stop.busStops.indexOfFirst { it.stopIndexOnLine == stop.stopIndexOnLine }
                val middleStop = if (stop.line in repo.oneWayLines()) repo.findMiddleStop(stop.busStops) else null
                val lastStop = stop.busStops.last { it.time != null }
                val currentNextStop = onlineConn?.nextStop?.let { nextStop ->
                    stop.busStops
                        .filter { it.time != null }
                        .findLast { it.time!! == nextStop }
                        ?.let { it.name to it.time!! }
                }
                val lastIndexOfThisStop = stop.busStops.indexOfLast { it.name == stop.name }.let {
                    if (it == thisStopIndex) stop.busStops.lastIndex else it
                }

                DepartureState(
                    destination = if (middleStop != null && (thisStopIndex + 1) < middleStop.index) middleStop.name else lastStop.name,
                    lineNumber = stop.line,
                    time = stop.time,
                    currentNextStop = currentNextStop,
                    busName = stop.busName,
                    lowFloor = stop.lowFloor,
                    confirmedLowFloor = onlineConn?.lowFloor,
                    delay = onlineConn?.delayMin,
                    runsVia = stop.busStops.map { it.name }.filterIndexed { i, _ -> i in (thisStopIndex + 1)..lastIndexOfThisStop },
                    runsIn = stop.time + (onlineConn?.delayMin?.toDouble() ?: .0).minutes - now,
                    nextStop = stop.busStops.map { it.name }.getOrNull(thisStopIndex + 1),
                    stopType = stop.stopType,
                )
            }
    }
        .flowOn(Dispatchers.IO)

    val state = info
        .runningFold(null as Pair<DeparturesInfo?, DeparturesInfo>?) { last, new ->
            last?.second to new
        }
        .filterNotNull()
        .combine(list) { (lastInfo, info), list ->
            list
                .filter {
                    info.lineFilter?.let { filter -> it.lineNumber == filter } ?: true
                }
                .filter {
                    info.stopFilter?.let { filter -> it.runsVia.contains(filter) } ?: true
                }
                .remove {
                    info.justDepartures && (it.nextStop == null || it.stopType == StopType.GetOffOnly)
                }
                .also { filteredList ->
                    if (lastInfo == null) return@also
                    if (lastInfo.time == info.time && lastInfo.justDepartures == info.justDepartures && lastInfo.stopFilter == info.stopFilter && lastInfo.lineFilter == info.lineFilter) return@also
                    if (filteredList.isEmpty()) return@also
                    viewModelScope.launch(Dispatchers.Main) {
                        params.scroll(filteredList.home(info.time))
                    }
                }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        .combine(info, repo.hasAccessToMap) { filteredList, info, isOnline ->
            when {
                filteredList == null -> DeparturesState.Loading(
                    info = info,
                    stop = params.stop,
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
                    stop = params.stop,
                    isOnline = isOnline,
                )
                else -> DeparturesState.Runs(
                    departures = filteredList,
                    info = info,
                    stop = params.stop,
                    isOnline = isOnline,
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeparturesState.Loading(info.value, params.stop, repo.hasAccessToMap.value))

    fun onEvent(e: DeparturesEvent) = when (e) {
        is DeparturesEvent.GoToBus -> {
            params.navigate(
                Route.Bus(
                    e.bus.busName,
                    info.value.date,
                ), null
            )
        }

        is DeparturesEvent.GoToTimetable -> {
            e.bus.nextStop?.let {
                params.navigate(
                    Route.Timetable(
                        lineNumber = e.bus.lineNumber,
                        stop = params.stop,
                        nextStop = e.bus.nextStop,
                        date = info.value.date,
                    ), null
                )
            }
        }

        is DeparturesEvent.ChangeTime -> {
            viewModelScope.launch(Dispatchers.Main) {
                info.update { oldState ->
                    oldState.copy(
                        time = e.time,
                    ).also(::changeCurrentRoute)
                }
            }
            Unit
        }

        is DeparturesEvent.Scroll -> {
            viewModelScope.launch(Dispatchers.Main) {
                info.update {
                    it.copy(
                        scrollIndex = e.i,
                    )
                }
            }
            Unit
        }

        is DeparturesEvent.WentBack -> {
            viewModelScope.launch(Dispatchers.Main) {
                info.update { oldState ->
                    when (e.result.chooserType) {
                        ChooserType.ReturnLine -> oldState.copy(lineFilter = e.result.value.toShortLine())
                        ChooserType.ReturnStop -> oldState.copy(stopFilter = e.result.value)
                        else -> return@launch
                    }.also(::changeCurrentRoute)
                }
            }
            Unit
        }

        is DeparturesEvent.Canceled -> {
            viewModelScope.launch(Dispatchers.IO) {
                info.update { oldState ->
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
            info.update {
                it.copy(
                    compactMode = !it.compactMode
                )
            }
        }

        DeparturesEvent.ChangeJustDepartures -> {
            info.update {
                it.copy(
                    justDepartures = !it.justDepartures
                ).also {
                    viewModelScope.launch {
                        repo.changeDepartures(it.justDepartures)
                    }
                }
            }
        }

        DeparturesEvent.ScrollToHome -> {
            viewModelScope.launch(Dispatchers.Main) {
                val state = state.value
                if (state !is DeparturesState.Runs) return@launch
                params.scroll(state.departures.home(state.info.time))
            }
            Unit
        }

        DeparturesEvent.NextDay -> params.navigate(
            Route.Departures(
                stop = params.stop,
                time = SimpleTime(0, 0),
                line = info.value.lineFilter,
                via = info.value.stopFilter,
                onlyDepartures = info.value.justDepartures,
                simple = info.value.compactMode,
                date = info.value.date + 1.days,
            ), null
        )

        DeparturesEvent.PreviousDay -> params.navigate(
            Route.Departures(
                stop = params.stop,
                time = SimpleTime(23, 59),
                line = info.value.lineFilter,
                via = info.value.stopFilter,
                onlyDepartures = info.value.justDepartures,
                simple = info.value.compactMode,
                date = info.value.date - 1.days,
            ), null
        )

        is DeparturesEvent.ChangeDate -> params.navigate(
            Route.Departures(
                stop = params.stop,
                time = info.value.time.toSimpleTime(),
                line = info.value.lineFilter,
                via = info.value.stopFilter,
                onlyDepartures = info.value.justDepartures,
                simple = info.value.compactMode,
                date = info.value.date,
            ), null
        )

        DeparturesEvent.ChangeLine -> params.navigate(
            Route.Chooser(ChooserType.ReturnLine, date = info.value.date),
            navOptions {
                launchSingleTop = true
            }
        )

        DeparturesEvent.ChangeStop -> params.navigate(Route.Chooser(ChooserType.Stops, date = date), null)
        DeparturesEvent.ChangeVia -> params.navigate(
            Route.Chooser(ChooserType.ReturnStop, date = date),
            navOptions {
                launchSingleTop = true
            },
        )
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (state.value is DeparturesState.Loading) Unit
            if (state.value !is DeparturesState.Runs) return@launch
            withContext(Dispatchers.Main) {
                val list = (state.value as DeparturesState.Runs).departures
                params.scroll(
                    list.home(params.time)
                )
            }
        }
    }
}
