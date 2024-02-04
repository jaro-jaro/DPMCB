package cz.jaro.dpmcb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cz.jaro.dpmcb.data.helperclasses.CastSpoje
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
        val VERZE = intPreferencesKey("verze")
        val OBLIBENE = stringPreferencesKey("oblibene_useky")
        val NIZKOPODLAZNOST = booleanPreferencesKey("nizkopodlaznost")
        val ODJEZDY = booleanPreferencesKey("odjezdy")
        val NASTAVENI = stringPreferencesKey("nastaveni")
        val PRUKAZKA = booleanPreferencesKey("prukazka")
    }

    object DefaultValues {
        const val VERZE = -1
        val OBLIBENE = listOf<CastSpoje>()
        const val NIZKOPODLAZNOST = false
        const val ODJEZDY = false
        val NASTAVENI = Nastaveni()
        const val PRUKAZKA = false
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val nastaveni = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.NASTAVENI]?.let { json.decodeFromString<Nastaveni>(it) } ?: DefaultValues.NASTAVENI
    }.stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), DefaultValues.NASTAVENI)

    suspend fun zmenitNastaveni(update: (Nastaveni) -> Nastaveni) {
        dataStore.edit { preferences ->
            val lastValue = preferences[PreferenceKeys.NASTAVENI]?.let { json.decodeFromString(it) } ?: DefaultValues.NASTAVENI
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

    val oblibene = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.OBLIBENE]?.let { Json.decodeFromString<List<CastSpoje>>(it) } ?: DefaultValues.OBLIBENE
    }

    suspend fun zmenitOblibene(update: (List<CastSpoje>) -> List<CastSpoje>) {
        dataStore.edit { preferences ->
            val lastValue = preferences[PreferenceKeys.OBLIBENE]?.let { Json.decodeFromString(it) } ?: DefaultValues.OBLIBENE
            preferences[PreferenceKeys.OBLIBENE] = Json.encodeToString(update(lastValue))
        }
    }

    val nizkopodlaznost = dataStore.data.map {
        it[PreferenceKeys.NIZKOPODLAZNOST] ?: DefaultValues.NIZKOPODLAZNOST
    }

    suspend fun zmenitNizkopodlaznost(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NIZKOPODLAZNOST] = value
        }
    }

    val odjezdy = dataStore.data.map {
        it[PreferenceKeys.ODJEZDY] ?: DefaultValues.ODJEZDY
    }

    suspend fun zmenitOdjezdy(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ODJEZDY] = value
        }
    }

    val maPrukazku = dataStore.data.map {
        it[PreferenceKeys.PRUKAZKA] ?: DefaultValues.PRUKAZKA
    }

    suspend fun zmenitPrukazku(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PRUKAZKA] = value
        }
    }
}
