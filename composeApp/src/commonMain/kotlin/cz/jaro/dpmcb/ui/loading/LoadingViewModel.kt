package cz.jaro.dpmcb.ui.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.BuildKonfig
import cz.jaro.dpmcb.data.DividedSequencesWithMultipleBuses
import cz.jaro.dpmcb.data.DownloadedData
import cz.jaro.dpmcb.data.FileStorageManager
import cz.jaro.dpmcb.data.LineTraction
import cz.jaro.dpmcb.data.Logger
import cz.jaro.dpmcb.data.SequenceConnections
import cz.jaro.dpmcb.data.SequenceTypes
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LineStopNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.number
import cz.jaro.dpmcb.data.entities.toLongLine
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.Companion.runs
import cz.jaro.dpmcb.data.entities.types.toDirection
import cz.jaro.dpmcb.data.generated.Conn
import cz.jaro.dpmcb.data.generated.ConnStop
import cz.jaro.dpmcb.data.generated.Line
import cz.jaro.dpmcb.data.generated.SeqGroup
import cz.jaro.dpmcb.data.generated.SeqOfConn
import cz.jaro.dpmcb.data.generated.Stop
import cz.jaro.dpmcb.data.generated.TimeCode
import cz.jaro.dpmcb.data.getText
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SuperNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.async
import cz.jaro.dpmcb.data.helperclasses.backgroundInfo
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.noCode
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.data.helperclasses.toDateWeirdly
import cz.jaro.dpmcb.data.helperclasses.toTimeWeirdly
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.tuples.Quadruple
import cz.jaro.dpmcb.data.tuples.Quintuple
import cz.jaro.dpmcb.data.version
import cz.jaro.dpmcb.data.work
import cz.jaro.dpmcb.ui.main.SuperRoute
import cz.jaro.dpmcb.ui.map.DiagramManager
import cz.jaro.dpmcb.ui.map.supportsLineDiagram
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.remoteconfig.remoteConfig
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@DslMarker
private annotation class TimetableProcessing

