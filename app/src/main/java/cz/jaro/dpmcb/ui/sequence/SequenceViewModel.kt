package cz.jaro.dpmcb.ui.sequence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.noCode
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.data.makeFixedCodesReadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SequenceViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val originalSequence: String,
) : ViewModel() {

    private val info: Flow<SequenceState> = repo.date.map { date ->
        val (sequence, before, buses, after, timeCodes, fixedCodes) = (
            repo.sequence(originalSequence, date) ?: return@map SequenceState.DoesNotExist(originalSequence, repo.seqName(originalSequence), date.toCzechLocative())
        )

        val runningBus = buses.find { (_, stops) ->
            stops.first().time <= LocalTime.now() && LocalTime.now() <= stops.last().time
        }

        SequenceState.Offline(
            sequence = sequence,
            sequenceName = repo.seqName(sequence),
            timeCodes = timeCodes.filterNot {
                !it.runs && it.`in`.start == noCode && it.`in`.endInclusive == noCode
            }.groupBy({ it.runs }, {
                if (it.`in`.start != it.`in`.endInclusive) "od ${it.`in`.start.asString()} do ${it.`in`.endInclusive.asString()}" else it.`in`.start.asString()
            }).map { (runs, dates) ->
                (if (runs) "Jede " else "Nejede ") + dates.joinToString()
            },
            fixedCodes = makeFixedCodesReadable(fixedCodes),
            before = before.map { it to repo.seqConnection(it) },
            after = after.map { it to repo.seqConnection(it) },
            buses = buses.map { (bus, stops) ->
                BusInSequence(
                    busId = bus.connId,
                    stops = stops,
                    lineNumber = bus.line,
                    lowFloor = bus.lowFloor,
                    isRunning = false,
                    shouldBeRunning = runningBus?.info?.connId == bus.connId && date == LocalDate.now(),
                )
            },
            runsToday = repo.runsAt(timeCodes = timeCodes, fixedCodes = fixedCodes, date = LocalDate.now()),
            height = 0F,
            traveledSegments = 0,
        )
    }

    private val nowRunningOnlineConn = combine(info, onlineRepo.nowRunningBuses(), repo.date) { info, onlineConns, date ->
        if (date != LocalDate.now()) return@combine null
        if (info !is SequenceState.OK) return@combine null
        val onlineConn = onlineConns.find { onlineConn -> onlineConn.id in info.buses.map { it.busId } }
        if (onlineConn?.delayMin == null) return@combine null
        return@combine onlineConn
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val traveledSegments = combine(info, nowRunningOnlineConn, UtilFunctions.nowFlow, repo.date) { info, onlineConn, now, date ->

        if (info !is SequenceState.OK) return@combine null

        val runningBus = onlineConn?.let { info.buses.find { it.busId == onlineConn.id } }
            ?: info.buses.find { (_, stops) ->
                stops.first().time <= LocalTime.now() && LocalTime.now() <= stops.last().time
            }

        when {
            runningBus == null -> null
            date != LocalDate.now() -> null
            // Je na mapě
            onlineConn?.nextStop != null -> runningBus.stops.indexOfLast { it.time == onlineConn.nextStop }.coerceAtLeast(1) - 1
            runningBus.stops.last().time < now -> runningBus.stops.lastIndex
            else -> runningBus.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, nowRunningOnlineConn, UtilFunctions.nowFlow, traveledSegments) { info, onlineConn, now, traveledSegments ->

        if (info !is SequenceState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        val runningBus = onlineConn?.let { info.buses.find { it.busId == onlineConn.id } }
            ?: info.buses.find { (_, stops) ->
                stops.first().time <= LocalTime.now() && LocalTime.now() <= stops.last().time
            }

        if (runningBus == null) return@combine 0F

        if (runningBus.stops.lastIndex < traveledSegments) return@combine 0F

        val departureFromLastStop = runningBus.stops[traveledSegments].time + (onlineConn?.delayMin?.toDouble()?.minutes ?: 0.minutes)

        val arrivalToNextStop = (runningBus.stops.getOrNull(traveledSegments + 1)?.time?.plus(onlineConn?.delayMin?.toDouble()?.minutes ?: 0.minutes))

        val length = arrivalToNextStop?.let { Duration.between(departureFromLastStop, it) } ?: Duration.ofSeconds(Long.MAX_VALUE)

        val passed = Duration.between(departureFromLastStop, now).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.seconds / length.seconds.toFloat()).coerceAtMost(1F)
    }

    val state = combine(info, traveledSegments, lineHeight, nowRunningOnlineConn, repo.date) { info, traveledSegments, lineHeight, onlineConn, date ->
        if (date != LocalDate.now()) return@combine info
        if (info !is SequenceState.OK) return@combine info
        val newInfo = (info as SequenceState.Offline).copy(
            height = lineHeight,
            traveledSegments = traveledSegments ?: 0,
        )
        if (onlineConn?.delayMin == null) return@combine newInfo
        SequenceState.Online(
            state = newInfo,
            delayMin = onlineConn.delayMin,
            vehicle = onlineConn.vehicle,
            confirmedLowFloor = onlineConn.lowFloor,
        ).copy(
            buses = newInfo.buses.map {
                it.copy(
                    isRunning = it.busId == onlineConn.id
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SequenceState.Loading)
}