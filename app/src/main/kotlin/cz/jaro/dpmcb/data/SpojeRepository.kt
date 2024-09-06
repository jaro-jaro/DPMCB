package cz.jaro.dpmcb.data

import android.app.Application
import android.net.Uri
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.get
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import cz.jaro.dpmcb.data.database.Dao
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SeqGroup
import cz.jaro.dpmcb.data.entities.SeqOfConn
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.SequenceModifiers
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.entities.Validity
import cz.jaro.dpmcb.data.entities.changePart
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.generic
import cz.jaro.dpmcb.data.entities.hasPart
import cz.jaro.dpmcb.data.entities.hasType
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.isInvalid
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.modifiers
import cz.jaro.dpmcb.data.entities.part
import cz.jaro.dpmcb.data.entities.sequenceNumber
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.entities.typeChar
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.DoesNotRun
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.Runs
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.RunsAlso
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.RunsOnly
import cz.jaro.dpmcb.data.helperclasses.SequenceType
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.anyTrue
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.minus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.noCode
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechAccusative
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.realtions.BusInfo
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.MiddleStop
import cz.jaro.dpmcb.data.realtions.RunsFromTo
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.bus.BusDetail
import cz.jaro.dpmcb.data.realtions.departures.Departure
import cz.jaro.dpmcb.data.realtions.departures.StopOfDeparture
import cz.jaro.dpmcb.data.realtions.favourites.Favourite
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.data.realtions.favourites.StopOfFavourite
import cz.jaro.dpmcb.data.realtions.invoke
import cz.jaro.dpmcb.data.realtions.now_running.NowRunning
import cz.jaro.dpmcb.data.realtions.now_running.StopOfNowRunning
import cz.jaro.dpmcb.data.realtions.sequence.BusOfSequence
import cz.jaro.dpmcb.data.realtions.sequence.InterBusOfSequence
import cz.jaro.dpmcb.data.realtions.sequence.Sequence
import cz.jaro.dpmcb.data.realtions.sequence.TimeOfSequence
import cz.jaro.dpmcb.data.realtions.timetable.BusInTimetable
import cz.jaro.dpmcb.data.tuples.Quadruple
import cz.jaro.dpmcb.ui.bus.unaryPlus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.collections.filterNot as remove