@OptIn(ExperimentalTime::class)
class LoadingViewModel(
    private val repo: SpojeRepository,
    private val diagramManager: DiagramManager,
    firebase: FirebaseApp,
    private val params: Parameters,
) : ViewModel(), Logger by repo {

    data class Parameters(
        val update: Boolean,
        val link: String?,
        val reset: suspend () -> Unit,
    )

    private val navigate = MutableStateFlow<SuperNavigateFunction?>(null)
    private val goTo = MutableStateFlow(null as SuperRoute?)

    fun setNavigate(navigate: SuperNavigateFunction) {
        this.navigate.value = navigate
        tryNavigate()
    }

    companion object {
        const val META_DATA_VERSION = 5
    }

    val state: StateFlow<LoadingState>
        field = MutableStateFlow<LoadingState>(LoadingState.Loading())

    val settings = repo.settings

    @Serializable
    data class GroupValidity(
        val validFrom: LocalDate,
        val validTo: LocalDate,
    )

    @Serializable
    data class Group(
        val validity: GroupValidity,
        val dividedSequencesWithMultipleBuses: DividedSequencesWithMultipleBuses = emptyList(),
        val lineTraction: LineTraction = emptyMap(),
        val sequenceConnections: SequenceConnections = emptyList(),
        val sequenceTypes: SequenceTypes = emptyMap(),
        val label: String = "",
        val sequences: Map<SequenceCode, List<BusName>>,
    )

    @Serializable
    data class SequencesFile(
        @SerialName($$"$schema")
        val schema: String = "",
        val dividedSequencesWithMultipleBuses: DividedSequencesWithMultipleBuses = emptyList(),
        val lineTraction: LineTraction = emptyMap(),
        val sequenceConnections: SequenceConnections = emptyList(),
        val sequenceTypes: SequenceTypes = emptyMap(),
        val groups: Map<SequenceGroup, Group>,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val _ = params.update || repo.version == -1
            } catch (e: Exception) {
                recordException(e)
                e.printStackTrace()
                state.value = LoadingState.Error
                return@launch
            }

            if (params.update || repo.needsToDownloadData && repo.version == -1) {
                downloadNewData(this)
                return@launch
            }

            try {
                doesEverythingWork()
            } catch (e: Exception) {
                recordException(e)
                e.printStackTrace()
                state.value = LoadingState.Error
                return@launch
            }

            if (!repo.needsToDownloadData || !repo.isOnline() || !repo.settings.value.checkForUpdates) {
                goTo.value = SuperRoute.Main(params.link)
                withContext(Dispatchers.Main) {
                    tryNavigate()
                }
                return@launch
            }

            state.value = LoadingState.Loading("Kontrola dostupnosti aktualizací")

            goTo.value = SuperRoute.Main(params.link, isDataUpdateNeeded(), appVersionToUpdate())
            withContext(Dispatchers.Main) {
                tryNavigate()
            }
        }
    }

    private fun tryNavigate() {
        if (navigate.value != null && goTo.value != null) {
            navigate.value!!(goTo.value!!, popUpTo<SuperRoute.Loading>())
            goTo.value = null
        }
    }

    private suspend fun doesEverythingWork(): Nothing? {
        if (repo.lineNumbers(SystemClock.todayHere()).isEmpty()) {
            error("No lines loaded")
        }
        if (!diagramManager.checkDiagram()) {
            error("Diagram not loaded")
        }
        return null
    }

    @IgnorableReturnValue
    fun onEvent(e: LoadingEvent) = viewModelScope.launch {
        when (e) {
            LoadingEvent.DownloadDataIfError -> withContext(Dispatchers.Main) {
                goTo.value = SuperRoute.Loading(link = params.link, update = true)
                tryNavigate()
            }
        }
    }

    private val client = HttpClient()
    private val versionProxy = "https://ygbqqztfvcnqxxbqvxwb.supabase.co/functions/v1/version"

    private suspend fun isDataUpdateNeeded(): Boolean {
        val localVersion = repo.version

        val onlineVersion = async {
            withTimeoutOrNull(3_000) {
                client.get(versionProxy).body()
            } ?: -2
        }

        return localVersion < onlineVersion.await()
    }

    private suspend fun appVersionToUpdate(): Version? {
        if (isDebug()) return null

        val newestVersion = latestAppVersion().work()
        val localVersion = BuildKonfig.versionName.toVersion(false)

        if (newestVersion != null && localVersion < newestVersion) return newestVersion

        if (!localVersion.isPreRelease) return null

        val preReleaseVersion = latestAppPreReleaseVersion(localVersion).work() ?: return null

        if (localVersion < preReleaseVersion) return preReleaseVersion
        return null
    }

    private suspend fun downloadText(path: String) = FileStorageManager().use { manager ->
        manager.getText(path) { progress ->
            state.update {
                require(it is LoadingState.Loading)
                it.copy(progress = progress)
            }
        }
    }

    private suspend fun downloadNewData(
        scope: CoroutineScope,
    ) {

        if (!repo.isOnline()) {
            state.value = LoadingState.Offline
            return
        }

        val m = when {
            supportsLineDiagram() -> "6"
            else -> "5"
        }

        state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nAnalyzování nové verze (0/$m)"
        )

        val newVersion = client.get(versionProxy).body<Int>()

        val connStops: MutableList<ConnStop> = mutableListOf()
        val stops: MutableList<Stop> = mutableListOf()
        val timeCodes: MutableList<TimeCode> = mutableListOf()
        val lines: MutableList<Line> = mutableListOf()
        val conns: MutableList<Conn> = mutableListOf()
        val seqOfConns: MutableList<SeqOfConn> = mutableListOf()
        val seqGroups: MutableList<SeqGroup> = mutableListOf()
        var loadedData = DownloadedData(version = newVersion).work()

        state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nOdstraňování starých dat (1/$m)"
        )

        repo.dropAllTables()
        repo.createTables()

        state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nStahování dat (2/$m)",
            progress = null,
        )

        val sequencesPath = "kurzy4.json"
        val platformsPath = "stanoviste.json"
        val diagramPath = "schema.svg"
        val dataPath = "data${META_DATA_VERSION}/data${newVersion}.json"

        val json = downloadText(dataPath)

        state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nStahování kurzů (3/$m)",
            progress = null,
        )

        val json2 = downloadText(sequencesPath)

        val sequences = json2.fromJson<SequencesFile>()

        sequences.extractSequences()
            .let { (groups, sequences) ->
                seqGroups += groups
                seqOfConns += sequences
            }

        seqGroups += SeqGroup(
            seqGroup = SequenceGroup.invalid,
            validFrom = noCode,
            validTo = noCode,
        )

        loadedData = loadedData.copy(
            dividedSequencesWithMultipleBuses = sequences.dividedSequencesWithMultipleBuses,
            linesTraction = sequences.lineTraction,
            sequenceConnections = sequences.sequenceConnections,
            sequenceTypes = sequences.sequenceTypes,
        )

        resetRemoteConfig()

        state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nStahování stanovišť (4/$m)",
            progress = null,
        )

        val json3 = downloadText(platformsPath)
        val platforms =
            json3.fromJson<Map<LongLine, Map<Direction, Map<LineStopNumber, Platform>>>>()

        state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nZpracovávání dat (5/$m)",
            progress = null,
        )

        val data: Map<Table, Map<TableType, List<List<String>>>> = Json.decodeFromString(json)

        data.extractData(
            connsWithSequence = seqOfConns.map { it.line / it.connNumber }.distinct(),
            platforms = platforms,
            scope = scope,
            addConnToSequence = {
                seqOfConns += SeqOfConn(
                    line = it.line,
                    connNumber = it.connNumber,
                    sequence = SequenceCode.invalid,
                    orderInSequence = 0,
                    seqGroup = SequenceGroup.invalid,
                )
            },
        ).forEach { (connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable) ->
            connStops += connStopsOfTable
            timeCodes += timeCodesOfTable
            stops += stopsOfTable
            lines += linesOfTable
            conns += connsOfTable
        }

        if (supportsLineDiagram()) {
            state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nStahování schématu MHD (6/6)",
                progress = null,
            )

            diagramManager.downloadDiagram(diagramPath) { progress ->
                state.update {
                    require(it is LoadingState.Loading)
                    it.copy(progress = progress)
                }
            }
        }

        state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nUkládání",
        )

        println(conns)
        println(lines)
        println(stops)
        println(connStops)
        println(timeCodes)
        println(seqOfConns)
        println(seqGroups)

        repo.write(
            connStops = connStops.distinctBy { Triple(it.tab, it.connNumber, it.stopIndexOnLine) },
            stops = stops.distinctBy { it.tab to it.stopNumber },
            timeCodes = timeCodes.distinctBy { Quadruple(it.tab, it.code, it.connNumber, it.termIndex) },
            lines = lines.distinctBy { it.tab },
            conns = conns.distinctBy { it.tab to it.connNumber },
            seqOfConns = seqOfConns.distinctBy { Quadruple(it.line, it.connNumber, it.sequence, it.seqGroup) },
            seqGroups = seqGroups.distinctBy { it.seqGroup },
            data = loadedData,
        ) { progress, label, groupProgress ->
            state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\n$backgroundInfo\nUkládání ($label – ${groupProgress.times(100).roundToInt()} %)",
                progress = progress,
            )
        }
        state.update {
            (it as LoadingState.Loading).copy(progress = null)
        }
