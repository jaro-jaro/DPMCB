package cz.jaro.dpmcb.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.combine
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.jikord.OnlineConn
import cz.jaro.dpmcb.data.realtions.favourites.Favourite
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.data.realtions.favourites.StopOfFavourite
import cz.jaro.dpmcb.data.recordException
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.days

class FavouritesViewModel(
    private val repo: SpojeRepository,
    private val onlineRepo: OnlineRepository,
) : ViewModel() {

    lateinit var navigate: NavigateFunction

    fun onEvent(e: FavouritesEvent) {
        when (e) {
            is FavouritesEvent.NavToBus -> {
                navigate(Route.Bus(e.nextWillRun ?: SystemClock.todayHere(), e.name))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val favourites = repo.favourites
        .map { favourites ->
            favourites
                .map { favourite ->
                    onlineRepo.onlineBus(favourite.busName)
                }
                .combine {
                    it.zip(favourites) { onlineConn, favourite ->
                        val bus = try {
                            repo.favouriteBus(favourite.busName, SystemClock.todayHere())
                        } catch (e: Exception) {
                            recordException(e)
                            return@zip null
                        }
                        FavouriteResult(onlineConn, bus.first, bus.second, repo.doesConnRunAt(favourite.busName), favourite)
                    }.filterNotNull()
                }
                .onEmpty {
                    emit(emptyList())
                }
        }
        .flattenConcat()
        .map { buses ->
            buses.map {
                it.toState()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val recents = repo.recents
        .map { recents ->
            recents
                .map { recent ->
                    onlineRepo.onlineBus(recent)
                }
                .combine {
                    it.zip(recents) { onlineConn, recent ->
                        val bus = try {
                            repo.favouriteBus(recent, SystemClock.todayHere())
                        } catch (e: Exception) {
                            recordException(e)
                            return@zip null
                        }
                        RecentResult(onlineConn, bus.first, bus.second, repo.doesConnRunAt(recent))
                    }.filterNotNull()
                }
                .onEmpty {
                    emit(emptyList())
                }
        }
        .flattenConcat()
        .map { buses ->
            buses.map { fr ->
                fr.toState()
            }
        }

    private suspend fun RecentResult.toState() =
        if (online?.delayMin != null) FavouriteState.Online(
            busName = info.connName,
            line = info.line,
            delay = online.delayMin,
            vehicle = online.vehicle,
            originStopName = stops.first().name,
            originStopTime = stops.first().time,
            currentStopName = stops.last { it.time == online.nextStop }.name,
            currentStopTime = stops.last { it.time == online.nextStop }.time,
            destinationStopName = stops.last().name,
            destinationStopTime = stops.last().time,
            positionOfCurrentStop = 0,
        )
        else FavouriteState.Offline(
            busName = info.connName,
            line = info.line,
            originStopName = stops.first().name,
            originStopTime = stops.first().time,
            destinationStopName = stops.last().name,
            destinationStopTime = stops.last().time,
            nextWillRun = List(365) { SystemClock.todayHere() + it.days }.firstOrNull { runsAt(it) },
        )

    private suspend fun FavouriteResult.toState() =
        if (online?.delayMin != null) FavouriteState.Online(
            busName = info.connName,
            line = info.line,
            delay = online.delayMin,
            vehicle = online.vehicle,
            originStopName = (stops.getOrNull(favourite.start) ?: stops.last()).name,
            originStopTime = (stops.getOrNull(favourite.start) ?: stops.last()).time,
            currentStopName = stops.last { it.time == online.nextStop }.name,
            currentStopTime = stops.last { it.time == online.nextStop }.time,
            destinationStopName = (stops.getOrNull(favourite.end) ?: stops.last()).name,
            destinationStopTime = (stops.getOrNull(favourite.end) ?: stops.last()).time,
            positionOfCurrentStop = when {
                stops.indexOfLast { it.time == online.nextStop } < favourite.start -> -1
                stops.indexOfLast { it.time == online.nextStop } > favourite.end -> 1
                else -> 0
            },
        )
        else FavouriteState.Offline(
            busName = info.connName,
            line = info.line,
            originStopName = (stops.getOrNull(favourite.start) ?: stops.last()).name,
            originStopTime = (stops.getOrNull(favourite.start) ?: stops.last()).time,
            destinationStopName = (stops.getOrNull(favourite.end) ?: stops.last()).name,
            destinationStopTime = (stops.getOrNull(favourite.end) ?: stops.last()).time,
            nextWillRun = List(365) { SystemClock.todayHere() + it.days }.firstOrNull { runsAt(it) },
        )

    val state =
        combine(favourites, recents, repo.settings) { buses, recentBuses, settings ->
            val today = buses
                .filter { it.nextWillRun == SystemClock.todayHere() }
                .sortedBy { it.originStopTime }
            val otherDay = buses
                .filter { it.nextWillRun != SystemClock.todayHere() }
                .sortedWith(compareBy<FavouriteState> { it.nextWillRun }
                    .thenBy { it.originStopTime })
                .filterIsInstance<FavouriteState.Offline>() // To by mělo být vždy

            val recents = recentBuses
                .take(settings.recentBusesCount)
                .takeUnless { settings.recentBusesCount == 0 }

            FavouritesState.Loaded(
                recents, today, otherDay
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavouritesState.Loading)
}

private data class FavouriteResult(
    val online: OnlineConn?,
    val info: Favourite,
    val stops: List<StopOfFavourite>,
    val runsAt: suspend (LocalDate) -> Boolean,
    val favourite: PartOfConn,
)

private data class RecentResult(
    val online: OnlineConn?,
    val info: Favourite,
    val stops: List<StopOfFavourite>,
    val runsAt: suspend (LocalDate) -> Boolean,
)
