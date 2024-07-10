package cz.jaro.dpmcb.ui.sequence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.filterFixedCodesAndMakeReadable
import cz.jaro.dpmcb.data.filterTimeCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.minus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.data.helperclasses.time
import cz.jaro.dpmcb.data.helperclasses.today
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SequenceViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val originalSequence: SequenceCode,
) : ViewModel() {

    val date = repo.date

    private val info: Flow<SequenceState> = repo.date.map { date ->
        val sequence = repo.sequence(originalSequence, date)
            ?: return@map SequenceState.DoesNotExist(originalSequence, with(repo) { originalSequence.seqName() }, date.toCzechLocative())

        val runningBus = sequence.buses.find { (_, stops) ->
            stops.first().time <= SystemClock.time() && SystemClock.time() <= stops.last().time
        }

        SequenceState.Offline(
            sequence = sequence.name,
            sequenceName = with(repo) { sequence.name.seqName() },
            timeCodes = filterTimeCodesAndMakeReadable(sequence.commonTimeCodes),
            fixedCodes = filterFixedCodesAndMakeReadable(sequence.commonFixedCodes, sequence.commonTimeCodes),
            before = sequence.before.map { it to with(repo) { it.seqConnection() } },
            after = sequence.after.map { it to with(repo) { it.seqConnection() } },
            buses = sequence.buses.map { bus ->
                val runsToday = repo.runsAt(
                    timeCodes = sequence.commonTimeCodes + bus.uniqueTimeCodes,
                    fixedCodes = sequence.commonFixedCodes + bus.uniqueFixedCodes,
                    date = SystemClock.today()
                )
                BusInSequence(
                    busName = bus.info.connName,
                    stops = bus.stops,
                    lineNumber = bus.info.line,
                    lowFloor = bus.info.lowFloor,
                    isRunning = false,
                    shouldBeRunning = runningBus?.info?.connName == bus.info.connName && date == SystemClock.today() && runsToday,
                    timeCodes = filterTimeCodesAndMakeReadable(bus.uniqueTimeCodes),
                    fixedCodes = filterFixedCodesAndMakeReadable(bus.uniqueFixedCodes, bus.uniqueTimeCodes),
                )
            },
            runsToday = repo.runsAt(timeCodes = sequence.commonTimeCodes, fixedCodes = sequence.commonFixedCodes, date = SystemClock.today()),
            height = 0F,
            traveledSegments = 0,
        )
    }

    private val nowRunningOnlineConn = combine(info, onlineRepo.nowRunningBuses(), repo.date) { info, onlineConns, date ->
        if (date != SystemClock.today()) return@combine null
        if (info !is SequenceState.OK) return@combine null
        val onlineConn = onlineConns.find { onlineConn -> onlineConn.name in info.buses.map { it.busName } }
        if (onlineConn?.delayMin == null) return@combine null
        return@combine onlineConn
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val traveledSegments = combine(info, nowRunningOnlineConn, UtilFunctions.nowFlow, repo.date) { info, onlineConn, now, date ->

        if (info !is SequenceState.OK) return@combine null

        val runningBus = onlineConn?.let { info.buses.find { it.busName == onlineConn.name } }
            ?: info.buses.find { (_, stops) ->
                stops.first().time <= SystemClock.time() && SystemClock.time() <= stops.last().time
            }

        when {
            runningBus == null -> null
            date != SystemClock.today() -> null
            // Je na mapě
            onlineConn?.nextStop != null -> runningBus.stops.indexOfLast { it.time == onlineConn.nextStop }.coerceAtLeast(1) - 1
            runningBus.stops.last().time < now -> runningBus.stops.lastIndex
            else -> runningBus.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, nowRunningOnlineConn, UtilFunctions.nowFlow, traveledSegments) { info, onlineConn, now, traveledSegments ->

        if (info !is SequenceState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        val runningBus = onlineConn?.let { info.buses.find { it.busName == onlineConn.name } }
            ?: info.buses.find { (_, stops) ->
                stops.first().time <= SystemClock.time() && SystemClock.time() <= stops.last().time
            }

        if (runningBus == null) return@combine 0F

        if (runningBus.stops.lastIndex < traveledSegments) return@combine 0F

        val departureFromLastStop = runningBus.stops[traveledSegments].time + (onlineConn?.delayMin?.toDouble() ?: .0).seconds

        val arrivalToNextStop = (runningBus.stops.getOrNull(traveledSegments + 1)?.time?.plus(onlineConn?.delayMin?.toDouble()?.seconds ?: 0.seconds))

        val length = arrivalToNextStop?.minus(departureFromLastStop) ?: Duration.INFINITE

        val passed = (now - departureFromLastStop).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.inWholeSeconds / length.inWholeSeconds.toFloat()).coerceAtMost(1F)
    }

    val state = combine(info, traveledSegments, lineHeight, nowRunningOnlineConn, repo.date) { info, traveledSegments, lineHeight, onlineConn, date ->
        if (date != SystemClock.today()) return@combine info
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
                    isRunning = it.busName == onlineConn.name
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SequenceState.Loading)
}