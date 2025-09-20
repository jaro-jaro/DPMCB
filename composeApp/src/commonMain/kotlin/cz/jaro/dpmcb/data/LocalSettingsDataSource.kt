package cz.jaro.dpmcb.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullStateFlow
import com.russhwolf.settings.coroutines.getIntOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import com.russhwolf.settings.set
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.MutateLambda
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.toJson
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

interface LocalSettingsDataSource {
    val settings: StateFlow<Settings>
    fun changeSettings(update: MutateLambda<Settings>)

    val version: StateFlow<Int>
    fun changeVersion(value: Int)

    val favourites: StateFlow<List<PartOfConn>>
    fun changeFavourites(update: MutateLambda<List<PartOfConn>>)

    val recents: StateFlow<List<BusName>>
    fun changeRecents(update: MutateLambda<List<BusName>>)

    val hasCard: StateFlow<Boolean>
    fun changeCard(value: Boolean)
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

@OptIn(ExperimentalSettingsApi::class)
class MultiplatformSettingsDataSource(
    private val data: ObservableSettings,
) : LocalSettingsDataSource {
    private val scope = CoroutineScope(Dispatchers.IO)

    object Keys {
        const val VERSION = "verze"
        const val FAVOURITES = "oblibene_useky"
        const val RECENTS = "recents"
//        const val DEPARTURES = "odjezdy"
        const val SETTINGS = "nastaveni"
        const val CARD = "prukazka"
    }

    object DefaultValues {
        const val VERSION = -1
        val FAVOURITES = listOf<PartOfConn>()
        val RECENTS = listOf<BusName>()
//        const val DEPARTURES = false
        val SETTINGS = Settings()
        const val CARD = false
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
            it?.fromJson<List<BusName>>(json) ?: DefaultValues.RECENTS
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
}
