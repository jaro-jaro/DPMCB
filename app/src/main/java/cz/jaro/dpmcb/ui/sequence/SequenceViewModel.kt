package cz.jaro.dpmcb.ui.sequence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.modifiers
import cz.jaro.dpmcb.data.entities.part
import cz.jaro.dpmcb.data.entities.sequenceNumber
import cz.jaro.dpmcb.data.entities.toRegNum
import cz.jaro.dpmcb.data.filterFixedCodesAndMakeReadable
import cz.jaro.dpmcb.data.filterTimeCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.minus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.validityString
import cz.jaro.dpmcb.ui.common.TimetableEvent
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SequenceViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val sequence: SequenceCode,
    )

    lateinit var navigate: (Route) -> Unit

    private val info: Flow<SequenceState> = repo.date.map { date ->
        val sequence = repo.sequence(params.sequence, date)
            ?: return@map SequenceState.DoesNotExist(params.sequence, with(repo) { params.sequence.seqName() }, date)

        val runningBus = sequence.buses.find { (_, stops) ->
            stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= stops.last().time
        }

        SequenceState.Offline(
            sequence = sequence.name,
            sequenceName = with(repo) { sequence.name.seqName() },
            timeCodes = filterTimeCodesAndMakeReadable(sequence.commonTimeCodes),
            fixedCodes = filterFixedCodesAndMakeReadable(sequence.commonFixedCodes, sequence.commonTimeCodes),
            lineCode = sequence.commonValidity?.let { validityString(it) } ?: "",
            before = sequence.before.map { it to with(repo) { it.seqConnection() } },
            after = sequence.after.map { it to with(repo) { it.seqConnection() } },
            buses = sequence.buses.map { bus ->
                val runsToday = repo.runsAt(
                    timeCodes = sequence.commonTimeCodes + bus.uniqueTimeCodes,
                    fixedCodes = sequence.commonFixedCodes + bus.uniqueFixedCodes,
                    date = SystemClock.todayHere()
                )
                BusInSequence(
                    busName = bus.info.connName,
                    stops = bus.stops,
                    lineNumber = bus.info.line,
                    lowFloor = bus.info.lowFloor,
                    isRunning = false,
                    shouldBeRunning = runningBus?.info?.connName == bus.info.connName && date == SystemClock.todayHere() && runsToday,
                    timeCodes = filterTimeCodesAndMakeReadable(bus.uniqueTimeCodes),
                    fixedCodes = filterFixedCodesAndMakeReadable(bus.uniqueFixedCodes, bus.uniqueTimeCodes),
                    lineCode = bus.uniqueValidity?.let { validityString(it) } ?: "",
                )
            },
            runsToday = repo.runsAt(timeCodes = sequence.commonTimeCodes, fixedCodes = sequence.commonFixedCodes, date = SystemClock.todayHere()),
            height = 0F,
            traveledSegments = 0,
            date = date,
            vehicle = null,
        )
    }

    private val nowRunningOnlineConn = combine(info, onlineRepo.nowRunningBuses(), repo.date) { info, onlineConns, date ->
        if (date != SystemClock.todayHere()) return@combine null
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
                stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= stops.last().time
            }

        when {
            runningBus == null -> null
            date != SystemClock.todayHere() -> null
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
                stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= stops.last().time
            }

        if (runningBus == null) return@combine 0F

        if (runningBus.stops.lastIndex < traveledSegments) return@combine 0F

        val departureFromLastStop = runningBus.stops[traveledSegments].time + (onlineConn?.delayMin?.toDouble() ?: .0).seconds

        val arrivalToNextStop = (runningBus.stops.getOrNull(traveledSegments + 1)?.time?.plus(onlineConn?.delayMin?.toDouble()?.seconds ?: 0.seconds))

        val length = arrivalToNextStop?.minus(departureFromLastStop) ?: Duration.INFINITE

        val passed = (now - departureFromLastStop).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.inWholeSeconds / length.inWholeSeconds.toFloat()).coerceAtMost(1F)
    }

    private val downloadedVehicle = MutableStateFlow(null as RegistrationNumber?)

    val state = combine(
        info,
        traveledSegments,
        lineHeight,
        nowRunningOnlineConn,
        repo.date,
        downloadedVehicle
    ) { info, traveledSegments, lineHeight, onlineConn, date, downloadedVehicle ->
        if (info !is SequenceState.OK) return@combine info
        val newInfo = (info as SequenceState.Offline).copy(
            vehicle = downloadedVehicle,
        )
        if (date != SystemClock.todayHere()) return@combine newInfo
        val newerInfo = newInfo.copy(
            height = lineHeight,
            traveledSegments = traveledSegments ?: 0,
        )
        if (onlineConn?.delayMin == null) return@combine newerInfo
        SequenceState.Online(
            state = newerInfo,
            delayMin = onlineConn.delayMin,
            vehicle = onlineConn.vehicle,
            confirmedLowFloor = onlineConn.lowFloor,
        ).copy(
            buses = newerInfo.buses.map {
                it.copy(
                    isRunning = it.busName == onlineConn.name
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SequenceState.Loading)

    fun onEvent(e: SequenceEvent) = when (e) {
        is SequenceEvent.BusClick -> navigate(Route.Bus(e.busName))
        is SequenceEvent.SequenceClick -> navigate(Route.Sequence(e.sequence))
        is SequenceEvent.TimetableClick -> when (e.e) {
            is TimetableEvent.StopClick -> navigate(Route.Departures(e.e.stopName, e.e.time.toSimpleTime()))
            is TimetableEvent.TimetableClick -> navigate(Route.Timetable(e.e.line, e.e.stop, e.e.nextStop))
        }

        is SequenceEvent.FindBus -> {
            val state = state.value
            require(state is SequenceState.OK)
            viewModelScope.launch {
                val doc = Ksoup.parseGetRequest(
                    if (params.sequence.line().startsWith('5') && params.sequence.line().length == 2 && params.sequence.modifiers().part() == 2)
                        "https://seznam-autobusu.cz/vypravenost/mhd-cb/vypis?datum=${state.date.minus(1.days)}&linka=${params.sequence.line()}&kurz=${params.sequence.sequenceNumber()}"
                    else "https://seznam-autobusu.cz/vypravenost/mhd-cb/vypis?datum=${state.date}&linka=${params.sequence.line()}&kurz=${params.sequence.sequenceNumber()}".also(::println)
                )
                val data = doc
                    .body()
                    .select("#snippet--table > div > table > tbody")
                    .single()
                    .children().also(::println)
                    .filter { !it.hasClass("table-header") }
                    .map {
                        Pair(
                            it.getElementsByClass("car").single().text().toRegNum(),
                            it.getElementsByClass("note").single().text(),
                        )
                    }.also(::println)

                downloadedVehicle.value =
                    if (data.isEmpty()) null.also { e.onLost() }
                    else if (data.size == 1) data.single().first.also(e.onFound)
                    else {
                        val cast = when (params.sequence.modifiers().part()) {
                            1 -> "ran"
                            2 -> "odpo"
                            else -> null
                        }.also(::println)
                        if (cast == null) null.also { e.onLost() }
                        else data.find { it.second.contains(cast) }?.first?.also(e.onFound) ?: null.also { e.onLost() }
                    }
            }
            Unit
        }
    }
}