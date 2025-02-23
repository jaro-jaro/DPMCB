package cz.jaro.dpmcb.ui.loading

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.database
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SeqGroup
import cz.jaro.dpmcb.data.entities.SeqOfConn
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.entities.Validity
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.number
import cz.jaro.dpmcb.data.entities.toLongLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.Direction.Companion.invoke
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.helperclasses.SuperNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.noCode
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.data.helperclasses.toDateWeirdly
import cz.jaro.dpmcb.data.helperclasses.toTimeWeirdly
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.tuples.Quadruple
import cz.jaro.dpmcb.data.tuples.Quintuple
import cz.jaro.dpmcb.ui.main.SuperRoute
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class LoadingViewModel(
    private val repo: SpojeRepository,
    private val db: AppDatabase,
    private val params: Parameters,
) : ViewModel() {
    data class Parameters(
        val update: Boolean,
        val diagramFile: File,
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
        val validity: Validity,
        val sequences: Map<SequenceCode, List<BusName>>,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                params.update || repo.version.first() == -1
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
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
                Firebase.crashlytics.recordException(e)
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
        if (!params.diagramFile.exists()) {
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

    @Keep
    object TI : GenericTypeIndicator<Int>()

    private suspend fun isDataUpdateNeeded(): Boolean {
        val localVersion = repo.version.first()

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val reference = database.getReference("data${META_DATA_VERSION}/verze")

        val onlineVersion = viewModelScope.async {
            withTimeoutOrNull(3_000) {
                reference.get().await().getValue(TI)
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

    private suspend fun File.downloadFile(ref: StorageReference) {

        val task = ref.getFile(this)

        task.addOnFailureListener {
            throw it
        }

        task.addOnProgressListener { snapshot ->
            _state.update {
                require(it is LoadingState.Loading)
                it.copy(progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount)
            }
        }

        task.await()
    }

    private suspend fun downloadText(ref: StorageReference): String {

        var result = ""

        val task = ref.getStream { _, input ->
            result += input.readBytes().decodeToString()
        }

        task.addOnFailureListener {
            throw it
        }
        task.addOnProgressListener { snapshot ->
            _state.update {
                require(it is LoadingState.Loading)
                it.copy(progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount)
            }
        }

        task.await()

        return result
    }

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
        val versionRef = database.getReference("data${META_DATA_VERSION}/verze")
        val newVersion = versionRef.get().await().getValue(TI) ?: -1
        val currentVersion = repo.version.first()

        val changesRef = storage.reference.child("data${META_DATA_VERSION}/zmeny$currentVersion..$newVersion.json")
        val doFullUpdate = (currentVersion + 1 != newVersion) || try {
            changesRef.downloadUrl.await()
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

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nOdstraňování starých dat (1/5)"
            )

            db.clearAllTables()
            repo.reset()

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/5)", progress = 0F,
            )

            val sequencesRef = storage.reference.child("kurzy3.json")
            val diagramRef = storage.reference.child("schema.svg")
            val dataRef = storage.reference.child("data${META_DATA_VERSION}/data${newVersion}.json")

            val json = downloadText(dataRef)

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování kurzů (3/5)", progress = 0F,
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
                validity = Validity(noCode, noCode),
            )

            resetRemoteConfig()

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (4/5)", progress = 0F,
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

            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (5/5)",
                progress = 0F,
            )

            downloadDiagram(diagramRef)
        } else {
            _state.value = LoadingState.Loading(
                infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování aktualizačního balíčku (1/?)",
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
                .map { Table(LongLine(it.value.substringBefore('-').toInt() + 325_000), it.number()) } // TODO: Nezávislost dat na předčíslí linky
            val changedTables = changes
                .map { Table(LongLine(it.key.value.substringBefore('-').toInt() + 325_000), it.key.number()) } // TODO: Nezávislost dat na předčíslí linky

            connStops.addAll(repo.connStops())
            stops.addAll(repo.stops())
            timeCodes.addAll(repo.timeCodes())
            lines.addAll(repo.lines())
            conns.addAll(repo.conns())
            seqGroups.addAll(repo.seqGroups())
            seqOfConns.addAll(repo.seqOfConns())

            val n = when {
                changeDiagram -> 6
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

            if (changeDiagram) {
                _state.value = LoadingState.Loading(
                    infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (6/6)",
                    progress = 0F,
                )

                downloadDiagram(diagramRef)
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
            connStops = connStops.distinctBy { Triple(it.tab, it.connNumber, it.stopIndexOnLine) }.toTypedArray(),
            stops = stops.distinctBy { it.tab to it.stopNumber }.toTypedArray(),
            timeCodes = timeCodes.distinctBy { Quadruple(it.tab, it.code, it.connNumber, it.termIndex) }.toTypedArray(),
            lines = lines.distinctBy { it.tab }.toTypedArray(),
            conns = conns.distinctBy { it.tab to it.connNumber }.toTypedArray(),
            seqOfConns = seqOfConns.distinctBy { Quadruple(it.line, it.connNumber, it.sequence, it.group) }.toTypedArray(),
            seqGroups = seqGroups.distinctBy { it.group }.toTypedArray(),
            version = newVersion,
        )
        return Unit
    }

    private suspend fun resetRemoteConfig() {
        Firebase.remoteConfig.reset().await()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        Firebase.remoteConfig.setConfigSettingsAsync(configSettings)
        Firebase.remoteConfig.fetchAndActivate().await()
    }

    private suspend fun downloadDiagram(schemaRef: StorageReference) = params.diagramFile.downloadFile(schemaRef)

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
            .mapKeys { Table(LongLine(it.key.value.substringBefore('-').toInt() + 325_000), it.key.number()) } // TODO: Nezávislost dat na předčíslí linky
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
                                    kmFromStart = row[9].ifEmpty { null }?.toInt() ?: return@radek,
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

                                TableType.Caskody -> timeCodesOfTable += TimeCode(
                                    line = row[0].toLongLine(),
                                    connNumber = row[1].toInt(),
                                    code = row[3].toInt(),
                                    termIndex = row[2].toInt(),
                                    type = TimeCodeType.entries.find { it.code.toString() == row[4] } ?: TimeCodeType.DoesNotRun,
                                    validity = Validity(
                                        validFrom = row[5].toDateWeirdly(),
                                        validTo = row[6].ifEmpty { row[5] }.toDateWeirdly(),
                                    ),
                                    tab = tab,
                                )

                                TableType.Linky -> linesOfTable += Line(
                                    number = row[0].toLongLine(),
                                    route = row[1],
                                    vehicleType = Json.decodeFromString("\"${row[4]}\""),
                                    lineType = Json.decodeFromString("\"${row[3]}\""),
                                    hasRestriction = row[5] != "0",
                                    validity = Validity(
                                        validFrom = row[13].toDateWeirdly(),
                                        validTo = row[14].toDateWeirdly(),
                                    ),
                                    tab = tab,
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
                                    ).also { conn ->
                                        timeCodesOfTable += TimeCode(
                                            line = conn.line,
                                            connNumber = conn.connNumber,
                                            code = -1,
                                            termIndex = 0,
                                            type = if (timeCodesOfTable.any { it.type != TimeCodeType.DoesNotRun && it.connNumber == conn.connNumber }) TimeCodeType.Runs else TimeCodeType.DoesNotRun,
                                            validity = Validity(noCode, noCode),
                                            tab = conn.tab,
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

private fun Map<SequenceGroup, LoadingViewModel.Group>.exctractSequences(): Pair<List<SeqGroup>, List<SeqOfConn>> =
    map { (group, groupData) ->
        SeqGroup(
            group = group,
            validity = groupData.validity,
        ) to groupData.sequences.flatMap { (sequenceCode, buses) ->
            buses.mapIndexed { i, bus ->
                SeqOfConn(
                    line = bus.line(),
                    connNumber = bus.bus(),
                    sequence = sequenceCode,
                    orderInSequence = i,
                    group = group,
                )
            }
        }
    }.let { list ->
        list.map { it.first } to list.flatMap { it.second }
    }
