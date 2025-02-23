package cz.jaro.dpmcb.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.jaro.dpmcb.ui.main.AndroidDetailsOpener
import cz.jaro.dpmcb.ui.main.DetailsOpener
import org.koin.dsl.module

val platformModule = module(true) {
    single {
        PreferenceDataStoreFactory.create(
            migrations = listOf()
        ) {
            get<Context>().dataStoreFile("DPMCB_DataStore.preferences_pb")
        }
    }
    single { UserOnlineManager { get<Context>().isOnline() } }
    single<DetailsOpener> { AndroidDetailsOpener(get()) }
}

fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java)
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false

    return capabilities.hasTransport(
        NetworkCapabilities.TRANSPORT_CELLULAR
    ) || capabilities.hasTransport(
        NetworkCapabilities.TRANSPORT_WIFI
    ) || capabilities.hasTransport(
        NetworkCapabilities.TRANSPORT_ETHERNET
    )
}