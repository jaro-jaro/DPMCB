package cz.jaro.dpmcb.ui.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.database.entities.Conn
import cz.jaro.dpmcb.data.database.entities.ConnStop
import cz.jaro.dpmcb.data.database.entities.Line
import cz.jaro.dpmcb.data.database.entities.SeqGroup
import cz.jaro.dpmcb.data.database.entities.SeqOfConn
import cz.jaro.dpmcb.data.database.entities.SpojeQueries
import cz.jaro.dpmcb.data.database.entities.Stop
import cz.jaro.dpmcb.data.database.entities.TimeCode
import cz.jaro.dpmcb.data.database.entities.Validity
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Direction
import cz.jaro.dpmcb.data.entities.Direction.Companion.invoke
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCodeType
import cz.jaro.dpmcb.data.entities.TimeCodeType.Companion.runs
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.number
import cz.jaro.dpmcb.data.entities.toLongLine
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.SuperNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.noCode
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.data.helperclasses.toDateWeirdly
import cz.jaro.dpmcb.data.helperclasses.toTimeWeirdly
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.recordException
import cz.jaro.dpmcb.data.tuples.Quadruple
import cz.jaro.dpmcb.data.tuples.Quintuple
import cz.jaro.dpmcb.ui.main.SuperRoute
import cz.jaro.dpmcb.ui.map.DiagramManager
import cz.jaro.dpmcb.ui.map.supportsLineDiagram
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.remoteconfig.remoteConfig
import dev.gitlive.firebase.storage.StorageReference
import dev.gitlive.firebase.storage.storage
import io.github.z4kn4fein.semver.toVersion
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.hours

