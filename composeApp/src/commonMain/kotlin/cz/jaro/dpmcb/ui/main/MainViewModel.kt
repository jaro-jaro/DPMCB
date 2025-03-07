package cz.jaro.dpmcb.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.MutateFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SuperNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.common.getRoute
import cz.jaro.dpmcb.ui.loading.AppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds

class MainViewModel(
    private val repo: SpojeRepository,
    private val detailsOpener: DetailsOpener,
    private val appUpdater: AppUpdater,
    private val cardManager: CardManager,
    private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val link: String?,
        val currentBackStackEntry: Flow<NavBackStackEntry>,
    )

    val isOnline = repo.isOnline
    val isOnlineModeEnabled = repo.isOnlineModeEnabled

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

    private fun String.transformBusIds() =
        replace("bus/S-(\\d{6})-(\\d{3})".toRegex(), "bus/$1/$2")

    private fun String.addInvalidDepartureTime() = when {
        "departures" !in this -> this
        "time=null" in this -> replace("time=null", "time=99:99")
        "time=" in this -> this
        "?" in this -> plus("&time=99:99")
        else -> plus("?time=99:99")
    }

    lateinit var confirmDeeplink: (String) -> Unit
    lateinit var navGraph: () -> NavGraph?
    lateinit var updateDrawerState: MutateFunction<Boolean>
    lateinit var navigate: NavigateFunction
    lateinit var superNavigate: SuperNavigateFunction

    init {
        params.link?.let {
            viewModelScope.launch(Dispatchers.IO) {
                val link = params.link.removePrefix("/DPMCB/")
                if (link == "app-details") detailsOpener.openAppDetails()
                val url = encodeLink(link)
                while (!::confirmDeeplink.isInitialized || !::navGraph.isInitialized || navGraph() == null) Unit
                try {
                    withContext(Dispatchers.Main) {
                        AppState.selected = null
                        navGraph()?.nodes

                        confirmDeeplink(url.translateOldCzechLinks().transformBusIds().addInvalidDepartureTime())
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onEvent(e: MainEvent) =
        viewModelScope.launch {
            when (e) {
                is MainEvent.DrawerItemClicked -> {
                    if (e.action.multiselect)
                        AppState.selected = e.action

                    e.action.route?.let {
                        navigate(it(params.currentBackStackEntry.first().date()))
                    }
                    updateDrawerState { false }
                }
                MainEvent.RemoveCard -> cardManager.removeCard()
                MainEvent.ToggleDrawer -> updateDrawerState { !it }
                MainEvent.ToggleOnlineMode -> repo.editOnlineMode(!isOnlineModeEnabled.value)
                MainEvent.UpdateData -> superNavigate(SuperRoute.Loading(update = true, link = null), popUpTo<SuperRoute.Main>())
                is MainEvent.UpdateApp -> appUpdater.updateApp(e.loadingDialog)
            }
        }

    private fun NavBackStackEntry.date(): LocalDate = getRoute()?.date ?: SystemClock.todayHere()

    val state = combine(isOnline, isOnlineModeEnabled, cardManager.card, params.currentBackStackEntry) { isOnline, isOnlineModeEnabled, hasCard, currentBackStackEntry ->
        MainState(
            onlineStatus = OnlineStatus(isOnline, isOnlineModeEnabled),
            hasCard = hasCard != null,
            date = currentBackStackEntry.date(),
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5.seconds),
        MainState(
            onlineStatus = OnlineStatus(isOnline.value, isOnlineModeEnabled.value),
        )
    )

    private fun OnlineStatus(isOnline: Boolean, isOnlineModeEnabled: Boolean): MainState.OnlineStatus =
        if (!isOnline) MainState.OnlineStatus.Offline
        else MainState.OnlineStatus.Online(isOnlineModeEnabled)
}