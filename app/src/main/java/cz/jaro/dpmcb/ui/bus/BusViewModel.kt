package cz.jaro.dpmcb.ui.bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.PartOfConn
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.noCode
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nowFlow
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.makeFixedCodesReadable
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
    @InjectedParam private val spojId: String,
) : ViewModel() {

    private val info: Flow<BusState> = combine(repo.date, repo.favourites, repo.hasAccessToMap) { date, favourites, online ->
        val exists = repo.doesBusExist(spojId)
        if (!exists) return@combine BusState.DoesNotExist(spojId)
        val runsAt = repo.doesConnRunAt(spojId)
        val validity = repo.lineValidity(spojId, date)
        if (!runsAt(date)) {
            val (timeCodes, fixedCodes) = repo.codes(spojId, date)
            return@combine BusState.DoesNotRun(
                busId = spojId,
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
                busName = spojId.split("-").let { "${it[1]}/${it[2]}" },
                deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$spojId",

            )
        }

        val (bus, stops, timeCodes, fixedCodes, sequence) = repo.busDetail(spojId, date)
        val restriction = repo.hasRestriction(spojId, date)
        BusState.OK.Offline(
            busId = spojId,
            stops = stops,
            lineNumber = bus.line,
            lowFloor = bus.lowFloor,
            timeCodes = timeCodes.filterNot {
                !it.runs && it.`in`.start == noCode && it.`in`.endInclusive == noCode
            }.groupBy({ it.runs }, {
                if (it.`in`.start != it.`in`.endInclusive) "od ${it.`in`.start.asString()} do ${it.`in`.endInclusive.asString()}" else it.`in`.start.asString()
            }).map { (runs, dates) ->
                (if (runs) "Jede " else "Nejede ") + dates.joinToString()
            },
            fixedCodes = makeFixedCodesReadable(fixedCodes),
            lineCode = "JŘ linky platí od ${validity.validFrom.asString()} do ${validity.validTo.asString()}",
            busName = spojId.split("-").let { "${it[1]}/${it[2]}" },
            deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$spojId",
            restriction = restriction,
            favourite = favourites.find { it.busId == spojId },
            lineHeight = 0F,
            traveledSegments = 0,
            error = online && date == LocalDate.now() && stops.first().time <= LocalTime.now() && LocalTime.now() <= stops.last().time,
            sequence = bus.sequence,
            sequenceName = bus.sequence?.let { repo.seqName(bus.sequence) } ?: "",
            previousBus = sequence?.let {
                val i = it.indexOf(spojId)
                it.getOrNull(i - 1)
            },
            nextBus = sequence?.let {
                val i = it.indexOf(spojId)
                it.getOrNull(i + 1)
            },
        )
    }

    fun removeFavourite() {
        viewModelScope.launch {
            repo.removeFavourite(spojId)
        }
    }

    fun changeFavourite(cast: PartOfConn) {
        viewModelScope.launch {
            repo.changeFavourite(cast)
        }
    }

    fun changeDate(datum: LocalDate) {
        viewModelScope.launch {
            repo.changeDate(datum)
        }
    }

    private val onlineState = onlineRepo.busById(spojId).map { (onlineConn, onlineConnStops) ->
        OnlineBusState(
            delay = onlineConn?.delayMin?.toDouble()?.minutes,
            onlineConnStops = onlineConnStops,
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
            // Je na mapě && má detail spoje
            state.nextStopTime != null -> info.stops.indexOfLast { it.time == state.nextStopTime }.coerceAtLeast(1) - 1
            info.stops.last().time < now -> info.stops.lastIndex
            else -> info.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, onlineState, nowFlow, traveledSegments) { info, state, now, traveledSegments ->

        if (info !is BusState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        val departureFromLastStop = info.stops[traveledSegments].time + (state.delay ?: 0.minutes)

        val arrivalToNextStop = state.onlineConnStops?.getOrNull(traveledSegments + 1)?.let {
            it.scheduledTime + it.delay.minutes + (state.delay?.inWholeSeconds?.rem(60)?.seconds ?: 0.seconds)
        } ?: (info.stops.getOrNull(traveledSegments + 1)?.time?.plus(state.delay ?: 0.minutes))

        val length = arrivalToNextStop?.let { Duration.between(departureFromLastStop, it) } ?: Duration.ofSeconds(Long.MAX_VALUE)

        val passed = Duration.between(departureFromLastStop, now).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.seconds / length.seconds.toFloat()).coerceAtMost(1F)
    }

    val state = combine(info, traveledSegments, lineHeight, onlineState) { info, traveledSegments, lineHeight, onlineState ->
        if (info !is BusState.OK) info
        else (info as BusState.OK.Offline).copy(
            lineHeight = lineHeight,
            traveledSegments = traveledSegments ?: 0
        ).let { state ->
            if (onlineState.delay == null || onlineState.onlineConnStops == null || onlineState.nextStopTime == null) state
            else BusState.OK.Online(
                state = state,
                onlineConnStops = onlineState.onlineConnStops,
                delayMin = onlineState.delay.inWholeSeconds.div(60F),
                vehicle = onlineState.vehicle,
                confirmedLowFloor = onlineState.confirmedLowFloor,
                nextStop = onlineState.nextStopTime,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), BusState.Loading)
}