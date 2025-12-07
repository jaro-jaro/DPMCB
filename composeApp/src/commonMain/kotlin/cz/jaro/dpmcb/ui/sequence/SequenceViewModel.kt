package cz.jaro.dpmcb.ui.sequence

//import com.fleeksoft.ksoup.Ksoup
//import com.fleeksoft.ksoup.network.parseGetRequest
import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.filterFixedCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.filterTimeCodesAndMakeReadable
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.runsAt
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import cz.jaro.dpmcb.data.helperclasses.timeFlow
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.validityString
import cz.jaro.dpmcb.data.seqConnection
import cz.jaro.dpmcb.data.seqName
import cz.jaro.dpmcb.data.vehicleName
import cz.jaro.dpmcb.data.vehicleTraction
import cz.jaro.dpmcb.ui.common.TimetableEvent
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SequenceViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val sequence: SequenceCode,
        val date: LocalDate,
    )

    lateinit var navigator: Navigator

    private val info: Flow<SequenceState> = suspend info@{
        val sequence = repo.sequence(params.sequence, params.date)
            ?: return@info SequenceState.DoesNotExist(params.sequence, with(repo) { params.sequence.seqName() }, params.date)

        val runningBus = sequence.buses.find { (_, stops) ->
            stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= stops.last().time
        }

        SequenceState.OK(
            sequence = sequence.name,
            sequenceName = with(repo) { sequence.name.seqName() },
            timeCodes = filterTimeCodesAndMakeReadable(sequence.commonTimeCodes),
            fixedCodes = filterFixedCodesAndMakeReadable(sequence.commonFixedCodes, sequence.commonTimeCodes),
            lineCode = sequence.commonValidity?.let { validityString(it) } ?: "",
            lineTraction = sequence.commonLineTraction,
            before = sequence.before.map { it to with(repo) { it.seqConnection() } },
            after = sequence.after.map { it to with(repo) { it.seqConnection() } },
            buses = sequence.buses.map { bus ->
                val runsToday = runsAt(
                    timeCodes = sequence.commonTimeCodes + bus.uniqueTimeCodes,
                    fixedCodes = sequence.commonFixedCodes + bus.uniqueFixedCodes,
                    date = params.date
                )
                BusInSequence(
                    busName = bus.info.connName,
                    stops = bus.stops,
                    lineNumber = bus.info.line.toShortLine(),
                    lowFloor = bus.info.lowFloor,
                    isRunning = false,
                    shouldBeRunning = runningBus?.info?.connName == bus.info.connName && params.date == SystemClock.todayHere() && runsToday,
                    timeCodes = filterTimeCodesAndMakeReadable(bus.uniqueTimeCodes),
                    fixedCodes = filterFixedCodesAndMakeReadable(bus.uniqueFixedCodes, bus.uniqueTimeCodes),
                    lineCode = bus.uniqueValidity?.let { validityString(it) } ?: "",
                    direction = bus.info.direction,
                    isOneWay = repo.isOneWay(bus.info.line),
                )
            },
            runsToday = runsAt(timeCodes = sequence.commonTimeCodes, fixedCodes = sequence.commonFixedCodes, date = params.date),
            height = 0F,
            traveledSegments = 0,
            date = params.date,
            vehicleNumber = null,
            vehicleName = null,
            vehicleTraction = null,
        )
    }.asFlow()

    private val nowRunningOnlineConn = combine(info, onlineRepo.nowRunningBuses()) { info, onlineConns ->
        if (params.date != SystemClock.todayHere()) return@combine null
        if (info !is SequenceState.OK) return@combine null
        val onlineConn = onlineConns.find { onlineConn -> onlineConn.name in info.buses.map { it.busName } }
        if (onlineConn?.delayMin == null) return@combine null
        return@combine onlineConn
    }
        .flowOn(Dispatchers.IO)
        .stateInViewModel(SharingStarted.WhileSubscribed(5_000), null)

    private val traveledSegments = combine(info, nowRunningOnlineConn, timeFlow) { info, onlineConn, now ->

        if (info !is SequenceState.OK) return@combine null

        val runningBus = onlineConn?.let { info.buses.find { it.busName == onlineConn.name } }
            ?: info.buses.find { (_, stops) ->
                stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= stops.last().time
            }

        when {
            runningBus == null -> null
            params.date != SystemClock.todayHere() -> null
            // Je na mapě
            onlineConn?.nextStop != null -> runningBus.stops.indexOfLast { it.time == onlineConn.nextStop }.coerceAtLeast(1) - 1
            runningBus.stops.last().time < now -> runningBus.stops.lastIndex
            else -> runningBus.stops.indexOfLast { it.time < now }.coerceAtLeast(0)
        }
    }

    private val lineHeight = combine(info, nowRunningOnlineConn, timeFlow, traveledSegments) { info, onlineConn, now, traveledSegments ->

        if (info !is SequenceState.OK) return@combine 0F

        if (traveledSegments == null) return@combine 0F

        val runningBus = onlineConn?.let { info.buses.find { it.busName == onlineConn.name } }
            ?: info.buses.find { (_, stops) ->
                stops.first().time <= SystemClock.timeHere() && SystemClock.timeHere() <= stops.last().time
            }

        if (runningBus == null) return@combine 0F

        if (runningBus.stops.lastIndex < traveledSegments) return@combine 0F

        val departureFromLastStop = runningBus.stops[traveledSegments].time + (onlineConn?.delayMin?.toDouble() ?: .0).seconds

        val arrivalToNextStop =
            (runningBus.stops.getOrNull(traveledSegments + 1)?.time?.plus(onlineConn?.delayMin?.toDouble()?.seconds ?: 0.seconds))

        val length = arrivalToNextStop?.minus(departureFromLastStop) ?: Duration.INFINITE

        val passed = (now - departureFromLastStop).coerceAtLeast(Duration.ZERO)

        traveledSegments + (passed.inWholeSeconds / length.inWholeSeconds.toFloat()).coerceAtMost(1F)
    }

    private val downloadedVehicle = MutableStateFlow(null as RegistrationNumber?)

    val state = cz.jaro.dpmcb.data.helperclasses.combine(
        info,
        traveledSegments,
        lineHeight,
        nowRunningOnlineConn,
        downloadedVehicle,
        repo.vehicleNumbersOnSequences,
    ) { info, traveledSegments, lineHeight, onlineConn, downloadedVehicle, vehicles ->
        if (info !is SequenceState.OK) return@combine info
        val vehicle = vehicles[info.date]?.get(info.sequence) ?: downloadedVehicle
        info.copy(
            vehicleNumber = vehicle,
            vehicleName = vehicle?.let(repo::vehicleName),
            vehicleTraction = vehicle?.let { repo.vehicleTraction(it) ?: info.lineTraction },
        ).let { info2 ->
            if (params.date != SystemClock.todayHere()) return@combine info2
            info2.copy(
                height = lineHeight,
                traveledSegments = traveledSegments ?: 0,
            ).let { info3 ->
                if (onlineConn?.delayMin == null) return@combine info3
                info3.copy(
                    online = SequenceState.OnlineState(
                        delay = onlineConn.delayMin.toDouble().minutes,
                        confirmedLowFloor = onlineConn.lowFloor,
                    ),
                    buses = info3.buses.map {
                        it.copy(
                            isRunning = it.busName == onlineConn.name
                        )
                    },
                )
            }
        }
    }.stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), SequenceState.Loading)

    fun onEvent(e: SequenceEvent) = when (e) {
        is SequenceEvent.BusClick -> navigator.navigate(Route.Bus(params.date, e.busName))
        is SequenceEvent.SequenceClick -> navigator.navigate(Route.Sequence(params.date, e.sequence))
        is SequenceEvent.TimetableClick -> when (e.e) {
            is TimetableEvent.StopClick -> navigator.navigate(Route.Departures(params.date, e.e.stopName, e.e.time.toSimpleTime()))
            is TimetableEvent.TimetableClick -> navigator.navigate(Route.Timetable(params.date, e.e.line, e.e.stop, e.e.direction))
        }

        is SequenceEvent.FindBus -> {
            val state = state.value
            require(state is SequenceState.OK)
            launch {
//                val isNight = params.sequence.line().startsWith('5') &&
//                        params.sequence.line().length == 2
//                val isMorning = params.sequence.modifiers().part() == 2
//                val yesterday = state.date - 1.days
//                val doc = try {
//                    val date = if (isNight && isMorning) yesterday else state.date
//
//                    Ksoup.parseGetRequest(
//                        "https://seznam-autobusu.cz/vypravenost/mhd-cb/vypis?datum=${date}&linka=${params.sequence.line()}&kurz=${params.sequence.sequenceNumber()}"
//                    )
//                } catch (ex: Exception) {
//                    ex.printStackTrace()
//                    recordException(ex)
//                    e.onLost()
//                    return@launch
//                }
//                val data = doc
//                    .body()
//                    .select("#snippet--table > div > table > tbody")
//                    .single()
//                    .children()
//                    .filter { !it.hasClass("table-header") }
//                    .map {
//                        Pair(
//                            it.getElementsByClass("car").single().text().toRegNum(),
//                            it.getElementsByClass("note").single().text(),
//                        )
//                    }
//
//                downloadedVehicle.value =
//                    if (data.isEmpty()) null.also { e.onLost() }
//                    else if (data.size == 1) data.single().first.also(e.onFound)
//                    else {
//                        val cast = when (params.sequence.modifiers().part()) {
//                            1 -> "ran"
//                            2 -> "odpo"
//                            else -> null
//                        }
//                        if (cast == null) null.also { e.onLost() }
//                        else data.find { it.second.contains(cast) }?.first?.also(e.onFound) ?: null.also { e.onLost() }
//                    }
//                if (downloadedVehicle.value != null)
//                    repo.pushVehicle(state.date, state.sequence, downloadedVehicle.value!!, reliable = false)
            }
            Unit
        }

        is SequenceEvent.ChangeDate -> navigator.navigate(Route.Sequence(e.date, params.sequence))
    }
}