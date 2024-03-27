package cz.jaro.dpmcb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cz.jaro.dpmcb.data.helperclasses.PartOfConn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class PreferenceDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    object PreferenceKeys {
        val VERSION = intPreferencesKey("verze")
        val FAVOURITES = stringPreferencesKey("oblibene_useky")
        val LOW_FLOOR = booleanPreferencesKey("nizkopodlaznost")
        val DEPARTURES = booleanPreferencesKey("odjezdy")
        val SETTINGS = stringPreferencesKey("nastaveni")
        val CARD = booleanPreferencesKey("prukazka")
    }

    object DefaultValues {
        const val VERSION = -1
        val FAVOURITES = listOf<PartOfConn>()
        const val LOW_FLOOR = false
        const val DEPARTURES = false
        val SETTINGS = Settings()
        const val CARD = false
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val settings = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SETTINGS]?.let { json.decodeFromString<Settings>(it) } ?: DefaultValues.SETTINGS
    }.stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), DefaultValues.SETTINGS)

    suspend fun changeSettings(update: (Settings) -> Settings) {
        dataStore.edit { preferences ->
            val lastValue = preferences[PreferenceKeys.SETTINGS]?.let { json.decodeFromString(it) } ?: DefaultValues.SETTINGS
            preferences[PreferenceKeys.SETTINGS] = Json.encodeToString(update(lastValue))
        }
    }

    val version = dataStore.data.map {
        it[PreferenceKeys.VERSION] ?: DefaultValues.VERSION
    }

//    val SpojverzeKurzu = dataStore.data.map {
//        it[PreferenceKeys.VERZE_KURZU] ?: DefaultValues.VERZE_KURZU
//    }

    suspend fun changeVersion(value: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.VERSION] = value
        }
    }

//    suspend fun zmenitVerziKurzu(value: Int) {
//        dataStore.edit { preferences ->
//            preferences[PreferenceKeys.VERZE_KURZU] = value
//        }
//    }

    val favourites = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.FAVOURITES]?.let { Json.decodeFromString<List<PartOfConn>>(it) } ?: DefaultValues.FAVOURITES
    }

    suspend fun changeFavourites(update: (List<PartOfConn>) -> List<PartOfConn>) {
        dataStore.edit { preferences ->
            val lastValue = preferences[PreferenceKeys.FAVOURITES]?.let { Json.decodeFromString(it) } ?: DefaultValues.FAVOURITES
            preferences[PreferenceKeys.FAVOURITES] = Json.encodeToString(update(lastValue))
        }
    }

    val lowFloor = dataStore.data.map {
        it[PreferenceKeys.LOW_FLOOR] ?: DefaultValues.LOW_FLOOR
    }

    suspend fun changeLowFloor(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LOW_FLOOR] = value
        }
    }

    val departures = dataStore.data.map {
        it[PreferenceKeys.DEPARTURES] ?: DefaultValues.DEPARTURES
    }

    suspend fun changeDepartures(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEPARTURES] = value
        }
    }

    val hasCard = dataStore.data.map {
        it[PreferenceKeys.CARD] ?: DefaultValues.CARD
    }

    suspend fun changeCard(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.CARD] = value
        }
    }
}
