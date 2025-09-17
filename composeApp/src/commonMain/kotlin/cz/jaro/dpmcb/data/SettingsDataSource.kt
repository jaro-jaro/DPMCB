package cz.jaro.dpmcb.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullStateFlow
import com.russhwolf.settings.coroutines.getIntOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import com.russhwolf.settings.set
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.toJson
import cz.jaro.dpmcb.data.helperclasses.work
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSettingsApi::class)
class SettingsDataSource(
    private val data: ObservableSettings,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    object Keys {
        const val VERSION = "verze"
        const val FAVOURITES = "oblibene_useky"
        const val RECENTS = "recents"
        const val DEPARTURES = "odjezdy"
        const val SETTINGS = "nastaveni"
        const val CARD = "prukazka"
    }

    object DefaultValues {
        const val VERSION = -1
        val FAVOURITES = listOf<PartOfConn>()
        val RECENTS = listOf<BusName>()
        const val DEPARTURES = false
        val SETTINGS = Settings()
        const val CARD = false
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val settings = data
        .getStringOrNullStateFlow(scope, Keys.SETTINGS)
        .mapState(scope) {
            it?.fromJson<Settings>(json) ?: DefaultValues.SETTINGS
        }

    fun changeSettings(update: (Settings) -> Settings) {
        data[Keys.SETTINGS] = update(settings.value).toJson(json)
    }

    val version = data
        .getIntOrNullStateFlow(scope, Keys.VERSION)
        .mapState(scope) {
            it ?: DefaultValues.VERSION
        }

    fun changeVersion(value: Int) {
        data[Keys.VERSION] = value
    }

    val favourites = data
        .getStringOrNullStateFlow(scope, Keys.FAVOURITES)
        .mapState(scope) {
            it?.fromJson<List<PartOfConn>>(json).work(1)
            (it?.fromJson<List<PartOfConn>>(json) ?: DefaultValues.FAVOURITES)
        }

    init {
        data.addStringOrNullListener(Keys.FAVOURITES) {
            it?.fromJson<List<PartOfConn>>(json).work(2)
        }
    }

    fun changeFavourites(update: (List<PartOfConn>) -> List<PartOfConn>) {
        data[Keys.FAVOURITES] = update(favourites.value.work(3)).work(4).toJson(json)
    }

    val recents = data
        .getStringOrNullStateFlow(scope, Keys.RECENTS)
        .mapState(scope) {
            it?.fromJson<List<BusName>>(json) ?: DefaultValues.RECENTS
        }

    fun changeRecents(update: (List<BusName>) -> List<BusName>) {
        data[Keys.RECENTS] = update(recents.value).toJson(json)
    }

    val departures = data
        .getBooleanOrNullStateFlow(scope, Keys.DEPARTURES)
        .mapState(scope) {
            it ?: DefaultValues.DEPARTURES
        }

    fun changeDepartures(value: Boolean) {
        data[Keys.DEPARTURES] = value
    }

    val hasCard = data
        .getBooleanOrNullStateFlow(scope, Keys.CARD)
        .mapState(scope) {
            it ?: DefaultValues.CARD
        }

    fun changeCard(value: Boolean) {
        data[Keys.CARD] = value
    }
}
