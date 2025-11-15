package cz.jaro.dpmcb.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import com.russhwolf.settings.set
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceModifiers
import cz.jaro.dpmcb.data.entities.generic
import cz.jaro.dpmcb.data.entities.hasPart
import cz.jaro.dpmcb.data.entities.hasType
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.modifiers
import cz.jaro.dpmcb.data.entities.part
import cz.jaro.dpmcb.data.entities.sequenceNumber
import cz.jaro.dpmcb.data.entities.typeChar
import cz.jaro.dpmcb.data.entities.types.VehicleType
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.MutateLambda
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.helperclasses.durationUntil
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.toJson
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.unaryPlus
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

interface LocalSettingsDataSource {
    val settings: StateFlow<Settings>
    fun changeSettings(update: MutateLambda<Settings>)

    val loadedData: StateFlow<DownloadedData>
    fun changeLoadedData(update: MutateLambda<DownloadedData>)

    val favourites: StateFlow<List<PartOfConn>>
    fun changeFavourites(update: MutateLambda<List<PartOfConn>>)

    val recents: StateFlow<List<BusName>>
    fun changeRecents(update: MutateLambda<List<BusName>>)

    val hasCard: StateFlow<Boolean>
    fun changeCard(value: Boolean)

    val vehicleNumbersOnSequences: StateFlow<Map<LocalDate, Map<SequenceCode, RegistrationNumber>>>
    fun changeVehicleNumbersOnSequences(update: MutateLambda<Map<LocalDate, Map<SequenceCode, RegistrationNumber>>>)
}

fun LocalSettingsDataSource.changeFavourite(part: PartOfConn) {
    changeFavourites { favourites ->
        (listOf(part) + favourites).distinctBy { it.busName }
    }
}

fun LocalSettingsDataSource.removeFavourite(name: BusName) {
    changeFavourites { favourites ->
        favourites - favourites.first { it.busName == name }
    }
}

fun LocalSettingsDataSource.pushRecentBus(bus: BusName) {
    changeRecents { recents ->
        (listOf(bus) + recents).distinct().take(settings.value.recentBusesCount)
    }
}

@OptIn(ExperimentalTime::class)
fun LocalSettingsDataSource.pushVehicles(date: LocalDate, vehicles: Map<SequenceCode, RegistrationNumber>, reliable: Boolean = true) {
    changeVehicleNumbersOnSequences { current ->
        current.toMutableMap().also {
            if (reliable)
                it[date] = it.getOrElse(date) { mapOf() } + vehicles
            else
                it[date] = vehicles + it.getOrElse(date) { mapOf() }
        }.filterKeys { date -> date.durationUntil(SystemClock.todayHere()) <= 7.days }
    }
}

fun LocalSettingsDataSource.pushVehicle(date: LocalDate, sequence: SequenceCode, vehicle: RegistrationNumber, reliable: Boolean = true) =
    pushVehicles(date, mapOf(sequence to vehicle))

val LocalSettingsDataSource.version get() = loadedData.value.version
val LocalSettingsDataSource.dividedSequencesWithMultipleBuses get() = loadedData.value.dividedSequencesWithMultipleBuses
val LocalSettingsDataSource.linesTraction get() = loadedData.value.linesTraction
val LocalSettingsDataSource.sequenceConnections get() = loadedData.value.sequenceConnections
val LocalSettingsDataSource.sequenceTypes get() = loadedData.value.sequenceTypes

context(m: LocalSettingsDataSource) fun SequenceModifiers.type() = typeChar()?.let { m.sequenceTypes[it] }

fun LocalSettingsDataSource.lineTraction(line: LongLine, type: VehicleType) =
    linesTraction.entries.find { (_, lines) -> line in lines }?.key
        ?: when (type) {
            VehicleType.TROLEJBUS -> Traction.Trolleybus
            VehicleType.AUTOBUS -> Traction.Diesel
            else -> Traction.Other
        }

fun LocalSettingsDataSource.getSequenceComparator(): Comparator<SequenceCode> {
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

context(m: LocalSettingsDataSource) fun SequenceCode.seqName() = let {
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

context(m: LocalSettingsDataSource) fun SequenceCode.seqConnection() = "Potenciální návaznost na " + let {
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

@OptIn(ExperimentalSettingsApi::class)
class MultiplatformSettingsDataSource(
    private val data: ObservableSettings,
) : LocalSettingsDataSource {
    private val scope = CoroutineScope(Dispatchers.IO)

    object Keys {
        const val DATA = "data"
        const val FAVOURITES = "oblibene_useky"
        const val RECENTS = "recents"

        //        const val DEPARTURES = "odjezdy"
        const val SETTINGS = "nastaveni"
        const val CARD = "prukazka"
        const val VEHICLES = "vehiclesOnSequences"
    }

    object DefaultValues {
        val DATA = DownloadedData()
        val FAVOURITES = listOf<PartOfConn>()
        val RECENTS = listOf<BusName>()

        //        const val DEPARTURES = false
        val SETTINGS = Settings()
        const val CARD = false
        val VEHICLES = emptyMap<LocalDate, Map<SequenceCode, RegistrationNumber>>()
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override val settings = data
        .getStringOrNullStateFlow(scope, Keys.SETTINGS)
        .mapState(scope) {
            it?.fromJson<Settings>(json) ?: DefaultValues.SETTINGS
        }

    override fun changeSettings(update: (Settings) -> Settings) {
        data[Keys.SETTINGS] = update(settings.value).toJson(json)
    }

    override val loadedData = data
        .getStringOrNullStateFlow(scope, Keys.DATA)
        .mapState(scope) {
            it?.fromJson<DownloadedData>(json) ?: DefaultValues.DATA
        }

    override fun changeLoadedData(update: MutateLambda<DownloadedData>) {
        data[Keys.DATA] = update(loadedData.value).toJson(json)
    }

    override val favourites = data
        .getStringOrNullStateFlow(scope, Keys.FAVOURITES)
        .mapState(scope) {
            it?.fromJson<List<PartOfConn>>(json) ?: DefaultValues.FAVOURITES
        }

    override fun changeFavourites(update: (List<PartOfConn>) -> List<PartOfConn>) {
        data[Keys.FAVOURITES] = update(favourites.value).toJson(json)
    }

    override val recents = data
        .getStringOrNullStateFlow(scope, Keys.RECENTS)
        .mapState(scope) {
            it?.fromJson(json) ?: DefaultValues.RECENTS
        }

    override fun changeRecents(update: (List<BusName>) -> List<BusName>) {
        data[Keys.RECENTS] = update(recents.value).toJson(json)
    }

//    override val departures = data
//        .getBooleanOrNullStateFlow(scope, Keys.DEPARTURES)
//        .mapState(scope) {
//            it ?: DefaultValues.DEPARTURES
//        }
//
//    override fun changeDepartures(value: Boolean) {
//        data[Keys.DEPARTURES] = value
//    }

    override val hasCard = data
        .getBooleanOrNullStateFlow(scope, Keys.CARD)
        .mapState(scope) {
            it ?: DefaultValues.CARD
        }

    override fun changeCard(value: Boolean) {
        data[Keys.CARD] = value
    }

    override val vehicleNumbersOnSequences = data
        .getStringOrNullStateFlow(scope, Keys.VEHICLES)
        .mapState(scope) {
            it?.fromJson(json) ?: DefaultValues.VEHICLES
        }

    override fun changeVehicleNumbersOnSequences(update: MutateLambda<Map<LocalDate, Map<SequenceCode, RegistrationNumber>>>) {
        data[Keys.VEHICLES] = update(vehicleNumbersOnSequences.value).toJson(json)
    }
}
