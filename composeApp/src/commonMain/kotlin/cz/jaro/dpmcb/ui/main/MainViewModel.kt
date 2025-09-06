package cz.jaro.dpmcb.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.MutateFunction
import cz.jaro.dpmcb.data.helperclasses.SuperNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.encodeURL
import cz.jaro.dpmcb.data.helperclasses.flattenMergeStates
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.common.getRoute
import cz.jaro.dpmcb.ui.loading.AppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainViewModel(
    private val repo: SpojeRepository,
    private val detailsOpener: DetailsOpener,
    private val appUpdater: AppUpdater,
    private val cardManager: CardManager,
    private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val link: String?,
    )

    val isOnline = repo.isOnline
    val isOnlineModeEnabled = repo.isOnlineModeEnabled

    private fun encodeLink(link: String) = link.split("?").let { segments ->
        val path = segments[0].split("/").joinToString("/") {
            it.encodeURL()
        }
        val args = segments.getOrNull(1)?.split("&")?.joinToString("&") { argument ->
            argument.split("=").let {
                val name = it[0]
                val value = it[1].encodeURL()
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

    lateinit var updateDrawerState: MutateFunction<Boolean>
    lateinit var navigator: Navigator
    lateinit var superNavigate: SuperNavigateFunction
    var currentBackStack: MutableStateFlow<StateFlow<List<NavBackStackEntry>>> = MutableStateFlow(MutableStateFlow(emptyList()))
    private val _currentBackStack = currentBackStack.flattenMergeStates()

    fun confirmDeeplink(
        confirmDeeplink: (String) -> Unit,
        navGraph: () -> NavGraph?,
    ) = params.link?.let {
        val link = params.link.removePrefix("/#")
        if (link == "app-details") detailsOpener.openAppDetails()
        if (link == "update") return@let superNavigate(SuperRoute.Loading(update = true, link = null), popUpTo<SuperRoute.Main>())
        viewModelScope.launch(Dispatchers.IO) {
            val url = encodeLink(link)
            while (navGraph() == null) Unit
            try {
                withContext(Dispatchers.Main) {
                    AppState.selected = null
                    navGraph()

                    confirmDeeplink(url.translateOldCzechLinks().transformBusIds().addInvalidDepartureTime())
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    private val currentBackStackEntry =
        _currentBackStack.mapState { it.lastOrNull() }

    fun onEvent(e: MainEvent) = when (e) {
        is MainEvent.DrawerItemClicked -> {
            if (e.action.multiselect)
                AppState.selected = e.action

            e.action.route?.let {
                navigator.navigate(it(currentBackStackEntry.value.date()))
            }
            updateDrawerState { false }
        }

        MainEvent.RemoveCard -> cardManager.removeCard()
        MainEvent.ToggleDrawer -> updateDrawerState { !it }
        MainEvent.NavigateBack -> navigator.navigateUp()
        MainEvent.ToggleOnlineMode -> repo.editOnlineMode(!isOnlineModeEnabled.value)
        MainEvent.UpdateData -> superNavigate(SuperRoute.Loading(update = true, link = null), popUpTo<SuperRoute.Main>())
        is MainEvent.UpdateApp -> appUpdater.updateApp(e.loadingDialog)
    }

    private fun NavBackStackEntry?.date(): LocalDate = this?.getRoute()?.date ?: SystemClock.todayHere()

    val state = combineStates(
        viewModelScope, isOnline, isOnlineModeEnabled, cardManager.card, currentBackStackEntry, _currentBackStack
    ) { isOnline, isOnlineModeEnabled, hasCard, currentBackStackEntry, currentBackStack ->
        MainState(
            onlineStatus = OnlineStatus(isOnline, isOnlineModeEnabled),
            hasCard = hasCard != null,
            date = currentBackStackEntry.date(),
            canGoBack = currentBackStack.count() > 1,
        )
    }

    private fun OnlineStatus(isOnline: Boolean, isOnlineModeEnabled: Boolean): MainState.OnlineStatus =
        if (!isOnline) MainState.OnlineStatus.Offline
        else MainState.OnlineStatus.Online(isOnlineModeEnabled)
}