class LoadingViewModel(
    private val repo: SpojeRepository,
    private val queries: SpojeQueries,
    private val diagramManager: DiagramManager,
    firebase: FirebaseApp,
    private val params: Parameters,
) : ViewModel() {
    data class Parameters(
        val update: Boolean,
        val link: String?,
    )

    lateinit var navigate: SuperNavigateFunction

    companion object {
        const val META_DATA_VERSION = 5
    }

    private val _state = MutableStateFlow<LoadingState>(LoadingState.Loading())
    val state = _state.asStateFlow()

    val settings = repo.settings

    @Serializable
    data class Group(
        val validity: @Serializable(with = ValiditySerializer::class) cz.jaro.dpmcb.data.entities.Validity,
        val sequences: Map<SequenceCode, List<BusName>>,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                params.update || repo.version.first() == -1
            } catch (e: Exception) {
                recordException(e)
                e.printStackTrace()
                _state.value = LoadingState.Error
                return@launch
            }

            if (params.update || repo.version.first() == -1) {
                downloadNewData() ?: return@launch
            }

            try {
                doesEverythingWork()
            } catch (e: Exception) {
                recordException(e)
                e.printStackTrace()
                _state.value = LoadingState.Error
                return@launch
            }


            if (!repo.isOnline.value || !repo.settings.value.checkForUpdates) {
                while (!::navigate.isInitialized) Unit
                withContext(Dispatchers.Main) {
                    navigate(SuperRoute.Main(params.link), popUpTo<SuperRoute.Loading>())
                }
                return@launch
            }

            _state.value = LoadingState.Loading("Kontrola dostupnosti aktualizací")

            while (!::navigate.isInitialized) Unit
            withContext(Dispatchers.Main) {
                navigate(SuperRoute.Main(params.link, isDataUpdateNeeded(), isAppDataUpdateNeeded()), popUpTo<SuperRoute.Loading>())
            }
        }
    }

    private suspend fun doesEverythingWork(): Nothing? {
        repo.lineNumbers(SystemClock.todayHere()).ifEmpty {
            throw Exception()
        }
        if (!diagramManager.checkDiagram()) {
            throw Exception()
        }
        return null
    }

    fun onEvent(e: LoadingEvent) = viewModelScope.launch {
        when (e) {
            LoadingEvent.DownloadDataIfError -> withContext(Dispatchers.Main) {
                navigate(SuperRoute.Loading(link = params.link, update = true), popUpTo<SuperRoute.Loading>())
            }
        }
    }

    private val database = Firebase.database(firebase)

    private suspend fun isDataUpdateNeeded(): Boolean {
        val localVersion = repo.version.first()

        val reference = database.reference("data${META_DATA_VERSION}/verze")

        val onlineVersion = viewModelScope.async {
            withTimeoutOrNull(3_000) {
                reference.valueEvents.first().value<Int>()
            } ?: -2
        }

        return localVersion < onlineVersion.await()
    }

    private suspend fun isAppDataUpdateNeeded(): Boolean {
        val isDebug = BuildConfig.DEBUG

        if (isDebug) return false

        val newestVersion = latestAppVersion()?.toVersion(false) ?: return false
        val localVersion = BuildConfig.VERSION_NAME.toVersion(false)

        return localVersion < newestVersion
    }

    private val client = HttpClient()

    private suspend fun downloadText(ref: StorageReference) =
        client.get(ref.getDownloadUrl()) {
            onDownload { bytesSentTotal, contentLength ->
                _state.update {
                    require(it is LoadingState.Loading)
                    it.copy(progress = bytesSentTotal.toFloat() / contentLength)
                }
            }
        }.bodyAsText()

    private suspend fun downloadNewData(): Unit? {

        if (!repo.isOnline.value) {
            _state.value = LoadingState.Offline
            return null
        }

        _state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nAnalyzování nové verze (0/?)"
        )

        val storage = Firebase.storage
        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val versionRef = database.reference("data${META_DATA_VERSION}/verze")
        val newVersion = versionRef.valueEvents.first().value<Int>()
        val currentVersion = repo.version.first()

        val changesRef = storage.reference.child("data${META_DATA_VERSION}/zmeny$currentVersion..$newVersion.json")
        val doFullUpdate = (currentVersion + 1 != newVersion) || try {
            changesRef.getDownloadUrl()
            false
        } catch (_: FirebaseException) {
            true
        }

        val connStops: MutableList<ConnStop> = mutableListOf()
        val stops: MutableList<Stop> = mutableListOf()
        val timeCodes: MutableList<TimeCode> = mutableListOf()
        val lines: MutableList<Line> = mutableListOf()
        val conns: MutableList<Conn> = mutableListOf()
        val seqOfConns: MutableList<SeqOfConn> = mutableListOf()
        val seqGroups: MutableList<SeqGroup> = mutableListOf()

        if (doFullUpdate) {
            val m = when {
                supportsLineDiagram() -> "5"
                else -> "4"
            }

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nOdstraňování starých dat (1/$m)"
            )

            queries.clearAll()
            repo.reset()

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/$m)", progress = 0F,
            )

            val sequencesRef = storage.reference.child("kurzy3.json")
            val diagramRef = storage.reference.child("schema.svg")
            val dataRef = storage.reference.child("data${META_DATA_VERSION}/data${newVersion}.json")

            val json = downloadText(dataRef)

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování kurzů (3/$m)", progress = 0F,
            )

            val json2 = downloadText(sequencesRef)

            val sequences = json2.fromJson<Map<SequenceGroup, Group>>()

            sequences.exctractSequences()
                .let { (groups, sequences) ->
                    seqGroups += groups
                    seqOfConns += sequences
                }

            seqGroups += SeqGroup(
                group = SequenceGroup.invalid,
                validFrom = noCode,
                validTo = noCode,
            )

            resetRemoteConfig()

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (4/$m)", progress = 0F,
            )

            val data: Map<Table, Map<String, List<List<String>>>> = Json.decodeFromString(json)

            data.exctractData(seqOfConns.map { it.line / it.connNumber }.distinct()) {
                seqOfConns += SeqOfConn(
                    line = it.line,
                    connNumber = it.connNumber,
                    sequence = SequenceCode.invalid,
                    orderInSequence = 0,
                    group = SequenceGroup.invalid,
                )
            }.forEach { (connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable) ->
                connStops += connStopsOfTable
                timeCodes += timeCodesOfTable
                stops += stopsOfTable
                lines += linesOfTable
                conns += connsOfTable
            }

            if (supportsLineDiagram()) {
                _state.value = LoadingState.Loading(
                    infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (5/5)",
                    progress = 0F,
                )

                diagramManager.downloadDiagram(diagramRef) { progress ->
                    _state.update {
                        require(it is LoadingState.Loading)
                        it.copy(progress = progress)
                    }
                }
            }
        } else {
            val m = when {
                supportsLineDiagram() -> "?"
                else -> "5"
            }

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování aktualizačního balíčku (1/$m)",
                progress = 0F,
            )

            repo.reset()

            val sequencesRef = storage.reference.child("kurzy3.json")
            val diagramRef = storage.reference.child("schema.svg")

            val json = downloadText(changesRef)

            val manyChanges = Json.parseToJsonElement(json).jsonObject

            val plus = manyChanges["+"]!!.jsonObject.toString().fromJson<Map<Table, Map<String, List<List<String>>>>>()
            val minus = manyChanges["-"]!!.jsonArray.toString().fromJson<List<Table>>()
            val changes = manyChanges["Δ"]!!.jsonObject.toString().fromJson<Map<Table, Map<String, List<List<String>>>>>()
            val changeDiagram = manyChanges["Δ\uD83D\uDDFA"]!!.jsonPrimitive.boolean

            val minusTables = minus
                .map { Table(LongLine(it.value.substringBefore('-').toLong() + 325_000L), it.number()) } // TODO: Nezávislost dat na předčíslí linky
            val changedTables = changes
                .map { Table(LongLine(it.key.value.substringBefore('-').toLong() + 325_000L), it.key.number()) } // TODO: Nezávislost dat na předčíslí linky

            connStops.addAll(repo.connStops())
            stops.addAll(repo.stops())
            timeCodes.addAll(repo.timeCodes())
            lines.addAll(repo.lines())
            conns.addAll(repo.conns())
            seqGroups.addAll(repo.seqGroups())
            seqOfConns.addAll(repo.seqOfConns())

            val n = when {
                changeDiagram && supportsLineDiagram() -> 6
                else -> 5
            }

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání odstraněných jízdních řádů (2/$n)",
                progress = 0F,
            )

            connStops.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { require(it is LoadingState.Loading); it.copy(progress = 1 / 5F) }
            stops.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { require(it is LoadingState.Loading); it.copy(progress = 2 / 5F) }
            timeCodes.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { require(it is LoadingState.Loading); it.copy(progress = 3 / 5F) }
            lines.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { require(it is LoadingState.Loading); it.copy(progress = 4 / 5F) }
            conns.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { require(it is LoadingState.Loading); it.copy(progress = 5 / 5F) }

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování kurzů (3/$n)", progress = 0F,
            )

            val json2 = downloadText(sequencesRef)

            val sequences = json2.fromJson<Map<SequenceGroup, Group>>()

            sequences.exctractSequences()
                .let { (groups, sequences) ->
                    seqGroups += groups
                    seqOfConns += sequences
                }

            resetRemoteConfig()

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání nových jízdních řádů (4/$n)",
                progress = 0F,
            )

            plus.exctractData(seqOfConns.map { it.line / it.connNumber }.distinct()) {
                seqOfConns += SeqOfConn(
                    line = it.line,
                    connNumber = it.connNumber,
                    sequence = SequenceCode.invalid,
                    orderInSequence = 0,
                    group = SequenceGroup.invalid,
                )
            }.forEach { (connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable) ->
                connStops += connStopsOfTable
                timeCodes += timeCodesOfTable
                stops += stopsOfTable
                lines += linesOfTable
                conns += connsOfTable
            }

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání změněných jízdních řádů (5/$n)",
                progress = 0F,
            )

            changes.exctractData(seqOfConns.map { it.line / it.connNumber }.distinct()) {
                seqOfConns += SeqOfConn(
                    line = it.line,
                    connNumber = it.connNumber,
                    sequence = SequenceCode.invalid,
                    orderInSequence = 0,
                    group = SequenceGroup.invalid,
                )
            }.forEach { (connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable) ->
                connStops += connStopsOfTable
                timeCodes += timeCodesOfTable
                stops += stopsOfTable
                lines += linesOfTable
                conns += connsOfTable
            }

            if (changeDiagram && supportsLineDiagram()) {
                _state.value = LoadingState.Loading(
                    infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (6/6)",
                    progress = 0F,
                )

                diagramManager.downloadDiagram(diagramRef) { progress ->
                    _state.update {
                        require(it is LoadingState.Loading)
                        it.copy(progress = progress)
                    }
                }
            }
        }

        _state.value = LoadingState.Loading(
            infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nUkládání",
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
            seqOfConns = seqOfConns.distinctBy { Quadruple(it.line, it.connNumber, it.sequence, it.group) },
            seqGroups = seqGroups.distinctBy { it.group },
            version = newVersion,
        )
        return Unit
    }

    private val remoteConfig = Firebase.remoteConfig(firebase)

    private suspend fun resetRemoteConfig() {
        remoteConfig.reset()
        remoteConfig.settings {
            minimumFetchInterval = 1.hours
        }
        remoteConfig.fetchAndActivate()
    }

    private inline fun Map<Table, Map<String, List<List<String>>>>.exctractData(
        connsWithSequence: List<BusName>,
        addConnToSequence: (conn: Conn) -> Unit,
    ): List<Quintuple<MutableList<ConnStop>, MutableList<TimeCode>, MutableList<Stop>, MutableList<Line>, MutableList<Conn>>> {

        val rowsCount = this
            .toList()
            .flatMap { it0 ->
                it0.second.flatMap {
                    it.value
                }
            }
            .count()

        var rowindex = 0F

        return this
            .mapKeys { Table(LongLine(it.key.value.substringBefore('-').toLong() + 325_000L), it.key.number()) } // TODO: Nezávislost dat na předčíslí linky
            .map { (tab, dataLinky) ->
                val connStopsOfTable: MutableList<ConnStop> = mutableListOf()
                val timeCodesOfTable: MutableList<TimeCode> = mutableListOf()
                val stopsOfTable: MutableList<Stop> = mutableListOf()
                val linesOfTable: MutableList<Line> = mutableListOf()
                val connsOfTable: MutableList<Conn> = mutableListOf()
                val fixedCodesOfTable: MutableMap<String, String> = mutableMapOf()

                dataLinky
                    .toList()
                    .sortedBy { (tableType, _) ->
                        TableType.entries.indexOf(TableType.valueOf(tableType))
                    }
                    .forEach { (typTabulky, table) ->
                        table.forEach radek@{ row ->
                            rowindex++

                            _state.update {
                                require(it is LoadingState.Loading)
                                it.copy(progress = rowindex / rowsCount)
                            }

                            when (TableType.valueOf(typTabulky)) {
                                TableType.Zasspoje -> connStopsOfTable += ConnStop(
                                    line = row[0].toLongLine(),
                                    connNumber = row[1].toInt(),
                                    stopIndexOnLine = row[2].toInt(),
                                    stopNumber = row[3].toInt(),
                                    kmFromStart = row[9].ifEmpty { null }?.toLong() ?: return@radek,
                                    arrival = row[10].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toTimeWeirdly(),
                                    departure = row[11].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toTimeWeirdly(),
                                    tab = tab,
                                    fixedCodes = row.slice(6..7).filter { it.isNotEmpty() }.joinToString(" ") {
                                        fixedCodesOfTable[it] ?: it
                                    },
                                )

                                TableType.Zastavky -> stopsOfTable += Stop(
                                    line = row[0].toLongLine(),
                                    stopNumber = row[1].toInt(),
                                    stopName = row[2],
                                    fixedCodes = row.slice(6..11).filter { it.isNotEmpty() }.joinToString(" ") {
                                        fixedCodesOfTable[it] ?: it
                                    },
                                    tab = tab,
                                )

                                TableType.Caskody -> {
                                    val type = TimeCodeType.entries.find { it.code.toString() == row[4] } ?: TimeCodeType.DoesNotRun
                                    timeCodesOfTable += TimeCode(
                                        line = row[0].toLongLine(),
                                        connNumber = row[1].toInt(),
                                        code = row[3].toLong(),
                                        termIndex = row[2].toLong(),
                                        type = type,
                                        validFrom = row[5].toDateWeirdly(),
                                        validTo = row[6].ifEmpty { row[5] }.toDateWeirdly(),
                                        tab = tab,
                                        runs2 = type.runs,
                                    )
                                }

                                TableType.Linky -> linesOfTable += Line(
                                    number = row[0].toLongLine(),
                                    route = row[1],
                                    vehicleType = Json.decodeFromString("\"${row[4]}\""),
                                    lineType = Json.decodeFromString("\"${row[3]}\""),
                                    hasRestriction = row[5] != "0",
                                    validFrom = row[13].toDateWeirdly(),
                                    validTo = row[14].toDateWeirdly(),
                                    tab = tab,
                                    shortNumber = row[0].toLongLine().toShortLine(),
                                )

                                TableType.Spoje -> {
                                    connsOfTable += Conn(
                                        line = row[0].toLongLine(),
                                        connNumber = row[1].toInt(),
                                        fixedCodes = row.slice(2..11).filter { it.isNotEmpty() }.joinToString(" ") {
                                            fixedCodesOfTable[it] ?: it
                                        },
                                        direction = connStopsOfTable
                                            .filter { it.connNumber == row[1].toInt() }
                                            .sortedBy { it.stopIndexOnLine }
                                            .filter { it.time != null }
                                            .let { stops ->
                                                Direction(stops.first().time!! <= stops.last().time!! && stops.first().kmFromStart <= stops.last().kmFromStart)
                                            },
                                        tab = tab,
                                        name = row[0].toLongLine() / row[1].toInt(),
                                    ).also { conn ->
                                        val type =
                                            if (timeCodesOfTable.any { it.type != TimeCodeType.DoesNotRun && it.connNumber == conn.connNumber }) TimeCodeType.Runs else TimeCodeType.DoesNotRun
                                        timeCodesOfTable += TimeCode(
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
                                        if (conn.name !in connsWithSequence) addConnToSequence(conn)
                                    }
                                }

                                TableType.Pevnykod -> {
                                    fixedCodesOfTable += row[0] to row[1]
                                }

                                TableType.Zaslinky -> Unit
                                TableType.VerzeJDF -> Unit
                                TableType.Dopravci -> Unit
                                TableType.LinExt -> Unit
                                TableType.Udaje -> Unit
                            }
                        }
                    }
                Quintuple(connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable)
            }
    }

    private inline fun <reified T> String.fromJson(): T = Json.decodeFromString<T>(this)
}

