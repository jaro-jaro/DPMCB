package cz.jaro.dpmcb.ui.main

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateToRouteFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class MainViewModel(
    repo: SpojeRepository,
    onlineRepository: OnlineRepository,
    @InjectedParam closeDrawer: () -> Unit,
    @InjectedParam link: String?,
    @InjectedParam navController: NavHostController,
    @InjectedParam loadingActivityIntent: Intent,
    @InjectedParam startActivity: (Intent) -> Unit,
    @InjectedParam logError: (String) -> Unit,
) : ViewModel() {

    val isOnline = repo.isOnline
    val hasCard = repo.hasCard.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)
    val isOnlineModeEnabled = repo.isOnlineModeEnabled
    val editOnlineMode = repo::editOnlineMode

    private fun encodeLink(link: String) = link.split("?").let { segments ->
        val path = segments[0].split("/").joinToString("/") {
            URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
        }
        val args = segments.getOrNull(1)?.split("&")?.joinToString("&") { argument ->
            argument.split("=").let {
                val name = it[0]
                val value = URLEncoder.encode(it[1], StandardCharsets.UTF_8.toString())
                "$name=$value"
            }
        }?.let { "?$it" } ?: ""
        "$path$args".replace(Regex("%25([0-9A-F]{2})"), "%$1")
    }

    private val NavController.graphOrNull: NavGraph?
        get() = try {
            graph
        } catch (e: IllegalStateException) {
            null
        }

    private fun String.translateOldCzechLinks() = this
        .replace("prave_jedouci", "now_running")
        .replace("filtry", "filters")
        .replace("typ[^e]".toRegex(), "type")
        .replace("odjezdy", "departures")
        .replace("zastavka", "stop")
        .replace("cas", "time")
        .replace("linka", "line")
        .replace("pres", "via")
        .replace("spoj", "bus")
        .replace("spojId", "busName")
        .replace("spojeni", "connection")
        .replace("kurz", "sequence")
        .replace("vybirator", "chooser")
        .replace("typ[^e]".toRegex(), "type")
        .replace("cisloLinky", "lineNumber")
        .replace("zastavka", "stop")
        .replace("jizdni_rady", "timetable")
        .replace("cisloLinky", "lineNumber")
        .replace("zastavka", "stop")
        .replace("pristiZastavka", "nextStop")
        .replace("prukazka", "card")
        .replace("oblibene", "favourites")

    private fun String.transformBusIds() = this
        .replace("bus/S-(\\d{6})-(\\d{3})".toRegex(), "bus/$1/$2")

    private fun String.addInvalidDepartureTime(): String {
        if ("departures" !in this) return this
        if ("time=null" in this) return replace("time=null", "time=99:99")
        if ("time=" in this) return this
        if ("?" in this) return plus("&time=99:99")
        return plus("?time=99:99")
    }

    init {
        link?.let {
            viewModelScope.launch(Dispatchers.IO) {
                val url = encodeLink(link.removePrefix("/"))
                while (navController.graphOrNull == null) Unit
                try {
                    withContext(Dispatchers.Main) {
                        App.selected = null
                        navController.graphOrNull?.nodes
                        navController.navigateToRouteFunction(url.translateOldCzechLinks().transformBusIds().addInvalidDepartureTime())
                        closeDrawer()
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        logError("Vadná zkratka $url")
                    }
                }
            }
        }
    }

    val updateData = {
        startActivity(loadingActivityIntent.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra("update", true)
        })
    }
    val updateApp = {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = try {
                withContext(Dispatchers.IO) {
                    Ksoup.parseGetRequest("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/app/version.txt")
                }
            } catch (e: SocketTimeoutException) {
                Firebase.crashlytics.recordException(e)
                return@launch
            }

            val newestVersion = doc.text()

            startActivity(Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases/download/v$newestVersion/Lepsi-DPMCB-v$newestVersion.apk")
            })
        }
        Unit
    }

    val removeCard = {
        viewModelScope.launch {
            repo.changeCard(false)
        }
        Unit
    }
}

suspend inline fun <T, R> Iterable<T>.asyncMap(crossinline transform: suspend (T) -> R): List<R> = supervisorScope {
    map {
        async { transform(it) }
    }.awaitAll()
}