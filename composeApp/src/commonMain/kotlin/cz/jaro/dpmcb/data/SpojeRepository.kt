package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.database.SpojeDataSource
import cz.jaro.dpmcb.data.database.SpojeQueries
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.generic
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.isInvalid
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.modifiers
import cz.jaro.dpmcb.data.entities.part
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.DoesNotRun
import cz.jaro.dpmcb.data.entities.withPart
import cz.jaro.dpmcb.data.generated.Validity
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.asRepeatingFlow
import cz.jaro.dpmcb.data.helperclasses.countMembers
import cz.jaro.dpmcb.data.helperclasses.findMiddleStop
import cz.jaro.dpmcb.data.helperclasses.maxDatabaseInsertBatchSize
import cz.jaro.dpmcb.data.helperclasses.middleDestination
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.nowHere
import cz.jaro.dpmcb.data.helperclasses.runsAt
import cz.jaro.dpmcb.data.helperclasses.sorted
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.toMap
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.withCache
import cz.jaro.dpmcb.data.realtions.BusInfo
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.RunsFromTo
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.bus.BusDetail
import cz.jaro.dpmcb.data.realtions.canGetOff
import cz.jaro.dpmcb.data.realtions.canGetOn
import cz.jaro.dpmcb.data.realtions.connection.GraphBus
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
import cz.jaro.dpmcb.data.realtions.departures.Departure
import cz.jaro.dpmcb.data.realtions.invoke
import cz.jaro.dpmcb.data.realtions.now_running.NowRunning
import cz.jaro.dpmcb.data.realtions.sequence.BusOfSequence
import cz.jaro.dpmcb.data.realtions.sequence.InterBusOfSequence
import cz.jaro.dpmcb.data.realtions.sequence.Sequence
import cz.jaro.dpmcb.data.realtions.timetable.BusInTimetable
import cz.jaro.dpmcb.data.tuples.Quadruple
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atDate
import kotlin.collections.filterNot
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.collections.filterNot as remove