//@Single
class SpojeRepository(
    ctx: Application,
    private val localDataSource: Dao,
    private val preferenceDataSource: PreferenceDataSource,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val remoteConfig = Firebase.remoteConfig

    private suspend fun getConfigActive() = try {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        if (!isOnline.value)
            remoteConfig.activate().await()
        else
            remoteConfig.fetchAndActivate().await()
    } catch (e: FirebaseRemoteConfigException) {
        e.printStackTrace()
        Firebase.crashlytics.recordException(e)
        try {
            remoteConfig.activate().await()
        } catch (e: FirebaseRemoteConfigException) {
            false
        }
    }

    private val configActive = ::getConfigActive.asFlow()

    private val sequenceTypes = configActive.map {
        Json.decodeFromString<Map<Char, SequenceType>>(remoteConfig["sequenceTypes"].asString())
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun SequenceModifiers.type() = typeChar()?.let { sequenceTypes.first()[it] }

    private val sequenceConnections = configActive.map {
        Json.decodeFromString<List<List<SequenceCode>>>(remoteConfig["sequenceConnections"].asString()).map { Pair(it[0], it[1]) }
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    private val dividedSequencesWithMultipleBuses = configActive.map {
        Json.decodeFromString<List<SequenceCode>>(remoteConfig["dividedSequencesWithMultipleBuses"].asString())
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    private val _date = MutableStateFlow(SystemClock.todayHere())
    val date = _date.asStateFlow()

    private val _onlineMode = MutableStateFlow(Settings().autoOnline)
    val isOnlineModeEnabled = _onlineMode.asStateFlow()

    val settings = preferenceDataSource.settings

    val showLowFloor = preferenceDataSource.lowFloor

    val showDeparturesOnly = preferenceDataSource.departures

    val favourites = preferenceDataSource.favourites

    val version = preferenceDataSource.version

    internal val makeText = { text: String ->
        Toast.makeText(ctx, text, Toast.LENGTH_LONG)
    }

    val hasCard = preferenceDataSource.hasCard

    val cardFile = File(ctx.filesDir, "prukazka.jpg")

    init {
        scope.launch {
            preferenceDataSource.settings.collect { nastaveni ->
                _onlineMode.value = nastaveni.autoOnline
            }
        }
    }

    private val _tables = mutableMapOf<LongLine, MutableMap<LocalDate, Table?>>()

    private suspend fun _nowUsedTable(date: LocalDate, lineNumber: LongLine): Line? {
        val allTables = localDataSource.lineTables(lineNumber)

        val tablesByDate = allTables.filter {
            it.validity.validFrom <= date && date <= it.validity.validTo
        }

        if (tablesByDate.isEmpty()) return null
        if (tablesByDate.size == 1) return tablesByDate.first()

        val sortedTablesByDate = tablesByDate.sortedByDescending { it.validity.validFrom }

        val tablesByDateAndRestriction =
            if (sortedTablesByDate.none { it.hasRestriction })
                sortedTablesByDate
            else
                sortedTablesByDate.filter { it.hasRestriction }

        return tablesByDateAndRestriction.first()
    }

    private suspend fun nowUsedTable(date: LocalDate, lineNumber: LongLine) = _tables.getOrPut(lineNumber) { mutableMapOf() }.getOrPut(date) {
        _nowUsedTable(date, lineNumber)?.tab
    }

    private val _groups = mutableMapOf<SequenceCode, MutableMap<LocalDate, SequenceGroup?>>()

    private suspend fun _nowUsedGroup(date: LocalDate, seq: SequenceCode): SeqGroup? {
        val allGroups = localDataSource.seqGroups(seq)

        val groupsByDate = allGroups.filter {
            it.validity.validFrom <= date && date <= it.validity.validTo
        }

        if (groupsByDate.isEmpty()) return null
        if (groupsByDate.size == 1) return groupsByDate.first()

        val sortedGroupsByDate = groupsByDate.sortedByDescending { it.validity.validFrom }

        return sortedGroupsByDate.first()
    }

    private suspend fun nowUsedGroup(date: LocalDate, seq: SequenceCode) = _groups.getOrPut(seq) { mutableMapOf() }.getOrPut(date) {
        _nowUsedGroup(date, seq)?.group
    }

    private val sequencesMap = mutableMapOf<LocalDate, Set<TimeOfSequence>>()

    private suspend fun nowRunningSequencesOrNotInternal(date: LocalDate): Set<TimeOfSequence> {
        return localDataSource.fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
            date = date,
            groups = allGroups(date),
            tabs = allTables(date),
        )
            .filter { (_, conns) ->
                if (conns.isEmpty()) return@filter false

                conns.any { (_, codes) ->
                    runsAt(
                        timeCodes = codes.map { RunsFromTo(it.type, it.from..it.to) },
                        fixedCodes = codes.first().fixedCodes,
                        date = date,
                    )
                }
            }
            .keys
    }

    private suspend fun nowRunningSequencesOrNot(datum: LocalDate) = sequencesMap.getOrPut(datum) {
        nowRunningSequencesOrNotInternal(datum)
    }

    private suspend fun allTables(date: LocalDate) =
        localDataSource.allLineNumbers().mapNotNull { lineNumber ->
            nowUsedTable(date, lineNumber)
        }

    private suspend fun allGroups(date: LocalDate) =
        localDataSource.allSequences().mapNotNull { seq ->
            nowUsedGroup(date, seq)
        } + SequenceGroup.invalid

    suspend fun stopNames(datum: LocalDate) = localDataSource.stopNames(allTables(datum))
    suspend fun lineNumbers(datum: LocalDate) = localDataSource.lineNumbers(allTables(datum))
    suspend fun lineNumbersToday() = lineNumbers(SystemClock.todayHere())

    suspend fun busDetail(busName: BusName, date: LocalDate) =
        localDataSource.coreBus(busName, allGroups(date), nowUsedTable(date, busName.line())!!).run {
            val noCodes = distinctBy {
                it.copy(fixedCodes = "", type = DoesNotRun, from = SystemClock.todayHere(), to = SystemClock.todayHere())
            }
            val timeCodes = map {
                RunsFromTo(
                    type = it.type,
                    `in` = it.from..it.to,
                )
            }.distinctBy {
                it.type to it.`in`.toString()
            }
            val sequence = first().sequence.takeUnless { it.isInvalid() }

            val before = sequence?.let { seq ->
                buildList {
                    if (seq.modifiers().part() == 2 && seq.generic() !in dividedSequencesWithMultipleBuses.first()) add(seq.changePart(1))
                    addAll(sequenceConnections.first().filter { (_, s2) -> s2 == seq }.map { (s1, _) -> s1 })
                }
            }

            val after = sequence?.let { seq ->
                buildList {
                    if (seq.modifiers().part() == 1 && seq.generic() !in dividedSequencesWithMultipleBuses.first()) add(seq.changePart(2))
                    addAll(sequenceConnections.first().filter { (s1, _) -> s1 == seq }.map { (_, s2) -> s2 })
                }
            }

            BusDetail(
                info = first().let {
                    BusInfo(
                        lowFloor = it.lowFloor,
                        line = it.line.toShortLine(),
                        connName = it.connName,
                        sequence = sequence,
                    )
                },
                stops = noCodes.mapIndexed { i, it ->
                    BusStop(
                        time = it.time,
                        name = it.name,
                        line = it.line.toShortLine(),
                        nextStop = noCodes.getOrNull(i + 1)?.name,
                        connName = it.connName,
                        type = StopType(it.connStopFixedCodes),
                    )
                }.distinct(),
                timeCodes = timeCodes,
                fixedCodes = first().fixedCodes,
                sequence = sequence?.let { sequenceBuses(it, date) },
                before = before,
                after = after,
            )
        }

    suspend fun codes(connName: BusName, date: LocalDate) =
        localDataSource.codes(connName, nowUsedTable(date, connName.line())!!).run {
            if (isEmpty()) return@run emptyList<RunsFromTo>() to ""
            map { RunsFromTo(type = it.type, `in` = it.from..it.to) } to first().fixedCodes
        }

    suspend fun favouriteBus(busName: BusName, date: LocalDate) =
        localDataSource.coreBus(busName, allGroups(date), nowUsedTable(date, busName.line())!!)
            .run {
                Pair(
                    first().let { Favourite(it.lowFloor, it.line.toShortLine(), it.connName) },
                    map { StopOfFavourite(it.time, it.name, it.connName) }.distinct(),
                )
            }

    suspend fun ShortLine.findLongLine() = localDataSource.findLongLine(this)

    suspend fun stopNamesOfLine(line: ShortLine, date: LocalDate) =
        localDataSource.stopNamesOfLine(line.findLongLine(), nowUsedTable(date, line.findLongLine())!!)

    suspend fun nextStopNames(line: ShortLine, thisStop: String, date: LocalDate) =
        localDataSource.nextStops(line.findLongLine(), thisStop, nowUsedTable(date, line.findLongLine())!!)

    suspend fun timetable(line: ShortLine, thisStop: String, nextStop: String, date: LocalDate) =
        localDataSource.connStopsOnLineWithNextStopAtDate(
            stop = thisStop,
            nextStop = nextStop,
            date = date,
            tab = nowUsedTable(date, line.findLongLine())!!
        )
            .groupBy {
                BusInTimetable(
                    departure = it.departure,
                    lowFloor = it.lowFloor,
                    busName = it.connName,
                    destination = it.destination,
                )
            }
            .filter { (_, timeCodes) ->
                runsAt(
                    timeCodes = timeCodes.map { RunsFromTo(it.type, it.from..it.to) },
                    fixedCodes = timeCodes.first().fixedCodes,
                    date = date,
                )
            }
            .keys

    suspend fun findSequences(seq: String) = seq.partMayBeMissing()?.let { s ->
        localDataSource.findSequences(
            sequence1 = s,
            sequence2 = "$s-1",
            sequence3 = "$s-2",
            sequence4 = "$s-_",
            sequence5 = "$s-_1",
            sequence6 = "$s-_2",
        ).sortedWith(getSequenceComparator())
            .remove {
                it.isInvalid()
            }
            .map {
                it to it.seqName()
            }
    } ?: emptyList()

    val nowRunningOrNot = channelFlow {
        coroutineScope {
            while (currentCoroutineContext().isActive) {
                launch {
                    send(
                        localDataSource.lastStopTimesOfConnsInSequences(
                            todayRunningSequences = nowRunningSequencesOrNot(SystemClock.todayHere())
                                .filter {
                                    it.start - 30.minutes < SystemClock.timeHere() && SystemClock.timeHere() < it.end
                                }
                                .map { it.sequence },
                            groups = allGroups(SystemClock.todayHere()),
                            tabs = allTables(SystemClock.todayHere()),
                        )
                            .mapValues { (_, a) ->
                                a.toList().sortedBy { it.second }.find {
                                    SystemClock.timeHere() <= it.second
                                }?.first ?: error("This should never happen")
                            }
                            .toList()
                            .sortedWith(Comparator.comparing({ it.first }, getSequenceComparator()))
                    )
                }
                delay(30.seconds)
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            replay = 1
        )

    suspend fun sequence(seq: SequenceCode, date: LocalDate): Sequence? {
        val conns = localDataSource.coreBusOfSequence(seq, nowUsedGroup(date, seq))
            .groupBy {
                it.connName to it.tab
            }
            .filter { (a, _) ->
                val (connName, tab) = a
                val nowUsedTable = nowUsedTable(date, connName.line())
                nowUsedTable == tab
            }
            .map { (_, stops) ->
                val noCodes = stops.distinctBy {
                    it.copy(fixedCodes = "", type = DoesNotRun, from = SystemClock.todayHere(), to = SystemClock.todayHere())
                }
                val timeCodes = stops.map {
                    RunsFromTo(
                        type = it.type,
                        `in` = it.from..it.to
                    )
                }.distinctBy {
                    it.type to it.`in`.toString()
                }
                InterBusOfSequence(
                    stops.first().let {
                        BusInfo(
                            lowFloor = it.lowFloor,
                            line = it.line.toShortLine(),
                            connName = it.connName,
                            sequence = it.sequence,
                        )
                    },
                    noCodes.mapIndexed { i, it ->
                        BusStop(
                            time = it.time,
                            name = it.name,
                            line = it.line.toShortLine(),
                            nextStop = noCodes.getOrNull(i + 1)?.name,
                            connName = it.connName,
                            type = StopType(it.connStopFixedCodes),
                        )
                    }.distinct(),
                    timeCodes,
                    stops.first().fixedCodes,
                    Validity(stops.first().validFrom, stops.first().validTo),
                )
            }
            .sortedBy {
                it.stops.first().time
            }

        if (conns.isEmpty()) return null

        val commonTimeCodes = conns.first().timeCodes.filter { code ->
            conns.all {
                code in it.timeCodes
            }
        }

        val commonFixedCodes = conns.first().fixedCodes.split(" ").filter { code ->
            conns.all {
                code in it.fixedCodes.split(" ")
            }
        }

        val commonValidity = conns.first().validity.takeIf {
            conns.distinctBy { it.validity }.size == 1
        }

        val before = buildList {
            if (seq.modifiers().part() == 2 && seq.generic() !in dividedSequencesWithMultipleBuses.first()) add(seq.changePart(1))
            addAll(sequenceConnections.first().filter { (_, s2) -> s2 == seq }.map { (s1, _) -> s1 })
        }

        val after = buildList {
            if (seq.modifiers().part() == 1 && seq.generic() !in dividedSequencesWithMultipleBuses.first()) add(seq.changePart(2))
            addAll(sequenceConnections.first().filter { (s1, _) -> s1 == seq }.map { (_, s2) -> s2 })
        }

        return Sequence(
            name = conns.first().info.sequence!!,
            before = before,
            after = after,
            buses = conns.map {
                BusOfSequence(
                    info = it.info,
                    stops = it.stops,
                    uniqueTimeCodes = it.timeCodes.filter { code ->
                        code !in commonTimeCodes
                    },
                    uniqueFixedCodes = it.fixedCodes.split(" ").filter { code ->
                        code !in commonFixedCodes
                    }.joinToString(" "),
                    uniqueValidity = it.validity.takeIf { commonValidity == null },
                )
            },
            commonTimeCodes = commonTimeCodes,
            commonFixedCodes = commonFixedCodes.joinToString(" "),
            commonValidity = commonValidity,
        )
    }

    private suspend fun sequenceBuses(seq: SequenceCode, date: LocalDate) = localDataSource.connsOfSeq(seq, nowUsedGroup(date, seq), allTables(date))
    suspend fun firstBusOfSequence(seq: SequenceCode, date: LocalDate) = localDataSource.firstConnOfSeq(seq, nowUsedGroup(date, seq), allTables(date))
    suspend fun lastBusOfSequence(seq: SequenceCode, date: LocalDate) = localDataSource.lastConnOfSeq(seq, nowUsedGroup(date, seq), allTables(date))


    suspend fun write(
        connStops: Array<ConnStop>,
        stops: Array<Stop>,
        timeCodes: Array<TimeCode>,
        lines: Array<Line>,
        conns: Array<Conn>,
        seqGroups: Array<SeqGroup>,
        seqOfConns: Array<SeqOfConn>,
        version: Int,
    ) {
        preferenceDataSource.changeVersion(version)

        localDataSource.insertConnStops(*connStops)
        localDataSource.insertStops(*stops)
        localDataSource.insertTimeCodes(*timeCodes)
        localDataSource.insertLines(*lines)
        localDataSource.insertConns(*conns)
        localDataSource.insertSeqGroups(*seqGroups)
        localDataSource.insertSeqOfConns(*seqOfConns)
    }

    suspend fun connStops() = localDataSource.connStops()
    suspend fun stops() = localDataSource.stops()
    suspend fun timeCodes() = localDataSource.timeCodes()
    suspend fun lines() = localDataSource.lines()
    suspend fun conns() = localDataSource.conns()
    suspend fun seqGroups() = localDataSource.seqGroups()
    suspend fun seqOfConns() = localDataSource.seqOfConns()

    fun changeDate(date: LocalDate, notify: Boolean = true) {
        _date.update { date }
        if (notify) makeText("Datum změněno na ${date.toCzechAccusative()}").show()
    }

    fun editOnlineMode(mode: Boolean) {
        _onlineMode.update { mode }
    }

    suspend fun editSettings(update: (Settings) -> Settings) {
        preferenceDataSource.changeSettings(update)
    }

    suspend fun changeLowFloor(value: Boolean) {
        preferenceDataSource.changeLowFloor(value)
    }

    suspend fun changeDepartures(value: Boolean) {
        preferenceDataSource.changeDepartures(value)
    }

    suspend fun changeCard(value: Boolean) {
        preferenceDataSource.changeCard(value)
    }

    suspend fun changeFavourite(part: PartOfConn) {
        preferenceDataSource.changeFavourites { favourites ->
            listOf(part).plus(favourites).distinctBy { it.busName }
        }
    }

    suspend fun removeFavourite(name: BusName) {
        preferenceDataSource.changeFavourites { favourites ->
            favourites - favourites.first { it.busName == name }
        }
    }

    suspend fun departures(date: LocalDate, stop: String): List<Departure> =
        localDataSource.departures(stop, allTables(date))
            .groupBy { it.line / it.connNumber to it.stopIndexOnLine }
            .map { Triple(it.key.first, it.key.second, it.value) }
            .filter { (_, _, list) ->
                val timeCodes = list.map { RunsFromTo(it.type, it.from..it.to) }.distinctBy { it.type to it.`in`.toString() }
                runsAt(timeCodes, list.first().fixedCodes, date)
            }
            .map { Triple(it.first, it.second, it.third.first()) }
            .let { list ->
                val connStops = localDataSource.connStops(list.map { it.first }, allTables(date))
                list.map { Quadruple(it.first, it.second, it.third, connStops[it.first]!!) }
            }
            .map { (connName, stopIndexOnLine, info, stops) ->
                Departure(
                    name = info.name,
                    time = info.time,
                    stopIndexOnLine = stopIndexOnLine,
                    busName = connName,
                    line = info.line.toShortLine(),
                    lowFloor = info.lowFloor,
                    busStops = stops,
                    stopType = StopType(info.connStopFixedCodes),
                )
            }

    suspend fun oneWayLines() = localDataSource.oneDirectionLines().map(LongLine::toShortLine)

    fun findMiddleStop(stops: List<StopOfNowRunning>): MiddleStop? {
        fun StopOfNowRunning.indexOfDuplicate() = stops.filter { it.name == name }.takeUnless { it.size == 1 }?.indexOf(this)

        val lastCommonStop = stops.indexOfLast {
            it.indexOfDuplicate() == 0
        }

        val firstReCommonStop = stops.indexOfFirst {
            it.indexOfDuplicate() == 1
        }

        if (firstReCommonStop == -1 || lastCommonStop == -1) return null

        val last = stops[(lastCommonStop + firstReCommonStop).div(2F).roundToInt()]
        return MiddleStop(
            last.name,
            last.time,
            stops.indexOf(last)
        )
    }

    @JvmName("findMiddleStop2")
    fun findMiddleStop(stops: List<StopOfDeparture>): MiddleStop? {
        fun StopOfDeparture.indexOfDuplicate() = stops.filter { it.name == name }.takeUnless { it.size == 1 }?.indexOf(this)

        val lastCommonStop = stops.indexOfLast {
            it.indexOfDuplicate() == 0
        }

        val firstReCommonStop = stops.indexOfFirst {
            it.indexOfDuplicate() == 1
        }

        if (lastCommonStop == -1 || firstReCommonStop == -1) return null

        val last = stops[(lastCommonStop + firstReCommonStop).div(2F).roundToInt()]
        return MiddleStop(
            last.name,
            last.time,
            stops.indexOf(last)
        )
    }

    suspend fun nowRunningBus(busName: BusName, date: LocalDate): NowRunning? =
        localDataSource.connWithItsStops(busName, allGroups(date), nowUsedTable(date, busName.line())!!)
            .entries.singleOrNull()
            .also {
                if (it == null)
                    Firebase.crashlytics.recordException(IllegalStateException("No running bus found for $busName"))
            }
            ?.let { (conn, stops) ->
                NowRunning(
                    busName = conn.connName,
                    lineNumber = conn.line.toShortLine(),
                    direction = conn.direction,
                    sequence = conn.sequence,
                    stops = stops,
                )
            }

    val isOnline = flow {
        while (currentCoroutineContext().isActive) {
            emit(ctx.isOnline)
            delay(5000)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ctx.isOnline)

    val hasAccessToMap = isOnline.combine(isOnlineModeEnabled) { isOnline, onlineMode ->
        isOnline && onlineMode
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ctx.isOnline && settings.value.autoOnline)

    suspend fun hasRestriction(busName: BusName, date: LocalDate) =
        localDataSource.hasRestriction(nowUsedTable(date, busName.line())!!)

    suspend fun lineValidity(busName: BusName, date: LocalDate) =
        localDataSource.validity(nowUsedTable(date, busName.line())!!)

    suspend fun doesBusExist(busName: BusName): Boolean {
        return localDataSource.doesConnExist(busName) != null
    }

    fun doesConnRunAt(connName: BusName): suspend (LocalDate) -> Boolean = runsAt@{ datum ->
        val tab = nowUsedTable(datum, connName.line()) ?: return@runsAt false

        val list = localDataSource.codes(connName, tab).map { RunsFromTo(it.type, it.from..it.to) to it.fixedCodes }

        if (list.isEmpty()) false
        else runsAt(
            timeCodes = list.map { it.first },
            fixedCodes = list.first().second,
            date = datum,
        )
    }

    fun runsAt(
        timeCodes: List<RunsFromTo>,
        fixedCodes: String,
        date: LocalDate,
    ): Boolean = timeCodes.removeNoCodes().let { filteredCodes ->
        when {
            filteredCodes anyAre RunsOnly -> filteredCodes filter RunsOnly anySatisfies date
            filteredCodes filter RunsAlso anySatisfies date -> true
            !date.runsToday(fixedCodes) -> false
            filteredCodes filter DoesNotRun anySatisfies date -> false
            filteredCodes noneAre Runs -> true
            filteredCodes filter Runs anySatisfies date -> true
            else -> false
        }
    }

    private val contentResolver = ctx.contentResolver

    fun copyFile(oldUri: Uri, newFile: File) {
        contentResolver.openInputStream(oldUri)!!.use { input ->
            newFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun SequenceCode.seqName() = let {
        val m = modifiers()
        val (typeNominative, typeGenitive) = m.type()?.let { type ->
            type.nominative to type.genitive
        } ?: ("" to "")
        buildString {
            if (m.hasPart()) +"${m.part()}. část "
            if (m.hasPart() && m.hasType()) +"$typeGenitive "
            if (!m.hasPart() && m.hasType()) +"$typeNominative "
            +generic().value
        }
    }

    suspend fun SequenceCode.seqConnection() = "Potenciální návaznost na " + let {
        val m = modifiers()
        val (validityAccusative, typeGenitive) = m.type()?.let { type ->
            type.accusative to type.genitive
        } ?: ("" to "")
        buildString {
            if (m.hasPart()) +"${m.part()}. část "
            if (m.hasPart() && m.hasType()) +"$typeGenitive "
            if (!m.hasPart() && m.hasType()) +"$validityAccusative "
            +generic().value
        }
    }

    suspend fun getSequenceComparator(): Comparator<SequenceCode> {
        val sequenceTypes = sequenceTypes.first()

        return compareBy<SequenceCode> {
            0
        }.thenBy {
            it.modifiers().typeChar()?.let { type ->
                sequenceTypes[type]?.order
            } ?: 0
        }.thenBy {
            it.line().toIntOrNull() ?: 20
        }.thenBy {
            it.sequenceNumber()
        }.thenBy {
            it.modifiers().part()
        }
    }

//    suspend fun seqOfBus(busName: BusName, date: LocalDate) =
//        localDataSource.seqOfConn(busName, nowUsedTable(date, busName.line())!!, allGroups(date))

    fun reset() {
        sequencesMap.clear()
        _tables.clear()
    }
}

private infix fun List<RunsFromTo>.anyAre(type: TimeCodeType) = any { it.type == type }
private infix fun List<RunsFromTo>.noneAre(type: TimeCodeType) = none { it.type == type }
private infix fun List<RunsFromTo>.filter(type: TimeCodeType) = filter { it.type == type }

private infix fun List<RunsFromTo>.anySatisfies(date: LocalDate) = any { it satisfies date }
private infix fun RunsFromTo.satisfies(date: LocalDate) = date in `in`

private fun String.partMayBeMissing() = when {
    matches("^[0-9]{1,2}/[0-9A-Z]{1,2}(-[A-Z]?[12]?)?$".toRegex()) -> substringBefore('-')
    matches("^/[0-9A-Z]{1,2}(-[A-Z]?[12]?)?$".toRegex()) -> "%" + substringBefore('-')
    matches("^[0-9A-Z]{1,2}$".toRegex()) -> "%/$this"
    else -> null
}

private fun LocalDate.runsToday(fixedCodes: String) = fixedCodes
    .split(" ")
    .mapNotNull {
        when (it) {
            "X" -> dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY && !isPublicHoliday(this) // jede v pracovních dnech
            "+" -> dayOfWeek == DayOfWeek.SUNDAY || isPublicHoliday(this) // jede v neděli a ve státem uznané svátky
            "1" -> dayOfWeek == DayOfWeek.MONDAY // jede v pondělí
            "2" -> dayOfWeek == DayOfWeek.TUESDAY // jede v úterý
            "3" -> dayOfWeek == DayOfWeek.WEDNESDAY // jede ve středu
            "4" -> dayOfWeek == DayOfWeek.THURSDAY // jede ve čtvrtek
            "5" -> dayOfWeek == DayOfWeek.FRIDAY // jede v pátek
            "6" -> dayOfWeek == DayOfWeek.SATURDAY // jede v sobotu
            "7" -> dayOfWeek == DayOfWeek.SUNDAY // jede v neděli
            else -> null
        }
    }
    .ifEmpty { listOf(true) }
    .anyTrue()

// Je státní svátek nebo den pracovního klidu
private fun isPublicHoliday(datum: LocalDate) = listOf(
    LocalDate(1, 1, 1), // Den obnovy samostatného českého státu
    LocalDate(1, 1, 1), // Nový rok
    LocalDate(1, 5, 1), // Svátek práce
    LocalDate(1, 5, 8), // Den vítězství
    LocalDate(1, 7, 5), // Den slovanských věrozvěstů Cyrila a Metoděje,
    LocalDate(1, 7, 6), // Den upálení mistra Jana Husa
    LocalDate(1, 9, 28), // Den české státnosti
    LocalDate(1, 10, 28), // Den vzniku samostatného československého státu
    LocalDate(1, 11, 17), // Den boje za svobodu a demokracii
    LocalDate(1, 12, 24), // Štědrý den
    LocalDate(1, 12, 25), // 1. svátek vánoční
    LocalDate(1, 12, 26), // 2. svátek vánoční
).any {
    it.dayOfMonth == datum.dayOfMonth && it.month == datum.month
} || isEaster(datum)

// Je Velký pátek nebo Velikonoční pondělí
private fun isEaster(date: LocalDate): Boolean {
    val (bigFriday, easterMonday) = positionOfEasterInYear(date.year) ?: return false

    return date == easterMonday || date == bigFriday
}

// Poloha Velkého pátku a Velikonočního pondělí v roce
// Zdroj: https://cs.wikipedia.org/wiki/V%C3%BDpo%C4%8Det_data_Velikonoc#Algoritmus_k_v%C3%BDpo%C4%8Dtu_data
fun positionOfEasterInYear(year: Int): Pair<LocalDate, LocalDate>? {
    val (m, n) = listOf(
        1583..1599 to (22 to 2),
        1600..1699 to (22 to 2),
        1700..1799 to (23 to 3),
        1800..1899 to (23 to 4),
        1900..1999 to (24 to 5),
        2000..2099 to (24 to 5),
        2100..2199 to (24 to 6),
        2200..2299 to (25 to 0),
    ).find { (years, _) ->
        year in years
    }?.second ?: return null

    val a = year % 19
    val b = year % 4
    val c = year % 7
    val d = (19 * a + m) % 30
    val e = (n + 2 * b + 4 * c + 6 * d) % 7

    val isException = (d == 29 && e == 6) || (d == 28 && e == 6 && a > 10)

    val eaterSundayFromTheStartOfMarch = (d + e).days + if (isException) 15.days else 22.days

    val bigFridayFromTheStartOfMarch = eaterSundayFromTheStartOfMarch - 2.days
    val bigFriday = LocalDate(year, Month.MARCH, 1) + (bigFridayFromTheStartOfMarch - 1.days)

    val easterMondayFromTheStartOfMarch = eaterSundayFromTheStartOfMarch + 1.days
    val easterMonday = LocalDate(year, Month.MARCH, 1) + (easterMondayFromTheStartOfMarch - 1.days)

    return bigFriday to easterMonday
}

fun filterFixedCodesAndMakeReadable(fixedCodes: String, timeCodes: List<RunsFromTo>) = fixedCodes
    .split(" ")
    .mapNotNull {
        when (it) {
            "X" -> "Jede v pracovních dnech"
            "+" -> "Jede v neděli a ve státem uznané svátky"
            "1" -> "Jede v pondělí"
            "2" -> "Jede v úterý"
            "3" -> "Jede ve středu"
            "4" -> "Jede ve čtvrtek"
            "5" -> "Jede v pátek"
            "6" -> "Jede v sobotu"
            "7" -> "Jede v neděli"
            "24" -> "Spoj s částečně bezbariérově přístupným vozidlem, nutná dopomoc průvodce"
            else -> null
        }
    }
    .takeUnless { timeCodes.any { it.type == RunsOnly } }
    .orEmpty()

fun filterTimeCodesAndMakeReadable(timeCodes: List<RunsFromTo>) = timeCodes.removeNoCodes()
    .groupBy({
        it.type
    }, {
        if (it.`in`.start != it.`in`.endInclusive) "od ${it.`in`.start.asString()} do ${it.`in`.endInclusive.asString()}" else it.`in`.start.asString()
    })
    .let {
        if (it.containsKey(RunsOnly)) mapOf(RunsOnly to it[RunsOnly]!!) else it
    }
    .map { (type, dates) ->
        when (type) {
            Runs -> "Jede "
            RunsAlso -> "Jede také "
            RunsOnly -> "Jede pouze "
            DoesNotRun -> "Nejede "
        } + dates.joinToString()
    }

fun validityString(validity: Validity) = "JŘ linky platí od ${validity.validFrom.asString()} do ${validity.validTo.asString()}"

private fun List<RunsFromTo>.removeNoCodes() = remove(::isNoCode)

private fun isNoCode(it: RunsFromTo) = it.`in`.start == noCode && it.`in`.endInclusive == noCode

fun <K, V> Collection<Map.Entry<K, V>>.toMap(): Map<K, V> {
    return toMap(LinkedHashMap(size))
}

fun <K, V, M : MutableMap<in K, in V>> Iterable<Map.Entry<K, V>>.toMap(destination: M): M {
    for (element in this) {
        destination.put(element.key, element.value)
    }
    return destination
}