object ValiditySerializer : KSerializer<Validity> {
    private val serializer = serializer<Pair<LocalDate, LocalDate>>()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("Validity", serializer.descriptor)

    override fun serialize(encoder: Encoder, value: Validity) {
        encoder.encodeSerializableValue(serializer, value.validFrom to value.validTo)
    }

    override fun deserialize(decoder: Decoder): Validity {
        val pair = decoder.decodeSerializableValue(serializer)
        return Validity(
            pair.first,
            pair.second,
        )
    }
}

private val ConnStop.time get() = departure ?: arrival

private fun Map<SequenceGroup, LoadingViewModel.Group>.exctractSequences(): Pair<List<SeqGroup>, List<SeqOfConn>> =
    map { (group, groupData) ->
        SeqGroup(
            group = group,
            validFrom = groupData.validity.validFrom,
            validTo = groupData.validity.validTo,
        ) to groupData.sequences.flatMap { (sequenceCode, buses) ->
            buses.mapIndexed { i, bus ->
                SeqOfConn(
                    line = bus.line(),
                    connNumber = bus.bus(),
                    sequence = sequenceCode,
                    orderInSequence = i.toLong(),
                    group = group,
                )
            }
        }
    }.let { list ->
        list.map { it.first } to list.flatMap { it.second }
    }