@OptIn(ExperimentalTime::class)
class SpojeRepository(
    onlineManager: UserOnlineManager,
    private val spojeDataSource: SpojeDataSource,
    ls: LocalSettingsDataSource,
    gs: GlobalSettingsDataSource,
    lg: Logger,
) : UserOnlineManager by onlineManager, GlobalSettingsDataSource by gs, LocalSettingsDataSource by ls, Logger by lg {
    private val scope = CoroutineScope(Dispatchers.IO)

    val loaded = scope.async {
        spojeDataSource.createTables()
    }

    suspend fun q(): SpojeQueries {
        loaded.await()
        return spojeDataSource.q
    }

    // --- TODAY STUFF ---

    val allTables = scope.async { q().lines() }

    val nowUsedTable = withCache(scope) { date: LocalDate, lineNumber: LongLine ->
        val tablesByDate = allTables.await().filter { it.number == lineNumber }.filter {
            it.validFrom <= date && date <= it.validTo
        }

        if (tablesByDate.isEmpty()) return@withCache null
        if (tablesByDate.size == 1) return@withCache tablesByDate.first().tab

        val tablesByDateAndRestriction =
            if (tablesByDate.none { it.hasRestriction }) tablesByDate
            else tablesByDate.filter { it.hasRestriction }

        tablesByDateAndRestriction.maxBy { it.validFrom }.tab
    }

    private val groupsOfSequence = scope.async { q().seqGroupsPerSequence() }

    val nowUsedGroup = withCache(scope) { date: LocalDate, seq: SequenceCode ->
        val groupsByDate = groupsOfSequence.await().getValue(seq).filter {
            it.validFrom <= date && date <= it.validTo
        }

        if (groupsByDate.isEmpty()) return@withCache SequenceGroup.invalid
        if (groupsByDate.size == 1) return@withCache groupsByDate.first().seqGroup

        val sortedGroupsByDate = groupsByDate.sortedByDescending { it.validFrom }

        sortedGroupsByDate.first().seqGroup
    }

    private val tablesOfDay = withCache(scope) { date: LocalDate ->
        q().allLineNumbers().work().mapNotNull { lineNumber ->
            nowUsedTable(date, lineNumber).await().work()
        }
    }

    private val groupsOfDay = withCache(scope) { date: LocalDate ->
        q().allSequences().map { seq ->
            nowUsedGroup(date, seq).await()
        }.distinct().filterNot { it.isInvalid() } + SequenceGroup.invalid
    }

    // --- ---

    suspend fun stopNames(datum: LocalDate) = q().stopNamesAndZones(tablesOfDay(datum).await())

    suspend fun lineNumbers(datum: LocalDate) = q().lineNumbers(tablesOfDay(datum).await().work())
    suspend fun lineNumbersToday() = lineNumbers(SystemClock.todayHere())

    suspend fun busDetail(busName: BusName, date: LocalDate) =
        q().coreBus(busName, groupsOfDay(date).await(), nowUsedTable(date, busName.line()).await()!!).run {
            val noCodes = distinctBy {
                it.copy(fixedCodes = "", type = DoesNotRun, validFrom = SystemClock.todayHere(), validTo = SystemClock.todayHere())
            }
            val timeCodes = map {
                RunsFromTo(
                    type = it.type,
                    `in` = it.validFrom..it.validTo,
                )
            }.distinctBy {
                it.type to it.`in`.toString()
            }
            val sequence = first().sequence.takeUnless { it.isInvalid() }

            val before = sequence?.let { seq ->
                buildList {
                    if (seq.modifiers().part() == 2 && seq.generic() !in dividedSequencesWithMultipleBuses) add(seq.withPart(1))
                    addAll(sequenceConnections.filter { (_, s2) -> s2 == seq }.map { (s1, _) -> s1 })
                }
            }

            val after = sequence?.let { seq ->
                buildList {
                    if (seq.modifiers().part() == 1 && seq.generic() !in dividedSequencesWithMultipleBuses) add(seq.withPart(2))
                    addAll(sequenceConnections.filter { (s1, _) -> s1 == seq }.map { (_, s2) -> s2 })
                }
            }

            BusDetail(
                info = first().let {
                    BusInfo(
                        lowFloor = it.lowFloor,
                        line = it.line,
                        connName = it.connName,
                        sequence = sequence,
                        vehicleType = it.vehicleType,
                    )
                },
                stops = noCodes.map {
                    BusStop(
                        time = it.time!!.atDate(date),
                        arrival = it.arrival.takeIf { a -> a != it.time }?.atDate(date),
                        name = it.name,
                        line = it.line,
                        connName = it.connName,
                        platform = it.platform,
                        type = StopType(it.connStopFixedCodes),
                        direction = it.direction,
                        fareZone = it.fareZone,
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
        q().codes(connName, nowUsedTable(date, connName.line()).await()!!).run {
            if (isEmpty()) return@run emptyList<RunsFromTo>() to ""
            map { RunsFromTo(type = it.type, `in` = it.validFrom..it.validTo) } to first().fixedCodes
        }

    suspend fun ShortLine.findLongLine() = q().findLongLine(this)

    suspend fun stopNamesOfLine(line: LongLine, date: LocalDate) =
        q().stopNamesOfLine(nowUsedTable(date, line).await()!!)

    val platformsAndDirections = withCache(scope) {
            line: LongLine,
            thisStop: StopName,
            date: LocalDate,
        ->
        q().platformsAndDirections(
            stop = thisStop,
            tab = nowUsedTable(date, line).await()!!,
        )
            .groupBy {
                it.connName
            }
            .filter { (_, codes) ->
                runsAt(
                    timeCodes = codes.map { RunsFromTo(it.type, it.validFrom..it.validTo) },
                    fixedCodes = codes.first().fixedCodes,
                    date = date,
                )
            }
            .mapValues { (_, codes) ->
                codes.distinctBy { it.stopName to it.stopIndexOnLine }
            }
            .flatMap { (_, stops) ->
                val stopNames = stops.map { it.stopName }
                stops.withIndex()
                    .filter { it.value.stopName == thisStop }
                    .filter { it.index < stops.lastIndex && StopType(it.value.stopFixedCodes).canGetOn }
                    .filter { (_, stop) ->
                        stop.platform != null
                    }
                    .map { (i, stop) ->
                        val destination = middleDestination(line, stopNames, i)
                        Quadruple(
                            destination ?: stops.last().stopName,
                            stopNames.indexOf(destination ?: stops.last().stopName),
                            stop.platform!!,
                            if (destination != null) Direction.NEGATIVE else stops.first().direction,
                        ).work<Quadruple<StopName, Int, Platform, Direction>>(stop.connName, stop.stopIndexOnLine)
                    }
            }
            .sortedBy { it.second }
            .work<List<Quadruple<StopName, Int, Platform, Direction>>>()
            .map { Triple(it.first, it.third, it.fourth) }
            .let { endStops ->
                val total = endStops.count().toFloat()
                endStops.countMembers()
                    .mapValues { (_, count) ->
                        count / total
                    }
                    .filterValues {
                        it >= .1F
                    }
                    .keys
                    .groupBy({ (_, platform, direction) -> platform to direction }, { (stop) -> stop })
                    .entries
                    .sortedBy { it.key.second }
                    .sortedBy { it.key.first }
                    .toMap()
            }
    }

    val platformsOfStop = withCache(scope) {
            thisStop: StopName,
            date: LocalDate,
        ->
        q().platformsOfStop(
            stop = thisStop,
            tabs = tablesOfDay(date).await().work(),
        ).work()
            .groupBy {
                it.connName
            }
            .filter { (_, codes) ->
                runsAt(
                    timeCodes = codes.map { RunsFromTo(it.type, it.validFrom..it.validTo) },
                    fixedCodes = codes.first().fixedCodes,
                    date = date,
                )
            }
            .flatMap { (_, codes) ->
                codes.mapNotNull { it.platform }
            }
            .distinct()
            .sorted()
    }

    suspend fun timetable(
        line: LongLine,
        thisStop: StopName,
        platform: Platform,
        direction: Direction,
        date: LocalDate,
    ): Set<BusInTimetable> {
        val isOneWay = isOneWay(line)
        return q().connStopsOnLineOnPlatformInDirection(
            stop = thisStop,
            platform = platform,
            direction = if (isOneWay) Direction.POSITIVE else direction,
            tab = nowUsedTable(date, line).await()!!
        )
            .groupBy {
                it.connName
            }
            .filter { (_, codes) ->
                runsAt(
                    timeCodes = codes.map { RunsFromTo(it.type, it.validFrom..it.validTo) },
                    fixedCodes = codes.first().fixedCodes,
                    date = date,
                )
            }
            .flatMap { (busName, codes) ->
                val stops = codes.map { Triple(it.stopName, it.time, it.platform) }.distinct()
                val middleDestination = if (isOneWay) findMiddleStop(stops.map { it.first }) else null

                val stopsOnThisPart = if (middleDestination == null) stops
                else when (direction) {
                    Direction.NEGATIVE -> stops.take(middleDestination.index - 1)
                    Direction.POSITIVE -> stops.drop(middleDestination.index - 1)
                }

                stopsOnThisPart
                    .filter { (stopName, _, stopPlatform) ->
                        stopName == thisStop && stopPlatform == platform
                    }
                    .map { (_, time) ->
                        BusInTimetable(
                            departure = time!!,
                            busName = busName,
                            destination = middleDestination?.name.takeIf { direction == Direction.NEGATIVE } ?: stops.last().first,
                        )
                    }
            }
            .toSet()
    }

    suspend fun findSequences(
        line: String, number: String,
    ): List<Pair<SequenceCode, String>> {
        val s = number.ifEmpty { "%" } + "/" + line
        return q().findSequences(
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
    }

    suspend fun sequence(seq: SequenceCode, date: LocalDate): Sequence? {
        val conns = q().coreBusOfSequence(seq, nowUsedGroup(date, seq).await())
            .groupBy {
                it.connName to it.tab
            }
            .filter { (a, _) ->
                val (connName, tab) = a
                val nowUsedTable = nowUsedTable(date, connName.line()).await()
                nowUsedTable == tab
            }
            .map { (_, stops) ->
                val noCodes = stops.distinctBy {
                    it.copy(fixedCodes = "", type = DoesNotRun, validFrom = SystemClock.todayHere(), validTo = SystemClock.todayHere())
                }
                val timeCodes = stops.map {
                    RunsFromTo(
                        type = it.type,
                        `in` = it.validFrom..it.validTo
                    )
                }.distinctBy {
                    it.type to it.`in`.toString()
                }
                InterBusOfSequence(
                    stops.first().let {
                        BusInfo(
                            lowFloor = it.lowFloor,
                            line = it.line,
                            connName = it.connName,
                            sequence = it.sequence,
                            vehicleType = it.vehicleType,
                        )
                    },
                    noCodes.map {
                        BusStop(
                            time = it.time!!.atDate(date),
                            arrival = it.arrival.takeIf { a -> a != it.time }?.atDate(date),
                            name = it.name,
                            line = it.line,
                            connName = it.connName,
                            platform = it.platform,
                            type = StopType(it.connStopFixedCodes),
                            direction = it.direction,
                            fareZone = it.fareZone,
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

        val commonLineTraction = lineTraction(conns.first().info.line, conns.first().info.vehicleType).takeIf {
            conns.distinctBy { lineTraction(it.info.line, it.info.vehicleType) }.size == 1
        }

        val before = buildList {
            if (seq.modifiers().part() == 2 && seq.generic() !in dividedSequencesWithMultipleBuses) add(seq.withPart(1))
            addAll(sequenceConnections.filter { (_, s2) -> s2 == seq }.map { (s1, _) -> s1 })
        }

        val after = buildList {
            if (seq.modifiers().part() == 1 && seq.generic() !in dividedSequencesWithMultipleBuses) add(seq.withPart(2))
            addAll(sequenceConnections.filter { (s1, _) -> s1 == seq }.map { (_, s2) -> s2 })
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
            commonLineTraction = commonLineTraction,
        )
    }

    private suspend fun sequenceBuses(seq: SequenceCode, date: LocalDate) =
        q().connsOfSeq(seq, nowUsedGroup(date, seq).await(), tablesOfDay(date).await())

    suspend fun firstBusOfSequence(seq: SequenceCode, date: LocalDate) =
        q().firstConnOfSeq(seq, nowUsedGroup(date, seq).await(), tablesOfDay(date).await())

    suspend fun lastBusOfSequence(seq: SequenceCode, date: LocalDate) =
        q().lastConnOfSeq(seq, nowUsedGroup(date, seq).await(), tablesOfDay(date).await())


    suspend fun write(
        connStops: List<cz.jaro.dpmcb.data.generated.ConnStop>,
        stops: List<cz.jaro.dpmcb.data.generated.Stop>,
        timeCodes: List<cz.jaro.dpmcb.data.generated.TimeCode>,
        lines: List<cz.jaro.dpmcb.data.generated.Line>,
        conns: List<cz.jaro.dpmcb.data.generated.Conn>,
        seqGroups: List<cz.jaro.dpmcb.data.generated.SeqGroup>,
        seqOfConns: List<cz.jaro.dpmcb.data.generated.SeqOfConn>,
        data: DownloadedData,
        progress: (Float, String, Float) -> Unit,
    ) = coroutineScope {
        measure("Saving") {
            changeLoadedData { data }

            val all = listOf(
                connStops.chunked(maxDatabaseInsertBatchSize)
                    .map { scope.async(start = CoroutineStart.LAZY) { q().insertConnStops(it) } } to "Zastávky spojů",
                stops.chunked(maxDatabaseInsertBatchSize)
                    .map { scope.async(start = CoroutineStart.LAZY) { q().insertStops(it) } } to "Zastávky linek",
                timeCodes.chunked(maxDatabaseInsertBatchSize)
                    .map { scope.async(start = CoroutineStart.LAZY) { q().insertTimeCodes(it) } } to "Časové kódy",
                lines.chunked(maxDatabaseInsertBatchSize)
                    .map { scope.async(start = CoroutineStart.LAZY) { q().insertLines(it) } } to "Linky",
                conns.chunked(maxDatabaseInsertBatchSize)
                    .map { scope.async(start = CoroutineStart.LAZY) { q().insertConns(it) } } to "Spoje",
                seqGroups.chunked(maxDatabaseInsertBatchSize)
                    .map { scope.async(start = CoroutineStart.LAZY) { q().insertSeqGroups(it) } } to "Skupiny kurzů",
                seqOfConns.chunked(maxDatabaseInsertBatchSize)
                    .map { scope.async(start = CoroutineStart.LAZY) { q().insertSeqOfConns(it) } } to "Kurzy spojů",
            ).work()
            val count = all.sumOf { it.first.size } * 1F
            var done = 0
            all.forEach { (group, label) ->
                val groupCount = group.size * 1F
                var groupDone = 0
                progress(0F, label, 0F)
                group.forEach { deferred ->
                    deferred.await()
                    progress((++done / count).work(), label, (++groupDone / groupCount).work())
                }
            }
            "DONE".work()
            Unit
        }
    }

    val needsToDownloadData get() = spojeDataSource.needsToDownloadData

    suspend fun connStops() = q().connStops()
    suspend fun stops() = q().stops()
    suspend fun timeCodes() = q().timeCodes()
    suspend fun lines() = q().lines()
    suspend fun conns() = q().conns()
    suspend fun seqGroups() = q().seqGroups()
    suspend fun seqOfConns() = q().seqOfConns()

    suspend fun seqOfConns(
        conns: Set<BusName>,
        date: LocalDate,
    ) = q().seqOfConns(conns, groupsOfDay(date).await(), tablesOfDay(date).await())

    suspend fun departures(date: LocalDate, stop: StopName): List<Departure> =
        q().departures(stop, tablesOfDay(date).await(), groupsOfDay(date).await())
            .groupBy { it.line / it.connNumber to it.stopIndexOnLine }
            .map { Triple(it.key.first, it.key.second, it.value) }
            .filter { (_, _, list) ->
                val timeCodes = list.map { RunsFromTo(it.type, it.validFrom..it.validTo) }.distinctBy { it.type to it.`in`.toString() }
                runsAt(timeCodes, list.first().fixedCodes, date)
            }
            .map { Triple(it.first, it.second, it.third.first()) }
            .let { list ->
                val connStops = q().connStops(list.map { it.first }, tablesOfDay(date).await())
                list.map { Quadruple(it.first, it.second, it.third, connStops[it.first]!!) }
            }
            .map { (connName, stopIndexOnLine, info, stops) ->
                Departure(
                    name = info.name,
                    time = info.time!!,
                    stopIndexOnLine = stopIndexOnLine,
                    busName = connName,
                    line = info.line,
                    vehicleType = info.vehicleType,
                    busStops = stops,
                    stopType = StopType(info.connStopFixedCodes),
                    direction = info.direction,
                    sequence = info.sequence,
                    platform = info.platform,
                    fareZone = info.fareZone,
                )
            }

    private val oneWayLines = scope.async { q().oneDirectionLines() }

    suspend fun isOneWay(line: LongLine) = line in oneWayLines.await()

    fun doesConnRunAt(connName: BusName): suspend (LocalDate) -> Boolean = runsAt@{ datum ->
        val tab = nowUsedTable(datum, connName.line()).await() ?: return@runsAt false

        val list = q().codes(connName, tab)
            .map { RunsFromTo(it.type, it.validFrom..it.validTo) to it.fixedCodes }

        if (list.isEmpty()) false
        else runsAt(
            timeCodes = list.map { it.first },
            fixedCodes = list.first().second,
            date = datum,
        )
    }

    suspend fun hasRestriction(busName: BusName, date: LocalDate) =
        q().hasRestriction(nowUsedTable(date, busName.line()).await()!!)

    suspend fun lineValidity(busName: BusName, date: LocalDate) =
        q().validity(nowUsedTable(date, busName.line()).await()!!)

    suspend fun doesBusExist(busName: BusName) =
        q().doesConnExist(busName.work(10)).work(11) != null

    suspend fun dropAllTables() {
        spojeDataSource.dropAllTables()
    }

    suspend fun createTables() {
        spojeDataSource.createTables()
    }

    val stopGraph = withCache(scope) { date: LocalDate ->
        createStopGraph(stops(date).await())
    }

    val connsRunAt = withCache(scope) { date: LocalDate ->
        val codes = q().multiCodes(
            connNames = stops(date).await().keys.map { it.connName },
            tabs = tablesOfDay(date).await(),
        )
        // future: codes: Map<BusName, Map<Table, List<CodesOfBus>>>
        codes.mapValues { (_, codes) ->
            { date: LocalDate ->
                runsAt(
                    codes.map { RunsFromTo(it.type, it.validFrom..it.validTo) },
                    codes.first().fixedCodes,
                    date
                )
            }
        }
    }

    val stops = withCache(scope) { date: LocalDate ->
        q().stopsOnConns(tablesOfDay(date).await())
    }

    init {
        val _ = stopGraph(SystemClock.todayHere())
        val _ = connsRunAt(SystemClock.todayHere())
    }

    // --- NOW RUNNING ---

    val todayRunningSequences = withCache(scope) { date: LocalDate ->
        q().codesOfSequences(
            groups = groupsOfDay(date).await(),
            tabs = tablesOfDay(date).await(),
        )
            .mapValues { (_, conns) ->
                conns.filter { (_, codes) ->
                    runsAt(
                        timeCodes = codes.map { RunsFromTo(it.type, it.validFrom..it.validTo) },
                        fixedCodes = codes.first().fixedCodes,
                        date = date,
                    )
                }.map { (busName) ->
                    busName
                }
            }
            .filter { (_, conns) ->
                conns.isNotEmpty()
            }
    }

    val todayRunningBusBoundaries =
        withCache(scope) { date: LocalDate ->
            val sequences = todayRunningSequences(date).await()
            val times = q().busesStartAndEnd(
                conns = sequences.values.flatten(),
                tabs = tablesOfDay(SystemClock.todayHere()).await(),
            )

            sequences.mapValues { (_, buses) ->
                buses.map { it to times[it]!! }
            }
        }

    private suspend fun getNowRunning(): List<BusName> {
        return todayRunningBusBoundaries(SystemClock.todayHere()).await()
            .filterValues { buses ->
                buses.first().second.start.atDate(SystemClock.todayHere()) - 30.minutes < SystemClock.nowHere()
                        && SystemClock.timeHere() < buses.last().second.end
            }
            .mapValues { (_, buses) ->
                buses.find { SystemClock.timeHere() < it.second.end }?.first ?: buses.last().first
            }
            .values.toList()
    }

    val nowRunning = ::getNowRunning
        .asRepeatingFlow(30.seconds)
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            replay = 1
        )

    suspend fun nowRunningBusDetails(busNames: List<BusName>, date: LocalDate) =
        q().nowRunningBuses(busNames, groupsOfDay(date).await(), tablesOfDay(date).await())
            .entries
            .associate { (conn, stops) ->
                conn.connName to NowRunning(
                    busName = conn.connName,
                    lineNumber = conn.line,
                    direction = conn.direction,
                    sequence = conn.sequence,
                    stops = stops,
                )
            }

    suspend fun connectionBusInfo(
        buses: Set<BusName>, date: LocalDate,
    ) = q().connectionResultBuses(
        buses, groupsOfDay(date).await(), tablesOfDay(date).await()
    )
}

fun MutableStopGraph.toStopGraph(): StopGraph = mapValues { (_, set) -> set.toList() }
fun StopGraph.toMutableStopGraph(): MutableStopGraph = mapValues { (_, set) -> set.toMutableList() }.toMutableMap()

fun emptyStopGraph(): StopGraph = emptyMap()
fun mutableStopGraphOf(): MutableStopGraph = mutableMapOf()

private fun createStopGraph(
    connStops: Map<GraphBus, List<StopNameTime>>,
): StopGraph {
    val stopGraph = mutableStopGraphOf()

    connStops.forEach { (bus, stops) ->
        val indexed = stops.withIndex().toList()
        indexed.windowed(2).forEach { (thisStop, nextStop) ->
            val start = if (StopType(thisStop.value.fixedCodes).canGetOn) thisStop
            else indexed.take(thisStop.index).findLast { StopType(thisStop.value.fixedCodes).canGetOn } ?: return@forEach
            val end = if (StopType(nextStop.value.fixedCodes).canGetOff) nextStop
            else indexed.drop(nextStop.index + 1).find { StopType(thisStop.value.fixedCodes).canGetOff } ?: return@forEach
            stopGraph.getOrPut(start.value.name) { mutableListOf() } += GraphEdge(
                departure = start.value.departure!!,
                arrival = end.value.arrival!!,
                to = end.value.name,
                bus = bus.connName,
                vehicleType = bus.vehicleType,
                departureIndexOnBus = start.index,
                arrivalIndexOnBus = end.index,
                departurePlatform = start.value.platform,
                arrivalPlatform = end.value.platform,
            )
        }
    }

    val g: StopGraph = stopGraph.mapValues { (_, e) ->
        e.sortedBy { it.departure }
    }/*.toList().sortedBy { it.first }.toMap()*/

//    println(graphZastavek
//        .flatMap { (k, v) ->
//            v.map { k to it }
//        }
//        .joinToString("\n") {
//            it.toList().joinToString("&&&")
//        }
//        .replace(" ", "")
//        .replace("&&&", " ")
//    )

    return g
}