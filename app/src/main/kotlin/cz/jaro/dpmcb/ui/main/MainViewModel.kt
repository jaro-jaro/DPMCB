package cz.jaro.dpmcb.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class MainViewModel(
    repo: SpojeRepository,
    onlineRepository: OnlineRepository,
    @InjectedParam link: String?,
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

    lateinit var confirmDeeplink: (String) -> Unit
    lateinit var navGraph: () -> NavGraph?
    lateinit var navigateToLoadingActivity: (update: Boolean) -> Unit

    init {
        link?.let {
            viewModelScope.launch(Dispatchers.IO) {
                val url = encodeLink(link.removePrefix("/"))
                while (!::confirmDeeplink.isInitialized || !::navGraph.isInitialized || navGraph() == null) Unit
                try {
                    withContext(Dispatchers.Main) {
                        App.selected = null
                        navGraph()?.nodes
                        confirmDeeplink(url.translateOldCzechLinks().transformBusIds().addInvalidDepartureTime())
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    val updateData = {
        navigateToLoadingActivity(true)
    }

    val removeCard = {
        viewModelScope.launch {
            repo.changeCard(false)
        }
        Unit
    }
}