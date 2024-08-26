package cz.jaro.dpmcb.ui.loading

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.number
import cz.jaro.dpmcb.data.entities.toLongLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.invoke
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.noCode
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDateWeirdly
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toTimeWeirdly
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.tuples.Quadruple
import cz.jaro.dpmcb.data.tuples.Quintuple
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.io.File
import java.io.IOException

@KoinViewModel
class LoadingViewModel(
    private val repo: SpojeRepository,
    private val db: AppDatabase,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val uri: String?,
        val update: Boolean,
        val error: (() -> Unit) -> Unit,
        val internetNeeded: () -> Unit,
        val finish: () -> Unit,
        val diagramFile: File,
        val dataFile: File,
        val sequencesFile: File,
        val mainActivityIntent: Intent,
        val loadingActivityIntent: Intent,
        val startActivity: (Intent) -> Unit,
        val packageName: String,
        val exit: () -> Nothing,
    )

    companion object {
        const val META_DATA_VERSION = 5
        const val EXTRA_KEY_UPDATE_DATA = "aktualizovat-data"
        const val EXTRA_KEY_UPDATE_APP = "aktualizovat-aplikaci"
        const val EXTRA_KEY_DEEPLINK = "link"
    }

    private val _state = MutableStateFlow("" to (null as Float?))
    val state = _state.asStateFlow()

    val settings = repo.settings

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                params.update || repo.version.first() == -1
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                e.printStackTrace()
                showErrorDialog()
            }

            if (params.update || repo.version.first() == -1) {
                downloadNewData()
            }

            try {
                doesEverythingWork()
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                e.printStackTrace()
                showErrorDialog()
            }

            val intent = resolveLink(params.mainActivityIntent)

            if (!repo.isOnline.value || !repo.settings.value.checkForUpdates) {
                params.finish()
                params.startActivity(intent)
                return@launch
            }

            _state.update {
                "Kontrola dostupnosti aktualizací" to null
            }

            intent.putExtra(EXTRA_KEY_UPDATE_DATA, isDataUpdateNeeded())
            intent.putExtra(EXTRA_KEY_UPDATE_APP, isAppDataUpdateNeeded())

            params.finish()
            params.startActivity(intent)
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

    private suspend fun showErrorDialog(): Nothing {
        coroutineScope {
            withContext(Dispatchers.Main) {
                params.error {
                    params.startActivity(params.loadingActivityIntent.apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                        putExtra("update", true)
                    })
                    params.finish()
                }
            }
        }
        println() // !!! DO NOT REMOVE !!! DOESN'T WORK WITHOUT IT !!! *** !!! NEODSTRAŇOVAT !!! BEZ TOHO TO NEFUNGUJE !!!
        while (true) Unit
    }

    private fun resolveLink(baseIntent: Intent): Intent {
        params.uri?.let {

            val link = it.removePrefix("/DPMCB")

            if (link == "/app-details") openAppDetails()

            baseIntent.putExtra(EXTRA_KEY_DEEPLINK, link)
        }

        return baseIntent
    }

    private fun openAppDetails(): Nothing {
        params.finish()
        params.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", params.packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        })
        params.exit()
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

        val response = try {
            withContext(Dispatchers.IO) {
                Jsoup
                    .connect("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/app/version.txt")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .execute()
            }
        } catch (e: IOException) {
            Firebase.crashlytics.recordException(e)
            return false
        }

        if (response.statusCode() != 200) return false

        val localVersion = BuildConfig.VERSION_NAME.toVersion(false)
        val newestVersion = response.body().toVersion(false)

        return localVersion < newestVersion
    }

    private suspend fun downloadFile(ref: StorageReference, file: File): File {

        val task = ref.getFile(file)

        task.addOnFailureListener {
            throw it
        }

        task.addOnProgressListener { snapshot ->
            _state.update {
                it.first to snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount
            }
        }

        task.await()

        return file
    }

    private suspend fun downloadNewData() {

        if (!repo.isOnline.value) {
            withContext(Dispatchers.Main) {
                params.internetNeeded()
            }
            params.exit()
        }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nAnalyzování nové verze (0/?)" to null
        }

        val storage = Firebase.storage
        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val versionRef = database.getReference("data${META_DATA_VERSION}/verze")
        val newVersion = versionRef.get().await().getValue(TI) ?: -1
        val currentVersion = repo.version.first()

        val changesRef = storage.reference.child("data${META_DATA_VERSION}/zmeny$currentVersion..$newVersion.json")
        val doFullUpdate = (currentVersion + 1 != newVersion) || try {
            changesRef.downloadUrl.await()
            false
        } catch (e: FirebaseException) {
            true
        }

        val connStops: MutableList<ConnStop> = mutableListOf()
        val stops: MutableList<Stop> = mutableListOf()
        val timeCodes: MutableList<TimeCode> = mutableListOf()
        val lines: MutableList<Line> = mutableListOf()
        val conns: MutableList<Conn> = mutableListOf()

        if (doFullUpdate) {

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nOdstraňování starých dat (1/5)" to null
            }

            db.clearAllTables()
            repo.reset()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/5)" to 0F
            }

            val sequencesRef = storage.reference.child("kurzy2.json")
            val diagramRef = storage.reference.child("schema.svg")
            val dataRef = storage.reference.child("data${META_DATA_VERSION}/data${newVersion}.json")

            val dataFile = params.dataFile

            val json = downloadFile(
                ref = dataRef,
                file = dataFile,
            ).readText()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování kurzů (3/5)" to 0F
            }

            val sequencesFile = params.sequencesFile

            val json2 = downloadFile(
                ref = sequencesRef,
                file = sequencesFile,
            ).readText()

            val sequences = json2.fromJson<Map<SequenceCode, List<BusName>>>()

            Firebase.remoteConfig.reset().await()
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            Firebase.remoteConfig.setConfigSettingsAsync(configSettings)
            Firebase.remoteConfig.fetchAndActivate().await()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (4/5)" to 0F
            }

            val data: Map<Table, Map<String, List<List<String>>>> = Json.decodeFromString(json)

            data.exctractData(sequences)
                .forEach { (connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable) ->
                    connStops += connStopsOfTable
                    timeCodes += timeCodesOfTable
                    stops += stopsOfTable
                    lines += linesOfTable
                    conns += connsOfTable
                }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (5/5)" to 0F
            }

            downloadDiagram(diagramRef)
        } else {
            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování aktualizačního balíčku (1/?)" to 0F
            }

            repo.reset()

            val sequencesRef = storage.reference.child("kurzy.json")
            val diagramRef = storage.reference.child("schema.svg")

            val dataFile = params.dataFile

            val json = downloadFile(
                ref = changesRef,
                file = dataFile,
            ).readText()

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

            val n = when {
                changeDiagram -> 6
                else -> 5
            }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání odstraněných jízdních řádů (2/$n)" to 0F
            }

            connStops.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { it.first to 1 / 5F }
            stops.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { it.first to 2 / 5F }
            timeCodes.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { it.first to 3 / 5F }
            lines.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { it.first to 4 / 5F }
            conns.removeAll { it.tab in minusTables || it.tab in changedTables }
            _state.update { it.first to 5 / 5F }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování kurzů (3/$n)" to 0F
            }

            val sequencesFile = params.sequencesFile

            val json2 = downloadFile(
                ref = sequencesRef,
                file = sequencesFile,
            ).readText()

            val sequences = json2.fromJson<Map<SequenceCode, List<BusName>>>()

            Firebase.remoteConfig.reset().await()
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            Firebase.remoteConfig.setConfigSettingsAsync(configSettings)
            Firebase.remoteConfig.fetchAndActivate().await()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání nových jízdních řádů (4/$n)" to 0F
            }

            plus.exctractData(sequences)
                .forEach { (connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable) ->
                    connStops += connStopsOfTable
                    timeCodes += timeCodesOfTable
                    stops += stopsOfTable
                    lines += linesOfTable
                    conns += connsOfTable
                }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání změněných jízdních řádů (5/$n)" to 0F
            }

            changes.exctractData(sequences)
                .forEach { (connStopsOfTable, timeCodesOfTable, stopsOfTable, linesOfTable, connsOfTable) ->
                    connStops += connStopsOfTable
                    timeCodes += timeCodesOfTable
                    stops += stopsOfTable
                    lines += linesOfTable
                    conns += connsOfTable
                }

            if (changeDiagram) {
                _state.update {
                    "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (6/6)" to 0F
                }

                downloadDiagram(diagramRef)
            }
        }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nUkládání" to null
        }

        println(conns)
        println(lines)
        println(stops)
        println(connStops)
        println(timeCodes)

        repo.write(
            connStops = connStops.distinctBy { Triple(it.tab, it.connNumber, it.stopIndexOnLine) }.toTypedArray(),
            stops = stops.distinctBy { it.tab to it.stopNumber }.toTypedArray(),
            timeCodes = timeCodes.distinctBy { Quadruple(it.tab, it.code, it.connNumber, it.termIndex) }.toTypedArray(),
            lines = lines.distinctBy { it.tab }.toTypedArray(),
            conns = conns.distinctBy { it.tab to it.connNumber }.toTypedArray(),
            version = newVersion,
        )
    }

    private suspend fun downloadDiagram(schemaRef: StorageReference) = downloadFile(schemaRef, params.diagramFile)

    private fun Map<Table, Map<String, List<List<String>>>>.exctractData(
        sequences: Map<SequenceCode, List<BusName>>,
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
                                it.first to rowindex / rowsCount
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
                                    validFrom = row[5].toDateWeirdly(),
                                    validTo = row[6].ifEmpty { row[5] }.toDateWeirdly(),
                                    tab = tab,
                                )

                                TableType.Linky -> linesOfTable += Line(
                                    number = row[0].toLongLine(),
                                    route = row[1],
                                    vehicleType = Json.decodeFromString("\"${row[4]}\""),
                                    lineType = Json.decodeFromString("\"${row[3]}\""),
                                    hasRestriction = row[5] != "0",
                                    validFrom = row[13].toDateWeirdly(),
                                    validTo = row[14].toDateWeirdly(),
                                    tab = tab,
                                )

                                TableType.Spoje -> {
                                    val seq = sequences.toList().firstOrNull { (_, spoje) -> row[0] / row[1] in spoje }

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
                                        sequence = seq?.first,
                                        orderInSequence = seq?.second?.indexOf(row[0] / row[1])?.takeUnless { it == -1 },
                                    ).also { conn ->
                                        timeCodesOfTable += TimeCode(
                                            line = conn.line,
                                            connNumber = conn.connNumber,
                                            code = -1,
                                            termIndex = 0,
                                            type = if (timeCodesOfTable.any { it.type != TimeCodeType.DoesNotRun && it.connNumber == conn.connNumber }) TimeCodeType.Runs else TimeCodeType.DoesNotRun,
                                            validFrom = noCode,
                                            validTo = noCode,
                                            tab = conn.tab,
                                        )
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