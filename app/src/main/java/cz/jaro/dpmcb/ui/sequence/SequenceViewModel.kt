package cz.jaro.dpmcb.ui.sequence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.noCode
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.now
import cz.jaro.dpmcb.data.makeFixedCodesReadable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SequenceViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val originalSequence: String,
) : ViewModel() {

    private val info: Flow<SequenceState> = repo.date.map { date ->
        val (sequence, before, buses, after, timeCodes, fixedCodes) = (
                repo.sequence(originalSequence, date) ?: return@map SequenceState.DoesNotExist(originalSequence)
                )

        val runningBus = buses.find { (_, stops) ->
            stops.first().time <= LocalTime.now() && LocalTime.now() <= stops.last().time
        }

        val traveledSegments = when {
            runningBus == null -> null
            date > LocalDate.now() -> null
            date < LocalDate.now() -> runningBus.stops.lastIndex
            runningBus.stops.last().time < now -> runningBus.stops.lastIndex
            else -> runningBus.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }

        val height = if (traveledSegments == null || runningBus == null) 0F else {
            val departureFromLastStop = runningBus.stops[traveledSegments].time

            val arrivalToNextStop = runningBus.stops.getOrNull(traveledSegments + 1)?.time

            val length = arrivalToNextStop?.let { Duration.between(departureFromLastStop, it) } ?: Duration.ofSeconds(Long.MAX_VALUE)

            val passed = Duration.between(departureFromLastStop, now).coerceAtLeast(Duration.ZERO)

            traveledSegments + (passed.seconds / length.seconds.toFloat()).coerceAtMost(1F)
        }

        SequenceState.OK.Offline(
            sequence = sequence,
            timeCodes = timeCodes.filterNot {
                !it.runs && it.`in`.start == noCode && it.`in`.endInclusive == noCode
            }.groupBy({ it.runs }, {
                if (it.`in`.start != it.`in`.endInclusive) "od ${it.`in`.start.asString()} do ${it.`in`.endInclusive.asString()}" else it.`in`.start.asString()
            }).map { (runs, dates) ->
                (if (runs) "Jede " else "Nejede ") + dates.joinToString()
            },
            fixedCodes = makeFixedCodesReadable(fixedCodes),
            before = before,
            after = after,
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
            height = height,
            traveledSegments = traveledSegments ?: 0,
        )
    }

    val state = combine(info, onlineRepo.nowRunningBuses(), repo.date) { info, onlineConns, date ->
        if (date != LocalDate.now()) return@combine info
        if (info !is SequenceState.OK) return@combine info
        val onlineConn = onlineConns.find { onlineConn -> onlineConn.id in info.buses.map { it.busId } }
        if (onlineConn?.delayMin == null) return@combine info
        SequenceState.OK.Online(
            state = info,
            delayMin = onlineConn.delayMin,
            vehicle = onlineConn.vehicle,
            confirmedLowFloor = onlineConn.lowFloor
        ).copy(
            buses = info.buses.map {
                if (it.busId != onlineConn.id) it else it.copy(
                    isRunning = true
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SequenceState.Loading)
}