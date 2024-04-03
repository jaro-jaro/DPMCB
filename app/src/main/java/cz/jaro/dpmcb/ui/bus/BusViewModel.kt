package cz.jaro.dpmcb.ui.bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.noCode
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nowFlow
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.makeFixedCodesReadable
import cz.jaro.dpmcb.ui.destinations.BusDestination
import cz.jaro.dpmcb.ui.destinations.SequenceDestination
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class BusViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val busId: String,
    @InjectedParam private val navigate: NavigateFunction,
    @InjectedParam private val pop: () -> Unit,
) : ViewModel() {

    private val info: Flow<BusState> = combine(repo.date, repo.favourites, repo.hasAccessToMap) { date, favourites, online ->
        val exists = repo.doesBusExist(busId)
        if (!exists) return@combine BusState.DoesNotExist(busId)
        val runsAt = repo.doesConnRunAt(busId)
        val validity = repo.lineValidity(busId, date)
        if (!runsAt(date)) {
            val (timeCodes, fixedCodes) = repo.codes(busId, date)
            return@combine BusState.DoesNotRun(
                busId = busId,
                date = date,
                runsNextTimeAfterToday = List(365) { LocalDate.now().plusDays(it.toLong()) }.firstOrNull { runsAt(it) },
                runsNextTimeAfterDate = List(365) { date.plusDays(it.toLong()) }.firstOrNull { runsAt(it) },
                timeCodes = timeCodes.filterNot {
                    !it.runs && it.`in`.start == noCode && it.`in`.endInclusive == noCode
                }.groupBy({ it.runs }, {
                    if (it.`in`.start != it.`in`.endInclusive) "od ${it.`in`.start.asString()} do ${it.`in`.endInclusive.asString()}" else it.`in`.start.asString()
                }).map { (runs, dates) ->
                    (if (runs) "Jede " else "Nejede ") + dates.joinToString()
                },
                fixedCodes = fixedCodes,
                lineCode = "JŘ linky platí od ${validity.validFrom.asString()} do ${validity.validTo.asString()}",
                busName = busId.split("-").let { "${it[1]}/${it[2]}" },
                deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$busId",

                )
        }

        val bus = repo.busDetail(busId, date)
        val restriction = repo.hasRestriction(busId, date)
        BusState.Offline(
            busId = busId,
            stops = bus.stops,
            lineNumber = bus.info.line,
            lowFloor = bus.info.lowFloor,
            timeCodes = bus.timeCodes.filterNot {
                !it.runs && it.`in`.start == noCode && it.`in`.endInclusive == noCode
            }.groupBy({ it.runs }, {
                if (it.`in`.start != it.`in`.endInclusive) "od ${it.`in`.start.asString()} do ${it.`in`.endInclusive.asString()}" else it.`in`.start.asString()
            }).map { (runs, dates) ->
                (if (runs) "Jede " else "Nejede ") + dates.joinToString()
            },
            fixedCodes = makeFixedCodesReadable(bus.fixedCodes),
            lineCode = "JŘ linky platí od ${validity.validFrom.asString()} do ${validity.validTo.asString()}",
            busName = busId.split("-").let { "${it[1]}/${it[2]}" },
            deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$busId",
            restriction = restriction,
            favourite = favourites.find { it.busId == busId },
            lineHeight = 0F,
            traveledSegments = 0,
            error = online && date == LocalDate.now() && bus.stops.first().time <= LocalTime.now() && LocalTime.now() <= bus.stops.last().time,
            sequence = bus.info.sequence,
            sequenceName = bus.info.sequence?.let { repo.seqName(bus.info.sequence) },
            previousBus = bus.sequence?.let { seq ->
                val i = seq.indexOf(busId)
                seq.getOrNull(i - 1)?.to(false)
                    ?: bus.before?.let {
                        if (it.size != 1) null
                        else repo.lastBusOfSequence(it.first(), date) to true
                    }
            },
            nextBus = bus.sequence?.let { seq ->
                val i = seq.indexOf(busId)
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
                pop()
                navigate(BusDestination(state.nextBus!!.first))
            }
            Unit
        }
        BusEvent.PreviousBus -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null && state.previousBus != null) {
                if (state.previousBus!!.second) viewModelScope.launch(Dispatchers.Main) {
                    repo.makeText("Změněn kurz!").show()
                }
                pop()
                navigate(BusDestination(state.previousBus!!.first))
            }
            Unit
        }
        BusEvent.RemoveFavourite -> {
            viewModelScope.launch {
                repo.removeFavourite(busId)
            }
            Unit
        }

        BusEvent.ShowSequence -> {
            val state = state.value
            if (state is BusState.OK && state.sequence != null) {
                navigate(SequenceDestination(state.sequence!!))
            }
            Unit
        }
    }

    private val onlineState = onlineRepo.busById(busId).map { (onlineConn, onlineConnDetail) ->
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
            date > LocalDate.now() -> null
            date < LocalDate.now() -> info.stops.lastIndex
            state.onlineConnDetail?.nextStopIndex != null -> state.onlineConnDetail.nextStopIndex - 1
            state.nextStopTime != null -> info.stops.indexOfLast { it.time == state.nextStopTime }.coerceAtLeast(1) - 1
            info.stops.last().time < now -> info.stops.lastIndex
            else -> info.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, onlineState, nowFlow, traveledSegments) { info, state, now, traveledSegments ->

        if (info !is BusState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        val departureFromLastStop = info.stops[traveledSegments].time + (state.delay ?: 0.minutes)

        val arrivalToNextStop = state.onlineConnDetail?.stops?.getOrNull(traveledSegments + 1)?.let {
            it.scheduledTime + it.delay.minutes + (state.delay?.inWholeSeconds?.rem(60)?.seconds ?: 0.seconds)
        } ?: (info.stops.getOrNull(traveledSegments + 1)?.time?.plus(state.delay ?: 0.minutes))

        val length = arrivalToNextStop?.let { Duration.between(departureFromLastStop, it) } ?: Duration.ofSeconds(Long.MAX_VALUE)

        val passed = Duration.between(departureFromLastStop, now).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.seconds / length.seconds.toFloat()).coerceIn(0F, 1F)
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