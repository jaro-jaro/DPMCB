package cz.jaro.dpmcb.ui.connection_search

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.mutate
import cz.jaro.dpmcb.data.helperclasses.nowHere
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.truncatedToMinutes
import cz.jaro.dpmcb.data.pushSearchToHistory
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.common.toLocalTime
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.Route.Chooser
import cz.jaro.dpmcb.ui.main.Route.ConnectionResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.atDate
import kotlinx.datetime.atTime
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ConnectionSearchViewModel(
    private val repo: SpojeRepository,
    private val onlineRepository: OnlineRepository,
    val args: Route.ConnectionSearch,
) : ViewModel() {
    lateinit var navigator: Navigator

    private val history = repo.searchHistory

    private val settings =
        MutableStateFlow(defaultSettings().let {
            SearchSettings(
                start = args.start ?: it.start,
                destination = args.destination ?: it.destination,
                directOnly = args.directOnly ?: it.directOnly,
                showInefficientConnections = args.showInefficientConnections ?: it.showInefficientConnections,
                datetime = args.date.atTime(args.time?.toLocalTime() ?: it.datetime.time),
            )
        })

    private fun defaultSettings(): SearchSettings = SearchSettings(
        start = StopName.Empty,
        destination = StopName.Empty,
        directOnly = false,
        showInefficientConnections = false,
        datetime = args.date.atTime(SystemClock.timeHere().truncatedToMinutes()),
    )

    private fun changeCurrentRoute(settings: SearchSettings) {
        AppState.route = Route.ConnectionSearch(
            date = settings.datetime.date,
            time = settings.datetime.time.toSimpleTime(),
            start = settings.start,
            destination = settings.destination,
            directOnly = settings.directOnly,
            showInefficientConnections = settings.showInefficientConnections,
        ).generateRouteWithArgs(navigator.getNavDestination() ?: return)
    }

    fun onEvent(e: ConnectionSearchEvent): Unit = when (e) {
        is ConnectionSearchEvent.ChoseStop -> navigator.navigate(
            Chooser(type = e.type, date = settings.value.datetime.date)
        )

        is ConnectionSearchEvent.ClearAll -> settings.value = defaultSettings()

        is ConnectionSearchEvent.Search -> {
            repo.pushSearchToHistory(settings.value)
            navigator.navigate(ConnectionResults(settings = settings.value))
        }

        is ConnectionSearchEvent.SearchFromHistory -> {
            onEvent(ConnectionSearchEvent.FillFromHistory(e.i, e.includeDatetime))
            onEvent(ConnectionSearchEvent.Search)
        }

        is ConnectionSearchEvent.FillFromHistory -> {
            val s = history.value[e.i]
            if (e.includeDatetime) settings.value = s
            else settings.update { s.copy(datetime = SystemClock.nowHere()) }
            changeCurrentRoute(settings.value)
        }

        is ConnectionSearchEvent.DeleteFromHistory -> {
            repo.changeSearchHistory {
                it.mutate {
                    removeAt(e.i)
                }
            }
        }

        is ConnectionSearchEvent.SetOnlyDirect -> settings.update {
            it.copy(directOnly = e.onlyDirect).also(::changeCurrentRoute)
        }

        is ConnectionSearchEvent.SetShowInefficientConnections -> settings.update {
            it.copy(showInefficientConnections = e.showInefficientConnections).also(::changeCurrentRoute)
        }

        is ConnectionSearchEvent.SwitchStops -> settings.update {
            it.copy(start = it.destination, destination = it.start).also(::changeCurrentRoute)
        }

        is ConnectionSearchEvent.WentBack -> settings.update {
            if (e.type == ChooserType.ReturnStop1)
                it.copy(start = e.stop).also(::changeCurrentRoute)
            else
                it.copy(destination = e.stop).also(::changeCurrentRoute)
        }

        is ConnectionSearchEvent.ChangeDate -> settings.update {
            it.copy(datetime = it.datetime.time.atDate(e.date)).also(::changeCurrentRoute)
        }

        is ConnectionSearchEvent.ChangeTime -> settings.update {
            it.copy(datetime = it.datetime.date.atTime(e.time)).also(::changeCurrentRoute)
        }

        is ConnectionSearchEvent.SearchFavourite -> {
            navigator.navigate(ConnectionResults(
                date = settings.value.datetime.date,
                time = settings.value.datetime.time.toSimpleTime(),
                relations = Relations(repo.favourites.value[e.i]),
                directOnly = false,
                showInefficientConnections = false,
            ))
        }
    }

    val state = combineStates(
        settings, history, repo.favourites,
    ) { settings, history, favourites ->
        ConnectionSearchState(
            settings = settings,
            settingsModified = settings.showInefficientConnections || settings.directOnly,
            history = history,
            favourites = favourites,
        )
    }

    init {
        settings.map { it.datetime.date }.distinctUntilChanged().onEach {
            val _ = repo.stopGraph(it)
            val _ = repo.connsRunAt(it)
        }.launch()
    }
}