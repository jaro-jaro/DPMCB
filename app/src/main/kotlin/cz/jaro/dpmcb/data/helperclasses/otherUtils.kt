@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File

fun Offset(x: Float = 0F, y: Float = 0F) = androidx.compose.ui.geometry.Offset(x, y)

val Context.diagramFile get() = File(filesDir, "schema.pdf")

val Context.isOnline: Boolean
    get() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        ) || activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        ) || activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_ETHERNET
        )
    }