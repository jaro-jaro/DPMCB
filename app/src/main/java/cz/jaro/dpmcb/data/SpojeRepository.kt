package cz.jaro.dpmcb.data

import android.app.Application
import android.net.Uri
import android.widget.Toast
import cz.jaro.dpmcb.data.database.Dao
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.helperclasses.PartOfConn
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.allTrue
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.anyTrue
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechAccusative
import cz.jaro.dpmcb.data.realtions.InfoStops
import cz.jaro.dpmcb.data.realtions.InfoStopsCodes
import cz.jaro.dpmcb.data.realtions.LineLowFloorConnId
import cz.jaro.dpmcb.data.realtions.LineLowFloorConnIdSeq
import cz.jaro.dpmcb.data.realtions.LineLowFloorConnIdTimeNameIndexStops
import cz.jaro.dpmcb.data.realtions.LineTimeNameConnIdNextStop
import cz.jaro.dpmcb.data.realtions.NameAndTime
import cz.jaro.dpmcb.data.realtions.NameTimeIndex
import cz.jaro.dpmcb.data.realtions.NameTimeIndexOnLine
import cz.jaro.dpmcb.data.realtions.RunsFromTo
import cz.jaro.dpmcb.data.realtions.Sequence
import cz.jaro.dpmcb.data.realtions.TimeNameConnId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Single
class SpojeRepository(
    ctx: Application,
    private val localDataSource: Dao,
    private val preferenceDataSource: PreferenceDataSource,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _date = MutableStateFlow(LocalDate.now())
    val date = _date.asStateFlow()

    private val _onlineMode = MutableStateFlow(Settings().autoOnline)
    val isOnlineModeEnabled = _onlineMode.asStateFlow()

    val settings = preferenceDataSource.settings

    val showLowFloor = preferenceDataSource.lowFloor

    val showDeparturesOnly = preferenceDataSource.departures

    val favourites = preferenceDataSource.favourites

    val version = preferenceDataSource.version

    private val makeText = { text: String ->
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

    private val sequencesMap = mutableMapOf<LocalDate, List<String>>()

    private suspend fun nowRunningSequencesOrNotInternal(date: LocalDate): List<String> {
        return localDataSource.fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
            date = date,
            tabs = allTables(date),
        )
            .mapNotNull { (seq, fixedCodes) ->

                if (fixedCodes.isEmpty()) return@mapNotNull null

                val pevne = fixedCodes.first().split(" ").filter { kod ->
                    fixedCodes.all {
                        it.split(" ").contains(kod)
                    }
                }

                Pair(seq, pevne)
            }
            .filter { (_, fixedCodes) ->
                date.runsToday(fixedCodes.joinToString(" "))
            }
            .map {
                it.first
            }
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

    suspend fun busDetail(busId: String, date: LocalDate) =
        localDataSource.connWithItsConnStopsAndCodes(busId, nowUsedTable(date, extractLineNumber(busId))!!).run {
            val noCodes = distinctBy {
                it.copy(fixedCodes = "", runs = false, from = LocalDate.now(), to = LocalDate.now())
            }
            val timeCodes = map {
                RunsFromTo(
                    runs = it.runs,
                    `in` = it.from..it.to
                )
            }.distinctBy {
                it.runs to it.`in`.toString()
            }
            InfoStopsCodes(
                first().let {
                    LineLowFloorConnIdSeq(
                        lowFloor = it.lowFloor,
                        line = it.line - 325_000,
                        connId = it.connId,
                        sequence = it.sequence,
                    )
                },
                noCodes.mapIndexed { i, it ->
                    LineTimeNameConnIdNextStop(
                        time = it.time,
                        name = it.name,
                        line = it.line - 325_000,
                        nextStop = noCodes.getOrNull(i + 1)?.name,
                        connId = it.connId
                    )
                }.distinct(),
                timeCodes,
                first().fixedCodes,
            )
        }

    suspend fun codes(connId: String, date: LocalDate) =
        localDataSource.codes(connId, nowUsedTable(date, extractLineNumber(connId))!!).run {
            map { RunsFromTo(runs = it.runs, `in` = it.from..it.to) } to makeFixedCodesReadable(first().fixedCodes)
        }

    private fun extractLineNumber(connId: String) = connId.split("-")[1].toInt()

    suspend fun favouriteBus(busId: String, date: LocalDate) =
        localDataSource.connWithItsConnStopsAndCodes(busId, nowUsedTable(date, extractLineNumber(busId))!!)
            .run {
                Pair(
                    first().let { LineLowFloorConnId(it.lowFloor, it.line - 325_000, it.connId) },
                    map { TimeNameConnId(it.time, it.name, it.connId) }.distinct(),
                )
            }

    suspend fun stopNamesOfLine(line: Int, date: LocalDate) =
        localDataSource.stopNamesOfLine(line + 325_000, nowUsedTable(date, line + 325_000)!!)

    suspend fun nextStopNames(line: Int, thisStop: String, date: LocalDate) =
        localDataSource.nextStops(line + 325_000, thisStop, nowUsedTable(date, line + 325_000)!!)

    suspend fun timetable(line: Int, thisStop: String, nextStop: String, date: LocalDate) =
        localDataSource.connStopsOnLineWithNextStopAtDate(
            line = line + 325_000,
            stop = thisStop,
            nextStop = nextStop,
            date = date,
            tab = nowUsedTable(date, line + 325_000)!!
        ).filter {
            date.runsToday(it.fixedCodes)
        }

    suspend fun findSequences(seq: String) = seq.partMayBeMissing()?.let { s ->
        localDataSource.findSequences(
            sequence1 = s,
            sequence2 = "$s-1",
            sequence3 = "$s-2",
            sequence4 = "$s + %",
            sequence5 = "$s-1 + %",
            sequence6 = "$s-2 + %",
            sequence7 = "% + $s",
            sequence8 = "% + $s-1",
            sequence9 = "% + $s-2",
            sequence10 = "$s-V",
            sequence11 = "$s-V1",
            sequence12 = "$s-V2",
            sequence13 = "$s-V + %",
            sequence14 = "$s-V1 + %",
            sequence15 = "$s-V2 + %",
            sequence16 = "% + $s-V",
            sequence17 = "% + $s-V1",
            sequence18 = "% + $s-V2",
        ).sortedWith(sequenceComparator)
    } ?: emptyList()

    val nowRunningOrNot = channelFlow {
        coroutineScope {
            while (currentCoroutineContext().isActive) {
                launch {
                    send(
                        localDataSource.nowRunning(LocalTime.now(), nowRunningSequencesOrNot(LocalDate.now()))
                            .map { (s, lines) ->
                                s to lines.map { it - 325_000 }
                            }
                            .sortedWith(Comparator.comparing({ it.first }, sequenceComparator))
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
        val conns = localDataSource.connsOfSeqWithTheirConnStops(seq, "$seq + %", "% + $seq")
            .groupBy {
                it.connId to it.tab
            }
            .filter { (a, _) ->
                val (connId, tab) = a
                val nowUsedTable = nowUsedTable(date, extractLineNumber(connId))
                nowUsedTable == tab
            }
            .map { (_, stops) ->
                val noCodes = stops.distinctBy {
                    it.copy(fixedCodes = "", runs = false, from = LocalDate.now(), to = LocalDate.now())
                }
                val timeCodes = stops.map {
                    RunsFromTo(
                        runs = it.runs,
                        `in` = it.from..it.to
                    )
                }.distinctBy {
                    it.runs to it.`in`.toString()
                }
                InfoStopsCodes(
                    stops.first().let {
                        LineLowFloorConnIdSeq(
                            lowFloor = it.lowFloor,
                            line = it.line - 325_000,
                            connId = it.connId,
                            sequence = it.sequence,
                        )
                    },
                    noCodes.mapIndexed { i, it ->
                        LineTimeNameConnIdNextStop(
                            time = it.time,
                            name = it.name,
                            line = it.line - 325_000,
                            nextStop = noCodes.getOrNull(i + 1)?.name,
                            connId = it.connId
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

        val caskody = conns.first().timeCodes.filter { kod ->
            conns.all {
                it.timeCodes.contains(kod)
            }
        }

        val pevne = conns.first().fixedCodes.split(" ").filter { kod ->
            conns.all {
                it.fixedCodes.split(" ").contains(kod)
            }
        }

        val firstSeq = conns.first().info.sequence!!.split(" + ").first()
        val secondSeq = conns.first().info.sequence!!.split(" + ").last()

        val before = when {
            firstSeq.matches("[12]/5\\d-V?2".toRegex()) -> listOf(firstSeq.split("-")[0] + "-1", firstSeq.split("-")[0] + "-V1")
            '-' in firstSeq && firstSeq.endsWith('2') -> listOf(firstSeq.dropLast(1) + '1')
            else -> listOf()
        }

        val after = when {
            secondSeq.matches("[12]/5\\d-V?1".toRegex()) -> listOf(secondSeq.split("-")[0] + "-2", secondSeq.split("-")[0] + "-V2")
            '-' in secondSeq && secondSeq.endsWith('1') -> listOf(secondSeq.dropLast(1) + '2')
            else -> listOf()
        }

        return Sequence(
            name = conns.first().info.sequence!!,
            before = before,
            after = after,
            buses = conns.map { InfoStops(it.info, it.stops) },
            commonTimeCodes = caskody,
            commonFixedCodes = pevne.joinToString(" "),
        )
    }

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
            listOf(part).plus(favourites).distinctBy { it.busId }
        }
    }

    suspend fun removeFavourite(id: String) {
        preferenceDataSource.changeFavourites { favourites ->
            favourites - favourites.first { it.busId == id }
        }
    }

    suspend fun departures(date: LocalDate, stop: String): List<LineLowFloorConnIdTimeNameIndexStops> =
        localDataSource.connsStoppingOnStopName(stop, allTables(date))
            .groupBy { "S-${it.line}-${it.connNumber}" to it.stopIndexOnLine }
            .map { Triple(it.key.first, it.key.second, it.value) }
            .filter { (_, _, list) ->
                val timeCodes = list.map { RunsFromTo(it.runs, it.from..it.to) }.distinctBy { it.runs to it.`in`.toString() }
                runsAt(timeCodes, list.first().fixedCodes, date)
            }
            .map { Triple(it.first, it.second, it.third.first()) }
            .let { list ->
                val connStops = localDataSource.connStops(list.map { it.first }, allTables(date))
                list.map { Quadruple(it.first, it.second, it.third, connStops[it.first]!!) }
            }
            .map { (connId, stopIndexOnLine, info, stops) ->
                LineLowFloorConnIdTimeNameIndexStops(
                    name = info.name,
                    time = info.time,
                    stopIndexOnLine = stopIndexOnLine,
                    busId = connId,
                    line = info.line - 325_000,
                    lowFloor = info.lowFloor,
                    busStops = stops
                )
            }
    suspend fun oneWayLines() = localDataSource.oneDirectionLines()

    fun findMiddleStop(stops: List<NameAndTime>): NameTimeIndex {
        fun NameAndTime.indexOfDuplicate() = stops.filter { it.name == name }.takeUnless { it.size == 1 }?.indexOf(this)

        val lastCommonStop = stops.indexOfLast {
            it.indexOfDuplicate() == 0
        }

        val firstReCommonStop = stops.indexOfFirst {
            it.indexOfDuplicate() == 1
        }

        val last = stops[(lastCommonStop + firstReCommonStop).div(2F).roundToInt()]
        return NameTimeIndex(
            last.name,
            last.time,
            stops.indexOf(last)
        )
    }

    @JvmName("findMiddleStop2")
    fun findMiddleStop(stops: List<NameTimeIndexOnLine>): NameTimeIndex? {
        fun NameTimeIndexOnLine.indexOfDuplicate() = stops.filter { it.name == name }.takeUnless { it.size == 1 }?.indexOf(this)

        val lastCommonStop = stops.indexOfLast {
            it.indexOfDuplicate() == 0
        }

        val firstReCommonStop = stops.indexOfFirst {
            it.indexOfDuplicate() == 1
        }

        if (lastCommonStop == -1 || firstReCommonStop == -1) return null

        val last = stops[(lastCommonStop + firstReCommonStop).div(2F).roundToInt()]
        return NameTimeIndex(
            last.name,
            last.time,
            stops.indexOf(last)
        )
    }

    suspend fun nowRunningBus(busId: String, date: LocalDate): Pair<Conn, List<NameAndTime>> =
        localDataSource.connWithItsStops(busId, nowUsedTable(date, extractLineNumber(busId))!!)
            .toList()
            .first { date.isThisTableNowUsed(it.first.tab) }

    val isOnline = flow {
        while (currentCoroutineContext().isActive) {
            emit(ctx.isOnline)
            delay(5000)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ctx.isOnline)

    val hasAccessToMap = isOnline.combine(isOnlineModeEnabled) { isOnline, onlineMode ->
        isOnline && onlineMode
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ctx.isOnline && settings.value.autoOnline)

    suspend fun hasRestriction(busId: String, date: LocalDate) =
        localDataSource.hasRestriction(nowUsedTable(date, extractLineNumber(busId))!!)

    suspend fun lineValidity(busId: String, date: LocalDate) =
        localDataSource.validity(nowUsedTable(date, extractLineNumber(busId))!!)

    suspend fun doesBusExist(busId: String): Boolean {
        return localDataSource.doesConnExist(busId) != null
    }

    fun doesConnRunAt(spojId: String): suspend (LocalDate) -> Boolean = runsAt@{ datum ->
        val tab = nowUsedTable(datum, extractLineNumber(spojId)) ?: return@runsAt false

        val list = localDataSource.codes(spojId, tab).map { RunsFromTo(it.runs, it.from..it.to) to it.fixedCodes }

        runsAt(
            timeCodes = list.map { it.first },
            fixedCodes = list.first().second,
            date = datum,
        )
    }

    fun runsAt(
        timeCodes: List<RunsFromTo>,
        fixedCodes: String,
        date: LocalDate,
    ): Boolean = listOf(
        (timeCodes.filter { it.runs }.ifEmpty { null }?.any { date in it.`in` } ?: true),
        timeCodes.filter { !it.runs }.none { date in it.`in` },
        date.runsToday(fixedCodes),
    ).allTrue()

    private val contentResolver = ctx.contentResolver

    fun copyFile(oldUri: Uri, newFile: File) {
        contentResolver.openInputStream(oldUri)!!.use { input ->
            newFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

private fun String.partMayBeMissing() =
    if (matches("^[0-9]{1,2}/[0-9A-Z]{1,2}(-[A-Z]?[12]?)?$".toRegex())) split("-")[0]
    else if (matches("^/[0-9A-Z]{1,2}(-[A-Z]?[12]?)?$".toRegex())) "%" + split("-")[0]
    else null

private fun LocalDate.runsToday(fixedCodes: String) = fixedCodes
    .split(" ")
    .mapNotNull {
        when (it) {
            "1" -> dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY && !isPublicHoliday(this) // jede v pracovních dnech
            "2" -> dayOfWeek == DayOfWeek.SUNDAY || isPublicHoliday(this) // jede v neděli a ve státem uznané svátky
            "3" -> dayOfWeek == DayOfWeek.MONDAY // jede v pondělí
            "4" -> dayOfWeek == DayOfWeek.TUESDAY // jede v úterý
            "5" -> dayOfWeek == DayOfWeek.WEDNESDAY // jede ve středu
            "6" -> dayOfWeek == DayOfWeek.THURSDAY // jede ve čtvrtek
            "7" -> dayOfWeek == DayOfWeek.FRIDAY // jede v pátek
            "8" -> dayOfWeek == DayOfWeek.SATURDAY // jede v sobotu
            "14" -> null // bezbariérově přístupná zastávka
            "19" -> null // ???
            "24" -> null // spoj s částečně bezbariérově přístupným vozidlem, nutná dopomoc průvodce
            "28" -> null // zastávka s možností přestupu na železniční dopravu
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

fun makeFixedCodesReadable(fixedCodes: String) = fixedCodes
    .split(" ")
    .mapNotNull {
        when (it) {
            "1" -> "Jede v pracovních dnech"
            "2" -> "Jede v neděli a ve státem uznané svátky"
            "3" -> "Jede v pondělí"
            "4" -> "Jede v úterý"
            "5" -> "Jede ve středu"
            "6" -> "Jede ve čtvrtek"
            "7" -> "Jede v pátek"
            "8" -> "Jede v sobotu"
            "14" -> "Bezbariérově přístupná zastávka"
            "19" -> null
            "24" -> "Spoj s částečně bezbariérově přístupným vozidlem, nutná dopomoc průvodce"
            "28" -> "Zastávka s možností přestupu na železniční dopravu"
            else -> null
        }
    }

private val sequenceComparator = compareBy<String> {
    it.contains("V")
}.thenBy {
    it.split(" + ")[0].split("/")[1].split("-")[0].toIntOrNull() ?: 21
}.thenBy {
    it.split(" + ")[0].split("/")[0].toInt()
}.thenBy {
    it.split(" + ")[0].split("-").getOrNull(1) ?: ""
}