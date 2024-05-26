package cz.jaro.dpmcb.data

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import cz.jaro.dpmcb.data.helperclasses.PartOfConn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreMigrationConnName : DataMigration<Preferences> {
    override suspend fun cleanUp() {}

    @Serializable
    @SerialName("CastSpoje")
    data class OldPartofConn(
        @SerialName("spojId") val busId: String,
        val start: Int,
        val end: Int,
    )

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        val favsString = prefs[PreferenceDataSource.PreferenceKeys.FAVOURITES]!!
        val favs = Json.decodeFromString<List<OldPartofConn>>(favsString)
        val newFavs = favs.map {
            PartOfConn(
                busName = it.busId.split("-").drop(1).joinToString("/"),
                start = it.start,
                end = it.end,
            )
        }
        val newFavsString = Json.encodeToString(newFavs)
        prefs[PreferenceDataSource.PreferenceKeys.FAVOURITES] = newFavsString
        return prefs
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val favs = currentData[PreferenceDataSource.PreferenceKeys.FAVOURITES]
        return favs?.contains("S-") == true
    }
}