package cz.jaro.dpmcb.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullStateFlow
import com.russhwolf.settings.coroutines.getIntOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import com.russhwolf.settings.set
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.MutateLambda
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.durationUntil
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.toJson
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.connection_search.SearchSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

interface LocalSettingsDataSource {
    val settings: StateFlow<Settings>
    fun changeSettings(update: MutateLambda<Settings>)

    val version: StateFlow<Int>
    fun changeVersion(value: Int)

    val searchHistory: StateFlow<List<SearchSettings>>
    fun changeSearchHistory(update: MutateLambda<List<SearchSettings>>)

    val hasCard: StateFlow<Boolean>
    fun changeCard(value: Boolean)

    val vehicleNumbersOnSequences: StateFlow<Map<LocalDate, Map<SequenceCode, RegistrationNumber>>>
    fun changeVehicleNumbersOnSequences(update: MutateLambda<Map<LocalDate, Map<SequenceCode, RegistrationNumber>>>)
}

fun LocalSettingsDataSource.pushSearchToHistory(settings: SearchSettings) {
    changeSearchHistory { history ->
        (listOf(settings) + history).distinctBy { it.key }
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
    pushVehicles(date, mapOf(sequence to vehicle), reliable)

@OptIn(ExperimentalSettingsApi::class)
class MultiplatformSettingsDataSource(
    private val data: ObservableSettings,
) : LocalSettingsDataSource {
    private val scope = CoroutineScope(Dispatchers.IO)

    object Keys {
        const val VERSION = "verze"
        const val SEARCH_HISTORY = "search_history"
        const val SETTINGS = "nastaveni"
        const val CARD = "prukazka"
        const val VEHICLES = "vehiclesOnSequences"
    }

    object DefaultValues {
        const val VERSION = -1
        @OptIn(ExperimentalTime::class)
        val SEARCH_HISTORY = listOf(
            SearchSettings(
                start = "Pětidomí",
                destination = "Na Sádkách",
                directOnly = false,
                showInefficientConnections = true,
                datetime = SystemClock.todayHere().atTime(6, 30),
            ),
            SearchSettings(
                start = "Alešova",
                destination = "Aloise Kříže",
                directOnly = false,
                showInefficientConnections = true,
                datetime = SystemClock.todayHere().atTime(16, 10),
            ),
            SearchSettings(
                start = "Suché Vrbné",
                destination = "Jana Buděšínského",
                directOnly = false,
                showInefficientConnections = true,
                datetime = SystemClock.todayHere().atTime(16, 10),
            )
        )

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

    override val version = data
        .getIntOrNullStateFlow(scope, Keys.VERSION)
        .mapState(scope) {
            it ?: DefaultValues.VERSION
        }

    override fun changeVersion(value: Int) {
        data[Keys.VERSION] = value
    }

    override val searchHistory = data
        .getStringOrNullStateFlow(scope, Keys.SEARCH_HISTORY)
        .mapState(scope) {
            it?.fromJson(json) ?: DefaultValues.SEARCH_HISTORY
        }

    override fun changeSearchHistory(update: (List<SearchSettings>) -> List<SearchSettings>) {
        data[Keys.SEARCH_HISTORY] = update(searchHistory.value).toJson(json)
    }

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