//        repo.write(
//            connStops = emptyList(),
//            stops = listOf(Stop(Table(LongLine(325009), 0), 0, LongLine(325009), "Suché Vrbné", "X")),
//            timeCodes = emptyList(),
//            lines = listOf(Line(Table(LongLine(325009), 0), LongLine(325009), "", VehicleType.TROLEJBUS, LineType.MESTSKA, false, LocalDate(2025, 1, 1), LocalDate(2025, 12, 31), ShortLine(9))),
//            conns = emptyList(),
//            seqOfConns = emptyList(),
//            seqGroups = emptyList(),
//            data = loadedData,
//        )

        withContext(Dispatchers.Main) {
            params.reset()
        }

        repo.connStops().work()
        repo.stops().work()
        repo.timeCodes().work()
        repo.lines().work()
        repo.conns().work()
        repo.seqOfConns().work()
        repo.seqGroups().work()
        repo.lineNumbers(SystemClock.todayHere()).work()

        return
    }

    private val remoteConfig = Firebase.remoteConfig(firebase)

    private suspend fun resetRemoteConfig() {
        remoteConfig.reset()
        remoteConfig.settings {
            minimumFetchInterval = 1.hours
        }
        remoteConfig.fetchAndActivate()
    }
}

@TimetableProcessing
private suspend fun Map<Table, Map<TableType, List<List<String>>>>.extractData(
    connsWithSequence: List<BusName>,
    platforms: Map<LongLine, Map<Direction, Map<LineStopNumber, Platform>>>,
    scope: CoroutineScope,
    addConnToSequence: (conn: Conn) -> Unit,
): List<Quintuple<List<ConnStop>, List<TimeCode>, List<Stop>, List<Line>, List<Conn>>> {
    val data = mapKeys { (key) ->
        Table(LongLine(key.value.substringBefore('-').toInt() + 325_000), key.number()) // TODO: Nezávislost dat na předčíslí linky
    }

    return data.map { (tab, lineData) ->
        scope.async {

            @TimetableProcessing
            fun <T> processTable(
                type: TableType,
                processRow: (List<String>) -> T,
            ) = scope.async {
                lineData
                    .getOrElse(type) {
                        error("$type not found in the table $tab, available tables are: ${lineData.keys.joinToString()}")
                    }
                    .mapNotNull(processRow)
            }

            val fixedCodesA = processTable(TableType.Pevnykod) { row ->
                row[0] to row[1]
            }

            val timeCodesA = processTable(TableType.Caskody) { row ->
                val type = TimeCodeType.entries.find { it.code.toString() == row[4] } ?: TimeCodeType.DoesNotRun
                TimeCode(
                    line = row[0].toLongLine(),
                    connNumber = row[1].toInt(),
                    code = row[3].toShort(),
                    termIndex = row[2].toShort(),
                    type = type,
                    validFrom = row[5].toDateWeirdly(),
                    validTo = row[6].ifEmpty { row[5] }.toDateWeirdly(),
                    tab = tab,
                    runs2 = type.runs,
                )
            }

            val _ = processTable(TableType.Zaslinky) {}

            val fixedCodes = fixedCodesA.await().toMap()
            val timeCodes = timeCodesA.await()

            val connStopsWithoutPlatformsA = processTable(TableType.Zasspoje) { row ->
                ConnStop(
                    line = row[0].toLongLine(),
                    connNumber = row[1].toInt(),
                    stopIndexOnLine = row[2].toInt(),
                    stopNumber = row[3].toInt(),
                    kmFromStart = row[9].ifEmpty { return@processTable null }.toInt(),
                    arrival = row[10].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toTimeWeirdly(),
                    departure = row[11].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toTimeWeirdly(),
                    tab = tab,
                    fixedCodes = row.slice(6..7).filter { it.isNotEmpty() }.joinToString(" ") {
                        fixedCodes[it] ?: it
                    },
                    platform = null,
                )
            }

            val stopsA = processTable(TableType.Zastavky) { row ->
                Stop(
                    line = row[0].toLongLine(),
                    stopNumber = row[1].toInt(),
                    stopName = row[2],
                    fixedCodes = row.slice(6..11).filter { it.isNotEmpty() }.joinToString(" ") {
                        fixedCodes[it] ?: it
                    },
                    tab = tab,
                )
            }

            val linesA = processTable(TableType.Linky) { row ->
                val number = row[0].toLongLine()
                Line(
                    number = number,
                    route = row[1],
                    vehicleType = Json.decodeFromString("\"${row[4]}\""),
                    lineType = Json.decodeFromString("\"${row[3]}\""),
                    hasRestriction = row[5] != "0",
                    validFrom = row[13].toDateWeirdly(),
                    validTo = row[14].toDateWeirdly(),
                    tab = tab,
                    shortNumber = number.toShortLine(),
                )
            }

            val connStopsWithoutPlatforms = connStopsWithoutPlatformsA.await()

            val connsA = processTable(TableType.Spoje) { row ->
                val line = row[0].toLongLine()
                val connNumber = row[1].toInt()
                Conn(
                    line = line,
                    connNumber = connNumber,
                    fixedCodes = row.slice(2..11).filter { it.isNotEmpty() }.joinToString(" ") {
                        fixedCodes[it] ?: it
                    },
                    direction = connStopsWithoutPlatforms
                        .filter { it.connNumber == connNumber }
                        .sortedBy { it.stopIndexOnLine }
                        .filter { it.time != null }
                        .let { stops ->
                            (stops.first().time!! <= stops.last().time!! && stops.first().kmFromStart <= stops.last().kmFromStart).toDirection()
                        },
                    tab = tab,
                    name = line / connNumber
                )
            }

            val _ = processTable(TableType.LinExt) {}
            val _ = processTable(TableType.Dopravci) {}
            val _ = processTable(TableType.Udaje) {}
//            processTable(TableType.VerzeJDF) {}

            val conns = connsA.await()

            val connStopsA = scope.async {
                connStopsWithoutPlatforms.groupBy { it.tab }.flatMap { (tab, stopsOfTab) ->
                    val platformsOfLine = platforms[tab.line()]
                    stopsOfTab.groupBy { it.line / it.connNumber }.flatMap { (connName, stops) ->
                        val conn = conns.find { it.tab == tab && it.name == connName }
                        val platformsOfConn = conn?.direction?.let { platformsOfLine?.get(it) }
                        stops.map { stop ->
                            stop.copy(
                                platform = platformsOfConn?.get(stop.stopIndexOnLine)?.takeUnless { it.isEmpty() }
                            )
                        }
                    }
                }
            }

            val blankTimeCodes = conns.map { conn ->
                val type =
                    if (timeCodes.any { it.type != TimeCodeType.DoesNotRun && it.connNumber == conn.connNumber }) TimeCodeType.Runs else TimeCodeType.DoesNotRun
                TimeCode(
                    line = conn.line,
                    connNumber = conn.connNumber,
                    code = -1,
                    termIndex = 0,
                    type = type,
                    validFrom = noCode,
                    validTo = noCode,
                    tab = conn.tab,
                    runs2 = type.runs,
                )
            }

            conns.forEach { conn ->
                if (conn.name !in connsWithSequence) addConnToSequence(conn)
            }

            val stops = stopsA.await()
            val lines = linesA.await()
            val connStops = connStopsA.await()

            Quintuple(connStops, timeCodes + blankTimeCodes, stops, lines, conns)
//            Quintuple(listOf<ConnStop>(), listOf<TimeCode>(), listOf<Stop>(), listOf<Line>(), listOf<Conn>())
        }
    }
        .awaitAll()
}

@TimetableProcessing
private fun LoadingViewModel.SequencesFile.extractSequences(): Pair<List<SeqGroup>, List<SeqOfConn>> =
    groups.map { (seqGroup, groupData) ->
        SeqGroup(
            seqGroup = seqGroup,
            validFrom = groupData.validity.validFrom,
            validTo = groupData.validity.validTo,
        ) to groupData.sequences.flatMap { (sequenceCode, buses) ->
            buses.mapIndexed { i, bus ->
                SeqOfConn(
                    line = bus.line(),
                    connNumber = bus.bus(),
                    sequence = sequenceCode,
                    orderInSequence = i,
                    seqGroup = seqGroup,
                )
            }
        }
    }.let { list ->
        list.map { it.first } to list.flatMap { it.second }
    }

private val ConnStop.time get() = departure ?: arrival