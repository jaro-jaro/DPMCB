package cz.jaro.dpmcb.ui.bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.navOptions
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.filterFixedCodesAndMakeReadable
import cz.jaro.dpmcb.data.filterTimeCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.NavigateWithOptionsFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.minus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nowFlow
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.invoke
import cz.jaro.dpmcb.data.helperclasses.time
import cz.jaro.dpmcb.data.helperclasses.today
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    @InjectedParam private val navigate: NavigateWithOptionsFunction,
) : ViewModel() {

    private val info: Flow<BusState> = combine(repo.date, repo.favourites, repo.hasAccessToMap) { date, favourites, online ->
        val exists = repo.doesBusExist(busName)
        if (!exists) return@combine BusState.DoesNotExist(busName)
        val runsAt = repo.doesConnRunAt(busName)
        val validity = repo.lineValidity(busName, date)
        if (!runsAt(date)) {
            val (timeCodes, fixedCodes) = repo.codes(busName, date)
            return@combine BusState.DoesNotRun(
                busName = busName,
                date = date,
                runsNextTimeAfterToday = List(365) { SystemClock.today() + it.days }.firstOrNull { runsAt(it) },
                runsNextTimeAfterDate = List(365) { date + it.days }.firstOrNull { runsAt(it) },
                timeCodes = filterTimeCodesAndMakeReadable(timeCodes),
                fixedCodes = filterFixedCodesAndMakeReadable(fixedCodes, timeCodes),
                lineCode = "JŘ linky platí od ${validity.validFrom.asString()} do ${validity.validTo.asString()}",
                deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$busName",
            )
        }

        val bus = repo.busDetail(busName, date)
        val restriction = repo.hasRestriction(busName, date)
        BusState.Offline(
            busName = busName,
            stops = bus.stops,
            lineNumber = bus.info.line,
            lowFloor = bus.info.lowFloor,
            timeCodes = filterTimeCodesAndMakeReadable(bus.timeCodes),
            fixedCodes = filterFixedCodesAndMakeReadable(bus.fixedCodes, bus.timeCodes),
            lineCode = "JŘ linky platí od ${validity.validFrom.asString()} do ${validity.validTo.asString()}",
            deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$busName",
            restriction = restriction,
            favourite = favourites.find { it.busName == busName },
            lineHeight = 0F,
            traveledSegments = 0,
            error = online && date == SystemClock.today() && bus.stops.first().time <= SystemClock.time() && SystemClock.time() <= bus.stops.last().time,
            sequence = bus.info.sequence,
            sequenceName = with(repo) { bus.info.sequence?.seqName() },
            previousBus = bus.sequence?.let { seq ->
                val i = seq.indexOf(busName)
                seq.getOrNull(i - 1)?.to(false)
                    ?: bus.before?.let {
                        if (it.size != 1) null
                        else repo.lastBusOfSequence(it.first(), date) to true
                    }
            },
            nextBus = bus.sequence?.let { seq ->
                val i = seq.indexOf(busName)
                seq.getOrNull(i + 1)?.to(false)
                    ?: bus.after?.let {
                        if (it.size != 1) null
                        else repo.firstBusOfSequence(it.first(), date) to true
                    }
            },
        )
    }

    fun onEvent(e: BusEvent) = when (e) {
        is BusEvent.ChangeDate -> {
            viewModelScope.launch {
                repo.changeDate(e.date)
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
                if (state.nextBus!!.second) viewModelScope.launch(Dispatchers.Main) {
                    repo.makeText("Změněn kurz!").show()
                }
                navigate(Route.Bus(state.nextBus!!.first), navOptions {
                    popUpTo<Route.Bus> {
                        inclusive = true
                    }
                })
            }
            Unit
        }

        BusEvent.PreviousBus -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null && state.previousBus != null) {
                if (state.previousBus!!.second) viewModelScope.launch(Dispatchers.Main) {
                    repo.makeText("Změněn kurz!").show()
                }
                navigate(Route.Bus(state.previousBus!!.first), navOptions {
                    popUpTo<Route.Bus> {
                        inclusive = true
                    }
                })
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
                navigate(Route.Sequence(state.sequence!!.value))
            }
            Unit
        }
    }

    private val onlineState = onlineRepo.busByName(busName).map { (onlineConn, onlineConnDetail) ->
        OnlineBusState(
            delay = onlineConn?.delayMin?.toDouble()?.minutes,
            onlineConnDetail = onlineConnDetail,
            vehicle = onlineConn?.vehicle,
            confirmedLowFloor = onlineConn?.lowFloor,
            nextStopTime = onlineConn?.nextStop
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnlineBusState())

    private val traveledSegments = combine(info, onlineState, nowFlow, repo.date) { info, state, now, date ->
        when {
            info !is BusState.OK -> null
            info.stops.isEmpty() -> null
            date > SystemClock.today() -> null
            date < SystemClock.today() -> info.stops.lastIndex
            state.onlineConnDetail?.nextStopIndex != null -> state.onlineConnDetail.nextStopIndex - 1
            state.nextStopTime != null -> info.stops.indexOfLast { it.time == state.nextStopTime }.coerceAtLeast(1) - 1
            info.stops.last().time < now -> info.stops.lastIndex
            else -> info.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, onlineState, nowFlow, traveledSegments, repo.date) { info, state, now, traveledSegments, date ->

        if (info !is BusState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        val departureFromLastStop = info.stops[traveledSegments].time.plus(state.delay ?: 0.minutes)

        val arrivalToNextStop = state.onlineConnDetail?.stops?.getOrNull(traveledSegments + 1)?.let {
            it.scheduledTime.plus(it.delay.minutes).plus(state.delay ?: 0.seconds)
        } ?: (info.stops.getOrNull(traveledSegments + 1)?.time?.plus(state.delay ?: 0.minutes))

        val length = arrivalToNextStop?.minus(departureFromLastStop) ?: Duration.INFINITE

        val passed = (now - departureFromLastStop).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.inWholeSeconds / length.inWholeSeconds.toFloat()).coerceIn(0F, 1F)
    }

    val state = combine(info, traveledSegments, lineHeight, onlineState) { info, traveledSegments, lineHeight, onlineState ->
        if (info !is BusState.OK) info
        else (info as BusState.Offline).copy(
            lineHeight = lineHeight,
            traveledSegments = traveledSegments ?: 0
        ).let { state ->
            onlineState.onlineConnDetail
            if (onlineState.onlineConnDetail == null) state
            else if (onlineState.delay == null || onlineState.nextStopTime == null) BusState.OnlineNotRunning(
                state = state,
                onlineConnStops = onlineState.onlineConnDetail.stops,
            )
            else BusState.OnlineRunning(
                state = state,
                onlineConnStops = onlineState.onlineConnDetail.stops,
                delayMin = onlineState.delay.inWholeSeconds.div(60F),
                vehicle = onlineState.vehicle,
                confirmedLowFloor = onlineState.confirmedLowFloor,
                nextStopIndex = onlineState.onlineConnDetail.nextStopIndex ?: state.stops.indexOfFirst { it.time == onlineState.nextStopTime },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), BusState.Loading)
}