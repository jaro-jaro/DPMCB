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
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.DoesNotRun
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.Runs
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.RunsAlso
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.RunsOnly
import cz.jaro.dpmcb.data.helperclasses.SequenceType
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.anyTrue
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.noCode
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechAccusative
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
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
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.collections.filterNot as remove

@Single
class SpojeRepository(
    ctx: Application,
    private val localDataSource: Dao,
    private val preferenceDataSource: PreferenceDataSource,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val remoteConfig = Firebase.remoteConfig

    private val configActive = flow {
        try {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            if (!ctx.isOnline)
                emit(remoteConfig.activate().await())
            else
                emit(remoteConfig.fetchAndActivate().await())
        } catch (e: FirebaseRemoteConfigException) {
            e.printStackTrace()
            Firebase.crashlytics.recordException(e)
            try {
                emit(remoteConfig.activate().await())
            } catch (e: FirebaseRemoteConfigException) {
                emit(false)
            }
        }
    }

    private val sequenceTypes = configActive.map {
        Json.decodeFromString<Map<Char, SequenceType>>(remoteConfig["sequenceTypes"].asString())
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    private val sequenceConnections = configActive.map {
        Json.decodeFromString<List<List<String>>>(remoteConfig["sequenceConnections"].asString()).map { Pair(it[0], it[1]) }
    }//.stateIn(scope, SharingStarted.Eagerly, null)

    private val _date = MutableStateFlow(LocalDate.now())
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

    private val tablesMap = mutableMapOf<Int, MutableMap<LocalDate, String?>>()

    private suspend fun nowUsedTabInternal(date: LocalDate, lineNumber: Int): Line? {
        val allTables = localDataSource.lineTables(lineNumber)

        val tablesByDate = allTables.filter {
            it.validFrom <= date && date <= it.validTo
        }

        if (tablesByDate.isEmpty()) return null
        if (tablesByDate.size == 1) return tablesByDate.first()

        val sortedTablesByDate = tablesByDate.sortedByDescending { it.validFrom }

        val tablesByDateAndRestriction =
            if (sortedTablesByDate.none { it.hasRestriction })
                sortedTablesByDate
            else
                sortedTablesByDate.filter { it.hasRestriction }

        return tablesByDateAndRestriction.first()
    }

    private suspend fun nowUsedTable(datum: LocalDate, lineNumber: Int) = tablesMap.getOrPut(lineNumber) { mutableMapOf() }.getOrPut(datum) {
        nowUsedTabInternal(datum, lineNumber)?.tab
    }

    private val sequencesMap = mutableMapOf<LocalDate, Set<TimeOfSequence>>()

    private suspend fun nowRunningSequencesOrNotInternal(date: LocalDate): Set<TimeOfSequence> {
        return localDataSource.fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
            date = date,
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

    private suspend fun LocalDate.isThisTableNowUsed(tab: String): Boolean {
        val lineNumber = tab.split("-").first().toInt()
        return nowUsedTable(this, lineNumber) == tab
    }

    private suspend fun allTables(date: LocalDate) =
        localDataSource.allLineNumbers().mapNotNull { lineNumber ->
            nowUsedTable(date, lineNumber)
        }

    suspend fun stopNames(datum: LocalDate) = localDataSource.stopNames(allTables(datum))
    suspend fun lineNumbers(datum: LocalDate) = localDataSource.lineNumbers(allTables(datum))

    suspend fun busDetail(busName: String, date: LocalDate) =
        localDataSource.connWithItsConnStopsAndCodes(busName, nowUsedTable(date, extractLineNumber(busName))!!).run {
            val noCodes = distinctBy {
                it.copy(fixedCodes = "", type = DoesNotRun, from = LocalDate.now(), to = LocalDate.now())
            }
            val timeCodes = map {
                RunsFromTo(
                    type = it.type,
                    `in` = it.from..it.to,
                )
            }.distinctBy {
                it.type to it.`in`.toString()
            }

            val before = first().sequence?.let { seq ->
                buildList {
                    if ('-' in seq && seq.endsWith('2')) add(seq.dropLast(1) + '1')
                    addAll(sequenceConnections.first().filter { (_, s2) -> s2 == seq }.map { (s1, _) -> s1 })
                }
            }

            val after = first().sequence?.let { seq ->
                buildList {
                    if ('-' in seq && seq.endsWith('1')) add(seq.dropLast(1) + '2')
                    addAll(sequenceConnections.first().filter { (s1, _) -> s1 == seq }.map { (_, s2) -> s2 })
                }
            }

            BusDetail(
                info = first().let {
                    BusInfo(
                        lowFloor = it.lowFloor,
                        line = it.line - 325_000,
                        connName = it.connName,
                        sequence = it.sequence,
                    )
                },
                stops = noCodes.mapIndexed { i, it ->
                    BusStop(
                        time = it.time,
                        name = it.name,
                        line = it.line - 325_000,
                        nextStop = noCodes.getOrNull(i + 1)?.name,
                        connName = it.connName,
                        type = StopType(it.connStopFixedCodes),
                    )
                }.distinct(),
                timeCodes = timeCodes,
                fixedCodes = first().fixedCodes,
                sequence = first().sequence?.let { sequenceBuses(it, date) },
                before = before,
                after = after,
            )
        }

    suspend fun codes(connName: String, date: LocalDate) =
        localDataSource.codes(connName, nowUsedTable(date, extractLineNumber(connName))!!).run {
            if (isEmpty()) return@run emptyList<RunsFromTo>() to ""
            map { RunsFromTo(type = it.type, `in` = it.from..it.to) } to first().fixedCodes
        }

    private fun extractLineNumber(connName: String) = connName.split("/")[0].toInt()

    suspend fun favouriteBus(busName: String, date: LocalDate) =
        localDataSource.connWithItsConnStopsAndCodes(busName, nowUsedTable(date, extractLineNumber(busName))!!)
            .run {
                Pair(
                    first().let { Favourite(it.lowFloor, it.line - 325_000, it.connName) },
                    map { StopOfFavourite(it.time, it.name, it.connName) }.distinct(),
                )
            }

    suspend fun stopNamesOfLine(line: Int, date: LocalDate) =
        localDataSource.stopNamesOfLine(line + 325_000, nowUsedTable(date, line + 325_000)!!)

    suspend fun nextStopNames(line: Int, thisStop: String, date: LocalDate) =
        localDataSource.nextStops(line + 325_000, thisStop, nowUsedTable(date, line + 325_000)!!)

    suspend fun timetable(line: Int, thisStop: String, nextStop: String, date: LocalDate) =
        localDataSource.connStopsOnLineWithNextStopAtDate(
            stop = thisStop,
            nextStop = nextStop,
            date = date,
            tab = nowUsedTable(date, line + 325_000)!!
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
            .map {
                it to seqName(it)
            }
    } ?: emptyList()

    val nowRunningOrNot = channelFlow {
        coroutineScope {
            while (currentCoroutineContext().isActive) {
                launch {
                    send(
                        localDataSource.sequenceLines(
                            todayRunningSequences = nowRunningSequencesOrNot(LocalDate.now())
                                .filter {
                                    it.start < LocalTime.now() && LocalTime.now() < it.end
                                }
                                .map { it.sequence }
                        )
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

    suspend fun sequence(seq: String, date: LocalDate): Sequence? {
        val conns = localDataSource.connsOfSeqWithTheirConnStops(seq)
            .groupBy {
                it.connName to it.tab
            }
            .filter { (a, _) ->
                val (connName, tab) = a
                val nowUsedTable = nowUsedTable(date, extractLineNumber(connName))
                nowUsedTable == tab
            }
            .map { (_, stops) ->
                val noCodes = stops.distinctBy {
                    it.copy(fixedCodes = "", type = DoesNotRun, from = LocalDate.now(), to = LocalDate.now())
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
                            line = it.line - 325_000,
                            connName = it.connName,
                            sequence = it.sequence,
                        )
                    },
                    noCodes.mapIndexed { i, it ->
                        BusStop(
                            time = it.time,
                            name = it.name,
                            line = it.line - 325_000,
                            nextStop = noCodes.getOrNull(i + 1)?.name,
                            connName = it.connName,
                            type = StopType(it.connStopFixedCodes),
                        )
                    }.distinct(),
                    timeCodes,
                    stops.first().fixedCodes,
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

        val before = buildList {
            if ('-' in seq && seq.endsWith('2')) add(seq.dropLast(1) + '1')
            addAll(sequenceConnections.first().filter { (_, s2) -> s2 == seq }.map { (s1, _) -> s1 })
        }

        val after = buildList {
            if ('-' in seq && seq.endsWith('1')) add(seq.dropLast(1) + '2')
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
                )
          },
            commonTimeCodes = commonTimeCodes,
            commonFixedCodes = commonFixedCodes.joinToString(" "),
        )
    }

    suspend fun busStopTimesOfSequence(seq: String, date: LocalDate): List<Pair<Boolean, List<LocalTime>>>? {
        val conns = localDataSource.connsOfSeqWithTheirConnStopTimes(seq)
            .groupBy { it.connName to it.tab }
            .filter { (a, _) ->
                val (connName, tab) = a
                val nowUsedTable = nowUsedTable(date, extractLineNumber(connName))
                nowUsedTable == tab
            }
            .map { (_, stops) ->
                stops.first().lowFloor to stops.map { it.time }.distinct()
            }
            .sortedBy {
                it.second.first()
            }

        if (conns.isEmpty()) return null

        return conns
    }

    private suspend fun sequenceBuses(seq: String, date: LocalDate) = localDataSource.connsOfSeq(seq, allTables(date)).ifEmpty { null }
    suspend fun firstBusOfSequence(seq: String, date: LocalDate) = localDataSource.firstConnOfSeq(seq, allTables(date))
    suspend fun lastBusOfSequence(seq: String, date: LocalDate) = localDataSource.lastConnOfSeq(seq, allTables(date))


    suspend fun write(
        connStops: Array<ConnStop>,
        stops: Array<Stop>,
        timeCodes: Array<TimeCode>,
        lines: Array<Line>,
        conns: Array<Conn>,
        version: Int,
    ) {
        preferenceDataSource.changeVersion(version)

        localDataSource.insertConnStops(*connStops)
        localDataSource.insertStops(*stops)
        localDataSource.insertTimeCodes(*timeCodes)
        localDataSource.insertLines(*lines)
        localDataSource.insertConns(*conns)
    }

    suspend fun connStops() = localDataSource.connStops()
    suspend fun stops() = localDataSource.stops()
    suspend fun timeCodes() = localDataSource.timeCodes()
    suspend fun lines() = localDataSource.lines()
    suspend fun conns() = localDataSource.conns()

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

    suspend fun removeFavourite(name: String) {
        preferenceDataSource.changeFavourites { favourites ->
            favourites - favourites.first { it.busName == name }
        }
    }

    suspend fun departures(date: LocalDate, stop: String): List<Departure> =
        localDataSource.departures(stop, allTables(date))
            .groupBy { "${it.line}/${it.connNumber}" to it.stopIndexOnLine }
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
                    line = info.line - 325_000,
                    lowFloor = info.lowFloor,
                    busStops = stops,
                    stopType = StopType(info.connStopFixedCodes),
                )
            }

    suspend fun oneWayLines() = localDataSource.oneDirectionLines()

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

    suspend fun nowRunningBus(busName: String, date: LocalDate): NowRunning =
        localDataSource.connWithItsStops(busName, nowUsedTable(date, extractLineNumber(busName))!!)
            .toList()
            .first { date.isThisTableNowUsed(it.first.tab) }
            .let { (conn, stops) ->
                NowRunning(
                    busName = conn.name,
                    lineNumber = conn.line - 325_000,
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

    suspend fun hasRestriction(busName: String, date: LocalDate) =
        localDataSource.hasRestriction(nowUsedTable(date, extractLineNumber(busName))!!)

    suspend fun lineValidity(busName: String, date: LocalDate) =
        localDataSource.validity(nowUsedTable(date, extractLineNumber(busName))!!)

    suspend fun doesBusExist(busName: String): Boolean {
        return localDataSource.doesConnExist(busName) != null
    }

    fun doesConnRunAt(connName: String): suspend (LocalDate) -> Boolean = runsAt@{ datum ->
        val tab = nowUsedTable(datum, extractLineNumber(connName)) ?: return@runsAt false

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
    ): Boolean = when {
        timeCodes anyAre RunsOnly -> timeCodes filter RunsOnly anySatisfies date
        timeCodes filter RunsAlso anySatisfies date -> true
        !date.runsToday(fixedCodes) -> false
        timeCodes filter DoesNotRun anySatisfies date -> false
        timeCodes noneAre Runs -> true
        timeCodes filter Runs anySatisfies date -> true
        else -> false
    }

    private val contentResolver = ctx.contentResolver

    fun copyFile(oldUri: Uri, newFile: File) {
        contentResolver.openInputStream(oldUri)!!.use { input ->
            newFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun seqName(s: String) = s.let {
        val seq = it.split("-")
        val rawSeq = seq[0]
        val notes = seq.getOrNull(1) ?: ""
        val validity = "([A-Z])\\d?".toRegex().matchEntire(notes)?.groups?.get(index = 0)?.value?.get(0)
        val hasValidity = validity != null
        val part = if (hasValidity) notes.drop(1) else notes
        val hasPart = part.isNotEmpty()
        val (validityNominative, validityGenitive) = this.sequenceTypes.first().mapValues { (_, type) ->
            type.nominative to type.genitive
        }[validity] ?: ("" to "")
        buildString {
            if (hasPart) this.append("$part. část ")
            if (hasPart && hasValidity) this.append("$validityGenitive ")
            if (!hasPart && hasValidity) this.append("$validityNominative ")
            this.append(rawSeq)
        }
    }

    suspend fun seqConnection(s: String) = "Potenciální návaznost na " + s.let {
        val seq = it.split("-")
        val rawSeq = seq[0]
        val notes = seq.getOrNull(1) ?: ""
        val validity = "([A-Z])\\d?".toRegex().matchEntire(notes)?.groups?.get(index = 0)?.value?.get(0)
        val hasValidity = validity != null
        val part = if (hasValidity) notes.drop(1) else notes
        val hasPart = part.isNotEmpty()
        val (validityAccusative, validityGenitive) = this.sequenceTypes.first().mapValues { (_, type) ->
            type.accusative to type.genitive
        }[validity] ?: ("" to "")

        buildString {
            if (hasPart) this.append("$part. část ")
            if (hasPart && hasValidity) this.append("$validityGenitive ")
            if (!hasPart && hasValidity) this.append("$validityAccusative ")
            this.append(rawSeq)
        }
    }

    suspend fun getSequenceComparator(): Comparator<String> {
        val sequenceTypes = sequenceTypes.first()

        return compareBy<String> {
            0
        }.thenBy {
            val type = "([A-Z])\\d?".toRegex().matchEntire(it.split("-").getOrNull(1) ?: "")?.groups?.get(index = 0)?.value?.get(0)
            type?.let {
                sequenceTypes[type]?.order
            } ?: 0
        }.thenBy {
            it.split("/")[1].split("-")[0].toIntOrNull() ?: 21
        }.thenBy {
            it.split("/")[0].toInt()
        }.thenBy {
            it.split("-").getOrNull(1) ?: ""
        }
    }

    suspend fun seqOfBus(busName: String, date: LocalDate) =
        localDataSource.seqOfConn(busName, nowUsedTable(date, extractLineNumber(busName))!!)
            .toList()
            .first { date.isThisTableNowUsed(it.first) }
            .second
}

private infix fun List<RunsFromTo>.anyAre(type: TimeCodeType) = any { it.type == type }
private infix fun List<RunsFromTo>.noneAre(type: TimeCodeType) = none { it.type == type }
private infix fun List<RunsFromTo>.filter(type: TimeCodeType) = filter { it.type == type }

private infix fun List<RunsFromTo>.anySatisfies(date: LocalDate) = any { it satisfies date }
private infix fun RunsFromTo.satisfies(date: LocalDate) = date in `in`

private fun String.partMayBeMissing() =
    if (matches("^[0-9]{1,2}/[0-9A-Z]{1,2}(-[A-Z]?[12]?)?$".toRegex())) split("-")[0]
    else if (matches("^/[0-9A-Z]{1,2}(-[A-Z]?[12]?)?$".toRegex())) "%" + split("-")[0]
    else if (matches("^[0-9A-Z]{1,2}$".toRegex())) "%/$this"
    else null

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
    LocalDate.of(1, 1, 1), // Den obnovy samostatného českého státu
    LocalDate.of(1, 1, 1), // Nový rok
    LocalDate.of(1, 5, 1), // Svátek práce
    LocalDate.of(1, 5, 8), // Den vítězství
    LocalDate.of(1, 7, 5), // Den slovanských věrozvěstů Cyrila a Metoděje,
    LocalDate.of(1, 7, 6), // Den upálení mistra Jana Husa
    LocalDate.of(1, 9, 28), // Den české státnosti
    LocalDate.of(1, 10, 28), // Den vzniku samostatného československého státu
    LocalDate.of(1, 11, 17), // Den boje za svobodu a demokracii
    LocalDate.of(1, 12, 24), // Štědrý den
    LocalDate.of(1, 12, 25), // 1. svátek vánoční
    LocalDate.of(1, 12, 26), // 2. svátek vánoční
).any {
    it.dayOfMonth == datum.dayOfMonth && it.month == datum.month
} || isEaster(datum)

// Je Velký pátek nebo Velikonoční pondělí
private fun isEaster(date: LocalDate): Boolean {
    val (bigFriday, easterMonday) = positionOfEasterInYear(date.year) ?: return false

    return date == easterMonday || date == bigFriday
}

// Poloha Velkého pátku a Velikonočního pondělí v roce
// Zdroj: https://cs.wikipedia.org/wiki/V%C3%BDpo%C4%8Det_data_Velikonoc#Algoritmus_k_v%C3%BDpo%C4%8Dtu_data_Velikonoc
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
    val eaterSundayFromTheStartOfMarch = 22 + d + e

    val bigFridayFromTheStartOfMarch = eaterSundayFromTheStartOfMarch - 2
    val bigFriday = LocalDate.of(year, Month.MARCH, 1).plusDays(bigFridayFromTheStartOfMarch - 1L)

    val easterMondayFromTheStartOfMarch = eaterSundayFromTheStartOfMarch + 1
    val easterMonday = LocalDate.of(year, Month.MARCH, 1).plusDays(easterMondayFromTheStartOfMarch - 1L)

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

fun filterTimeCodesAndMakeReadable(timeCodes: List<RunsFromTo>) = timeCodes
    .remove {
        it.type == DoesNotRun && it.`in`.start == noCode && it.`in`.endInclusive == noCode
    }
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