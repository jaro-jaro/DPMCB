package cz.jaro.dpmcb.ui.bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.helperclasses.NavigateWithOptionsFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.filterFixedCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.filterTimeCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.invoke
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.nowFlow
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.validityString
import cz.jaro.dpmcb.ui.common.TimetableEvent
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class BusViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val busName: BusName,
    @InjectedParam private val date: LocalDate,
) : ViewModel() {

    lateinit var navigate: NavigateWithOptionsFunction

    private val info: Flow<BusState> = combine(repo.favourites, repo.hasAccessToMap) { favourites, online ->
        val exists = repo.doesBusExist(busName)
        if (!exists) return@combine BusState.DoesNotExist(busName)
        val runsAt = repo.doesConnRunAt(busName)
        val validity = repo.lineValidity(busName, date)
        if (!runsAt(date)) {
            val (timeCodes, fixedCodes) = repo.codes(busName, date)
            return@combine BusState.DoesNotRun(
                busName = busName,
                date = date,
                runsNextTimeAfterToday = List(365) { SystemClock.todayHere() + it.days }.firstOrNull { runsAt(it) },
                runsNextTimeAfterDate = List(365) { date + it.days }.firstOrNull { runsAt(it) },
                timeCodes = filterTimeCodesAndMakeReadable(timeCodes),
                fixedCodes = filterFixedCodesAndMakeReadable(fixedCodes, timeCodes),
                lineCode = validityString(validity),
                deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$busName",
            )
        }

        val bus = repo.busDetail(busName, date)
        val restriction = repo.hasRestriction(busName, date)
        BusState.OK(
            busName = busName,
            stops = bus.stops,
            lineNumber = bus.info.line,
            lowFloor = bus.info.lowFloor,
            timeCodes = filterTimeCodesAndMakeReadable(bus.timeCodes),
            fixedCodes = filterFixedCodesAndMakeReadable(bus.fixedCodes, bus.timeCodes),
            lineCode = validityString(validity),
            deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$busName",
            restriction = restriction,
            favourite = favourites.find { it.busName == busName },
            lineHeight = 0F,
            traveledSegments = 0,
            error = online && date == SystemClock.todayHere() && bus.stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= bus.stops.last().time,
            sequence = bus.info.sequence,
            sequenceName = with(repo) { bus.info.sequence?.seqName() },
            previousBus = bus.sequence?.let { seq ->
                val i = seq.indexOf(busName)
                seq.getOrNull(i - 1)
                    ?: bus.before?.let {
                        if (it.size != 1) null
                        else repo.lastBusOfSequence(it.single(), date)
                    }
            },
            nextBus = bus.sequence?.let { seq ->
                val i = seq.indexOf(busName)
                seq.getOrNull(i + 1)
                    ?: bus.after?.let {
                        if (it.size != 1) null
                        else repo.firstBusOfSequence(it.single(), date)
                    }
            },
            date = date,
        )
    }

    fun onEvent(e: BusEvent) = when (e) {
        is BusEvent.ChangeDate -> {
            viewModelScope.launch {
                navigate(Route.Bus(e.date, busName))
            }
            Unit
        }

        is BusEvent.ChangeFavourite -> {
            viewModelScope.launch {
                repo.changeFavourite(e.newFavourite)
            }
            Unit
        }

        BusEvent.NextBus -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null && state.nextBus != null) {
                navigate(Route.Bus(state.date, state.nextBus), popUpTo<Route.Bus>())
            }
            Unit
        }

        BusEvent.PreviousBus -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null && state.previousBus != null) {
                navigate(Route.Bus(state.date, state.previousBus), popUpTo<Route.Bus>())
            }
            Unit
        }

        BusEvent.RemoveFavourite -> {
            viewModelScope.launch {
                repo.removeFavourite(busName)
            }
            Unit
        }

        BusEvent.ShowSequence -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null) {
                navigate(Route.Sequence(date = date, state.sequence))
            }
            Unit
        }

        is BusEvent.TimetableClick -> when (e.e) {
            is TimetableEvent.StopClick -> navigate(Route.Departures(date, e.e.stopName, e.e.time.toSimpleTime()))
            is TimetableEvent.TimetableClick -> navigate(Route.Timetable(date, e.e.line, e.e.stop, e.e.nextStop))
        }
    }

    private val onlineState = (if (date == SystemClock.todayHere()) onlineRepo.bus(busName).map { (onlineConn, onlineTimetable, onlineStopIndex) ->
        OnlineBusState(
            delay = onlineConn?.delayMin?.toDouble()?.minutes,
            onlineTimetable = onlineTimetable,
            onlineStopIndex = onlineStopIndex,
            vehicle = onlineConn?.vehicle,
            confirmedLowFloor = onlineConn?.lowFloor,
            nextStopTime = onlineConn?.nextStop
        )
    } else flowOf(OnlineBusState()))
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnlineBusState())

    private val traveledSegments = combine(info, onlineState, nowFlow) { info, state, now ->
        when {
            info !is BusState.OK -> null
            info.stops.isEmpty() -> null
            date > SystemClock.todayHere() -> null
            date < SystemClock.todayHere() -> info.stops.lastIndex
            state.onlineStopIndex?.size == 1 -> state.onlineStopIndex.single().toInt()
            !state.onlineStopIndex.isNullOrEmpty() -> state.onlineStopIndex.minBy { (info.stops[it.toInt()].time - now).absoluteValue }.toInt()
//            state.onlineTimetable?.nextStopIndex != null -> state.onlineTimetable.nextStopIndex - 1
            state.nextStopTime != null -> info.stops.indexOfLast { it.time == state.nextStopTime }.coerceAtLeast(1) - 1
            info.stops.last().time < now -> info.stops.lastIndex
            else -> info.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, onlineState, nowFlow, traveledSegments) { info, state, now, traveledSegments ->

        if (info !is BusState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        if (state.onlineStopIndex?.size == 1) state.onlineStopIndex.single()
        if (!state.onlineStopIndex.isNullOrEmpty()) state.onlineStopIndex.minBy { (info.stops[it.toInt()].time - now).absoluteValue }.toInt()

        val departureFromLastStop = info.stops.getOrElse(traveledSegments) { info.stops.last() }.time.plus(state.delay ?: 0.minutes)

        val arrivalToNextStop = state.onlineTimetable?.stops?.getOrNull(traveledSegments + 1)?.let {
            it.scheduledTime.plus(it.delay.minutes).plus(state.delay ?: 0.seconds)
        } ?: (info.stops.getOrNull(traveledSegments + 1)?.time?.plus(state.delay ?: 0.minutes))

        val length = arrivalToNextStop?.minus(departureFromLastStop) ?: Duration.INFINITE

        val passed = (now - departureFromLastStop).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.inWholeSeconds / length.inWholeSeconds.toFloat()).coerceIn(0F, 1F)
    }

    val state = combine(info, traveledSegments, lineHeight, onlineState) { info, traveledSegments, lineHeight, onlineState ->
        if (info !is BusState.OK) info
        else info.copy(
            lineHeight = lineHeight,
            traveledSegments = traveledSegments ?: 0,
            online = if (onlineState.onlineTimetable != null) BusState.OnlineState(
                onlineConnStops = onlineState.onlineTimetable.stops,
                running = if (onlineState.delay != null || onlineState.nextStopTime != null) BusState.RunningState(
                    delayMin = onlineState.delay?.inWholeSeconds?.div(60F),
                    vehicle = onlineState.vehicle,
                    confirmedLowFloor = onlineState.confirmedLowFloor,
                    nextStopIndex = (traveledSegments ?: 0) + 1
                    //onlineState.onlineTimetable.nextStopIndex ?: state.stops.indexOfFirst { it.time == onlineState.nextStopTime },
                ) else null
            ) else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), BusState.Loading)
}