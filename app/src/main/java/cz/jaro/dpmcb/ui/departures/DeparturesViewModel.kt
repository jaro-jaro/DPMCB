package cz.jaro.dpmcb.ui.departures

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.busOnMapByName
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.now
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Duration
import java.time.LocalTime
import kotlin.math.roundToLong
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
        val line: Int?,
        val via: String?,
        val onlyDepartures: Boolean?,
        val simple: Boolean?,
        val getNavDestination: () -> NavDestination?,
    )

    lateinit var scroll: suspend (Int) -> Unit
    lateinit var navigate: NavigateFunction

    private val _info = MutableStateFlow(DeparturesInfo(
        time = params.time,
        lineFilter = params.line,
        stopFilter = params.via,
        justDepartures = params.onlyDepartures ?: false,
        compactMode = params.simple ?: false,
    ))
    val info = _info.asStateFlow()

    init {
        viewModelScope.launch {
            repo.showDeparturesOnly.collect {
                if (params.onlyDepartures != null) _info.update { i ->
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
        ).generateRouteWithArgs(params.getNavDestination() ?: return)
    }

    val hasMapAccess = repo.hasAccessToMap
    val datum = repo.date

    private val list = repo.date
        .combine(onlineRepo.nowRunningBuses()) { datum, onlineConns ->
            repo.departures(datum, params.stop)
                .map {
                    val onlineConn = onlineConns.busOnMapByName(it.busName)
                    it to onlineConn
                }
                .sortedBy { (stop, onlineConn) ->
                    stop.time.plusSeconds(onlineConn?.delayMin?.times(60)?.roundToLong() ?: 0L)
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
                        runsIn = Duration.between(now, stop.time + (onlineConn?.delayMin?.toDouble() ?: 0.0).minutes),
                        nextStop = stop.busStops.map { it.name }.getOrNull(thisStopIndex + 1),
                        stopType = stop.stopType,
                    )
                }
        }
        .flowOn(Dispatchers.IO)

    val state = _info
        .runningFold(null as Pair<DeparturesInfo?, DeparturesInfo>?) { last, new ->
            last?.second to new
        }
        .filterNotNull()
        .combine(list) { (lastState, info), list ->
            val filteredList = list
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
                    if (lastState == null) return@also
                    if (lastState.time == info.time && lastState.justDepartures == info.justDepartures && lastState.stopFilter == info.stopFilter && lastState.lineFilter == info.lineFilter) return@also
                    if (filteredList.isEmpty()) return@also
                    viewModelScope.launch(Dispatchers.Main) {
                        scroll(filteredList.home(info))
                    }
                }

            if (filteredList.isEmpty()) {
                if (info.lineFilter == null && info.stopFilter == null) DeparturesState.NothingRunsAtAll
                else if (info.lineFilter == null) DeparturesState.NothingRunsHere
                else if (info.stopFilter == null) DeparturesState.LineDoesNotRun
                else DeparturesState.LineDoesNotRunHere
            } else DeparturesState.Runs(
                line = filteredList
            )
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeparturesState.Loading)

    fun onEvent(e: DeparturesEvent) = when (e) {
        is DeparturesEvent.GoToBus -> {
            navigate(
                Route.Bus(
                    e.bus.busName
                )
            )
        }

        is DeparturesEvent.GoToTimetable -> {
            e.bus.nextStop?.let {
                navigate(
                    Route.Timetable(
                        lineNumber = e.bus.lineNumber,
                        stop = params.stop,
                        nextStop = e.bus.nextStop,
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
                        scrollIndex = e.i
                    )
                }
            }
            Unit
        }

        is DeparturesEvent.WentBack -> {
            viewModelScope.launch(Dispatchers.Main) {
                _info.update { oldState ->
                    when (e.result.chooserType) {
                        ChooserType.ReturnLine -> oldState.copy(lineFilter = e.result.value.toInt())
                        ChooserType.ReturnStop -> oldState.copy(stopFilter = e.result.value)
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
                ).also {
                    viewModelScope.launch {
                        repo.changeDepartures(it.justDepartures)
                    }
                }
            }
        }

        DeparturesEvent.ScrollToHome -> {
            viewModelScope.launch(Dispatchers.Main) {
                if (state.value !is DeparturesState.Runs) return@launch
                scroll((state.value as DeparturesState.Runs).line.home(_info.value))
            }
            Unit
        }

        DeparturesEvent.NextDay -> {
            repo.changeDate(repo.date.value.plusDays(1))
            _info.update {
                it.copy(
                    time = LocalTime.of(0, 0)
                )
            }
        }

        DeparturesEvent.PreviousDay -> {
            repo.changeDate(repo.date.value.plusDays(-1))
            _info.update {
                it.copy(
                    time = LocalTime.of(23, 59)
                )
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (state.value == DeparturesState.Loading) Unit
            if (state.value !is DeparturesState.Runs) return@launch
            while (!::scroll.isInitialized) Unit
            withContext(Dispatchers.Main) {
                val list = (state.value as DeparturesState.Runs).line
                scroll(
                    list.home(DeparturesInfo(time = params.time))
                )
            }
        }
    }
}
