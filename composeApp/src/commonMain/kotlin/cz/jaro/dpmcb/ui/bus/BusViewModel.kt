package cz.jaro.dpmcb.ui.bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.AppState.APP_URL
import cz.jaro.dpmcb.data.OnlineModeManager
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.filterFixedCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.filterTimeCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import cz.jaro.dpmcb.data.helperclasses.timeFlow
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.validityString
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.data.seqName
import cz.jaro.dpmcb.data.vehicleName
import cz.jaro.dpmcb.data.vehicleTraction
import cz.jaro.dpmcb.ui.common.TimetableEvent
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.localDateTypePair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.toDateTimePeriod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class BusViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    onlineModeManager: OnlineModeManager,
    args: Route.Bus,
) : ViewModel() {
    private val date = args.date
    private val busName = args.busName
    private val part = args.part

    lateinit var navigator: Navigator

    private val info: Flow<BusState> = combine(onlineModeManager.hasAccessToMap, repo.vehicleNumbersOnSequences) { online, vehicles ->
        val exists = repo.doesBusExist(busName)
        if (!exists) return@combine BusState.DoesNotExist(busName)
        val runsAt = repo.doesConnRunAt(busName)
        val validity = repo.lineValidity(busName, date)
        val serializedDate = localDateTypePair.second.serializeAsValue(date)
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
                deeplink = "${APP_URL}bus/$serializedDate/$busName",
                deeplink2 = "${APP_URL}bus/T/$busName",
            )
        }

        val bus = repo.busDetail(busName, date)
        val restriction = repo.hasRestriction(busName, date)
        val seq = bus.info.sequence
        val vehicleNumber = vehicles[date]?.get(seq)
        val lineTraction = repo.lineTraction(bus.info.line, bus.info.vehicleType)
        BusState.OK(
            busName = busName,
            part = part,
            stops = bus.stops,
            lineNumber = bus.info.line.toShortLine(),
            lowFloor = bus.info.lowFloor,
            lineTraction = lineTraction,
            timeCodes = filterTimeCodesAndMakeReadable(bus.timeCodes),
            fixedCodes = filterFixedCodesAndMakeReadable(bus.fixedCodes, bus.timeCodes),
            lineCode = validityString(validity),
            deeplink = "${APP_URL}bus/$serializedDate/$busName",
            deeplink2 = "${APP_URL}bus/T/$busName",
            restriction = restriction,
            lineHeight = 0F,
            traveledSegments = 0,
            shouldBeOnline = online && date == SystemClock.todayHere() && bus.stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= bus.stops.last().time,
            sequence = seq,
            sequenceName = with(repo) { seq?.seqName() },
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
            direction = bus.info.direction,
            isOneWay = repo.isOneWay(bus.info.line),
            vehicleNumber = vehicleNumber,
            vehicleName = vehicleNumber?.let(repo::vehicleName),
            vehicleTraction = vehicleNumber?.let { repo.vehicleTraction(it) ?: lineTraction },
        )
    }

    fun onEvent(e: BusEvent) = when (e) {
        is BusEvent.ChangeDate -> {
            viewModelScope.launch {
                navigator.navigate(Route.Bus(e.date, busName))
            }
            Unit
        }

        BusEvent.NextBus -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null && state.nextBus != null) {
                navigator.navigate(Route.Bus(state.date, state.nextBus), true)
            }
            Unit
        }

        BusEvent.PreviousBus -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null && state.previousBus != null) {
                navigator.navigate(Route.Bus(state.date, state.previousBus), true)
            }
            Unit
        }

        BusEvent.ShowSequence -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null) {
                navigator.navigate(Route.Sequence(date = date, state.sequence))
            }
            Unit
        }

        is BusEvent.TimetableClick -> {
            when (e.e) {
                is TimetableEvent.StopClick -> navigator.navigate(Route.Departures(date, e.e.stopName, e.e.time.toSimpleTime()))
                is TimetableEvent.TimetableClick -> navigator.navigate(Route.Timetable(date, e.e.line, e.e.stop, e.e.direction))
            }
        }
    }

    private val onlineState = (if (date == SystemClock.todayHere()) onlineRepo.bus(busName).map { (onlineConn, onlineTimetable) ->
        OnlineBusState(
            delay = onlineConn?.delayMin?.toDouble()?.minutes,
            onlineTimetable = onlineTimetable,
            confirmedLowFloor = onlineConn?.lowFloor,
            nextStopTime = onlineConn?.nextStop
        )
    } else flowOf(OnlineBusState()))
        .flowOn(Dispatchers.IO)
        .stateInViewModel(SharingStarted.WhileSubscribed(5_000), null)

    private val traveledSegments = combine(info, onlineState, timeFlow) { info, state, now ->
        when {
            info !is BusState.OK -> null
            info.stops.isEmpty() -> null
            date > SystemClock.todayHere() -> null
            date < SystemClock.todayHere() -> info.stops.lastIndex.coerceAtLeast(0)
            state?.onlineTimetable?.nextStopIndex != null -> (state.onlineTimetable.nextStopIndex.coerceAtLeast(1) - 1)
            state?.nextStopTime != null -> (info.stops.indexOfLast { it.time == state.nextStopTime }.coerceAtLeast(1) - 1)
            info.stops.last().time < now -> info.stops.lastIndex.coerceAtLeast(0)
            else -> info.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, onlineState, timeFlow, traveledSegments) { info, state, now, traveledSegments ->

        if (info !is BusState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        val delay = state?.delay ?: 0.minutes

        val departureFromLastStop = info.stops.getOrElse(traveledSegments) { info.stops.last() }.time + delay

        val netStopScheduledArrival = info.stops.getOrNull(traveledSegments + 1)?.run { arrival ?: time }
        val nextOnlineStop = state?.onlineTimetable?.stops?.getOrNull(traveledSegments + 1)
        val arrivalToNextStop = if (nextOnlineStop != null && netStopScheduledArrival != null)
            netStopScheduledArrival + nextOnlineStop.delay + delay.toDateTimePeriod().seconds.seconds
        else netStopScheduledArrival?.plus(delay)

        val length = arrivalToNextStop?.minus(departureFromLastStop) ?: Duration.INFINITE

        val passed = (now - departureFromLastStop).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed / length).toFloat().coerceIn(0F, 1F)
    }

    val state = combine(info, traveledSegments, lineHeight, onlineState) { info, traveledSegments, lineHeight, onlineState ->
        if (info !is BusState.OK) info
        else info.copy(
            shouldBeOnline = onlineState != null && info.shouldBeOnline,
            lineHeight = lineHeight,
            traveledSegments = traveledSegments ?: 0,
            online = if (onlineState?.onlineTimetable != null) BusState.OnlineState(
                onlineConnStops = onlineState.onlineTimetable.stops,
                running = if (onlineState.delay != null || onlineState.nextStopTime != null) BusState.RunningState(
                    delay = onlineState.delay,
                    confirmedLowFloor = onlineState.confirmedLowFloor,
                    nextStopIndex = (traveledSegments ?: 0) + 1
                    //onlineState.onlineTimetable.nextStopIndex ?: state.stops.indexOfFirst { it.time == onlineState.nextStopTime },
                ) else null
            ) else null
        )
    }.stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), BusState.Loading)
}