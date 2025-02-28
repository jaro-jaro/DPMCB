package cz.jaro.dpmcb.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun UserOnlineManager(ctx: Context) = UserOnlineManager {
    val connectivityManager = ctx.getSystemService(ConnectivityManager::class.java)
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        ?: return@UserOnlineManager false

    capabilities.hasTransport(
        NetworkCapabilities.TRANSPORT_CELLULAR
    ) || capabilities.hasTransport(
        NetworkCapabilities.TRANSPORT_WIFI
    ) || capabilities.hasTransport(
        NetworkCapabilities.TRANSPORT_ETHERNET
    )
}