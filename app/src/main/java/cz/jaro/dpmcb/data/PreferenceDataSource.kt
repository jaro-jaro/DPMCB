package cz.jaro.dpmcb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.decodeFromString
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
        val VERZE = intPreferencesKey("verze")
        val OBLIBENE = stringSetPreferencesKey("oblibene")
        val NIZKOPODLAZNOST = booleanPreferencesKey("nizkopodlaznost")
        val NASTAVENI = stringPreferencesKey("nastaveni")
    }

    object DefaultValues {
        const val VERZE = -1
        val OBLIBENE = setOf<String>()
        const val NIZKOPODLAZNOST = false
        val NASTAVENI = Nastaveni()
    }

    val nastaveni = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.NASTAVENI]?.let { Json.decodeFromString<Nastaveni>(it) } ?: DefaultValues.NASTAVENI
    }.stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), DefaultValues.NASTAVENI)

    suspend fun zmenitNastaveni(update: (Nastaveni) -> Nastaveni) {
        dataStore.edit { preferences ->
            val lastValue = preferences[PreferenceKeys.NASTAVENI]?.let { Json.decodeFromString(it) } ?: DefaultValues.NASTAVENI
            preferences[PreferenceKeys.NASTAVENI] = Json.encodeToString(update(lastValue))
        }
    }

    val verze = dataStore.data.map {
        it[PreferenceKeys.VERZE] ?: DefaultValues.VERZE
    }

    suspend fun zmenitVerzi(value: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.VERZE] = value
        }
    }

    val oblibene = dataStore.data.map {
        it[PreferenceKeys.OBLIBENE] ?: DefaultValues.OBLIBENE
    }

    suspend fun zmenitOblibene(value: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.OBLIBENE] = value
        }
    }
}
