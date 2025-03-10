package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.database.DataSource
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
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.*
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SequenceType
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.anyAre
import cz.jaro.dpmcb.data.helperclasses.anySatisfies
import cz.jaro.dpmcb.data.helperclasses.asRepeatingFlow
import cz.jaro.dpmcb.data.helperclasses.filter
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.noneAre
import cz.jaro.dpmcb.data.helperclasses.partMayBeMissing
import cz.jaro.dpmcb.data.helperclasses.removeNoCodes
import cz.jaro.dpmcb.data.helperclasses.runsToday
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.unaryPlus
import cz.jaro.dpmcb.data.helperclasses.withCache
import cz.jaro.dpmcb.data.helperclasses.work
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
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigException
import dev.gitlive.firebase.remoteconfig.get
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.collections.filterNot
import kotlin.jvm.JvmName
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.collections.filterNot as remove

class SpojeRepository(
    onlineManager: UserOnlineManager,
    private val localDataSource: DataSource,
    private val preferenceDataSource: PreferenceDataSource,
    firebase: FirebaseApp,
) : UserOnlineManager by onlineManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val remoteConfig = Firebase.remoteConfig(firebase)

    private suspend fun getConfigActive() = try {
        remoteConfig.settings {
            minimumFetchInterval = 1.hours
        }
        if (!isOnline())
            remoteConfig.activate()
        else
            remoteConfig.fetchAndActivate()
    } catch (e: FirebaseRemoteConfigException) {
        e.printStackTrace()
        recordException(e)
        try {
            remoteConfig.activate()
        } catch (_: FirebaseRemoteConfigException) {
            false
        }
    }

    private val configActive = ::getConfigActive.asFlow()

    private val sequenceTypes = configActive.map {
        Json.decodeFromString<Map<Char, SequenceType>>(remoteConfig["sequenceTypes"])
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun SequenceModifiers.type() = typeChar()?.let { sequenceTypes.first()[it] }

    private val sequenceConnections = configActive.map {
        Json.decodeFromString<List<List<SequenceCode>>>(remoteConfig["sequenceConnections"]).map { Pair(it[0], it[1]) }
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    private val dividedSequencesWithMultipleBuses = configActive.map {
        Json.decodeFromString<List<SequenceCode>>(remoteConfig["dividedSequencesWithMultipleBuses"])
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    private val _onlineMode = MutableStateFlow(Settings().autoOnline)
    val isOnlineModeEnabled = _onlineMode.asStateFlow()

    val settings = preferenceDataSource.settings

    val showLowFloor = preferenceDataSource.lowFloor

    val showDeparturesOnly = preferenceDataSource.departures

    val favourites = preferenceDataSource.favourites

    val recents: Flow<List<BusName>> = preferenceDataSource.recents

    val version = preferenceDataSource.version

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
            it.validFrom <= date && date <= it.validTo
        }

        if (tablesByDate.isEmpty()) return null
        if (tablesByDate.size == 1) return tablesByDate.first()

        val tablesByDateAndRestriction =
            if (tablesByDate.none { it.hasRestriction }) tablesByDate
            else tablesByDate.filter { it.hasRestriction }

        return tablesByDateAndRestriction.maxBy { it.validFrom }
    }

    private suspend fun nowUsedTable(date: LocalDate, lineNumber: LongLine) = _tables.getOrPut(lineNumber) { mutableMapOf() }.getOrPut(date) {
        _nowUsedTable(date, lineNumber)?.tab
    }

    private val _groups = mutableMapOf<SequenceCode, MutableMap<LocalDate, SequenceGroup?>>()

    private suspend fun _nowUsedGroup(date: LocalDate, seq: SequenceCode): SeqGroup? {
        val allGroups = localDataSource.seqGroups(seq)

        val groupsByDate = allGroups.filter {
            it.validFrom <= date && date <= it.validTo
        }

        if (groupsByDate.isEmpty()) return null
        if (groupsByDate.size == 1) return groupsByDate.first()

        val sortedGroupsByDate = groupsByDate.sortedByDescending { it.validFrom }

        return sortedGroupsByDate.first()
    }

    private suspend fun nowUsedGroup(date: LocalDate, seq: SequenceCode) = _groups.getOrPut(seq) { mutableMapOf() }.getOrPut(date) {
        _nowUsedGroup(date, seq)?.group
    } ?: SequenceGroup.invalid

    private val sequencesMap = mutableMapOf<LocalDate, Set<TimeOfSequence>>()

    private suspend fun nowRunningSequencesOrNotInternal(date: LocalDate) =
        localDataSource.fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
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

    private suspend fun nowRunningSequencesOrNot(datum: LocalDate) = sequencesMap.getOrPut(datum) {
        nowRunningSequencesOrNotInternal(datum)
    }

    private suspend fun allTables(date: LocalDate) =
        localDataSource.allLineNumbers().also(::println).also(::println).mapNotNull { lineNumber ->
            nowUsedTable(date, lineNumber)
        }

    private suspend fun allGroups(date: LocalDate) =
        localDataSource.allSequences().map { seq ->
            nowUsedGroup(date, seq)
        }.distinct().filterNot { it.isInvalid() } + SequenceGroup.invalid

    init {
        scope.launch(Dispatchers.IO) {
            allGroups(SystemClock.todayHere())
        }
        scope.launch(Dispatchers.IO) {
            allTables(SystemClock.todayHere())
        }
    }

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

    suspend fun nextStopNames(line: ShortLine, thisStop: String, date: LocalDate): List<String> =
        localDataSource.nextStops(line.findLongLine(), thisStop, nowUsedTable(date, line.findLongLine())!!)

    suspend fun timetable(line: ShortLine, thisStop: String, nextStop: String, date: LocalDate): Set<BusInTimetable> =
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

    val nowRunningOrNot = ::getNowRunningOrNot
        .asRepeatingFlow(30.seconds)
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            replay = 1
        )

    private suspend fun getNowRunningOrNot() =
        localDataSource.lastStopTimesOfConnsInSequences(
            todayRunningSequences = nowRunningSequencesOrNot(SystemClock.todayHere())
                .work()
                .filter {
                    it.start - 30.minutes < SystemClock.timeHere() && SystemClock.timeHere() < it.end
                }
                .map { it.sequence },
            groups = allGroups(SystemClock.todayHere()),
            tabs = allTables(SystemClock.todayHere()),
        )
            .values
            .map { stopByBus ->
                stopByBus
                    .toList()
                    .sortedBy { it.second }
                    .find {
                        SystemClock.timeHere() <= it.second
                    }?.first ?: error("This should never happen")
            }

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
        connStops: List<ConnStop>,
        stops: List<Stop>,
        timeCodes: List<TimeCode>,
        lines: List<Line>,
        conns: List<Conn>,
        seqGroups: List<SeqGroup>,
        seqOfConns: List<SeqOfConn>,
        version: Int,
    ) {
        preferenceDataSource.changeVersion(version)

        localDataSource.insertConnStops(*connStops.toTypedArray())
        localDataSource.insertStops(*stops.toTypedArray())
        localDataSource.insertTimeCodes(*timeCodes.toTypedArray())
        localDataSource.insertLines(*lines.toTypedArray())
        localDataSource.insertConns(*conns.toTypedArray())
        localDataSource.insertSeqGroups(*seqGroups.toTypedArray())
        localDataSource.insertSeqOfConns(*seqOfConns.toTypedArray())
    }

    suspend fun connStops() = localDataSource.connStops()
    suspend fun stops() = localDataSource.stops()
    suspend fun timeCodes() = localDataSource.timeCodes()
    suspend fun lines() = localDataSource.lines()
    suspend fun conns() = localDataSource.conns()
    suspend fun seqGroups() = localDataSource.seqGroups()
    suspend fun seqOfConns() = localDataSource.seqOfConns()

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

    suspend fun changeFavourite(part: PartOfConn) {
        preferenceDataSource.changeFavourites { favourites ->
            (listOf(part) + favourites).distinctBy { it.busName }
        }
    }

    suspend fun removeFavourite(name: BusName) {
        preferenceDataSource.changeFavourites { favourites ->
            favourites - favourites.first { it.busName == name }
        }
    }

    suspend fun pushRecentBus(bus: BusName) {
        preferenceDataSource.changeRecents { recents ->
            (listOf(bus) + recents).distinct().take(settings.value.recentBusesCount)
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

    val oneWayLines = withCache(suspend { localDataSource.oneDirectionLines().map(LongLine::toShortLine) })

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

    suspend fun nowRunningBuses(busNames: List<BusName>, date: LocalDate): Map<BusName, NowRunning> =
        localDataSource.nowRunningBuses(busNames, allGroups(date), allTables(date))
            .entries
            .associate { (conn, stops) ->
                conn.connName to NowRunning(
                    busName = conn.connName,
                    lineNumber = conn.line.toShortLine(),
                    direction = conn.direction,
                    sequence = conn.sequence,
                    stops = stops,
                )
            }

    val hasAccessToMap = isOnline.combine(isOnlineModeEnabled) { isOnline, onlineMode ->
        isOnline && onlineMode
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), isOnline() && settings.value.autoOnline)

    suspend fun hasRestriction(busName: BusName, date: LocalDate) =
        localDataSource.hasRestriction(nowUsedTable(date, busName.line())!!)

    suspend fun lineValidity(busName: BusName, date: LocalDate) =
        localDataSource.validity(nowUsedTable(date, busName.line())!!)

    suspend fun doesBusExist(busName: BusName) =
        localDataSource.doesConnExist(busName) != null

    fun doesConnRunAt(connName: BusName): suspend (LocalDate) -> Boolean = runsAt@{ datum ->
        val tab = nowUsedTable(datum, connName.line()) ?: return@runsAt false

        val list = localDataSource.codes(connName, tab)
            .map { RunsFromTo(it.type, it.from..it.to) to it.fixedCodes }

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

    suspend fun reset() {
//        localDataSource.clearAll() TODO!
        sequencesMap.clear()
        _tables.clear()
    }
}