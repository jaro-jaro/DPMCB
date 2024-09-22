package cz.jaro.dpmcb.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nullable
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.tuples.Quintuple
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.days

@KoinViewModel
class FavouritesViewModel(
    private val repo: SpojeRepository,
    private val onlineRepo: OnlineRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val navigate: NavigateFunction,
    )

    fun onEvent(e: FavouritesEvent) {
        when (e) {
            is FavouritesEvent.NavToBusToday -> {
                params.navigate(Route.Bus(e.name, SystemClock.todayHere()))
            }

            is FavouritesEvent.NavToBusOtherDay -> {
                params.navigate(Route.Bus(e.name, e.nextWillRun ?: SystemClock.todayHere()))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = repo.favourites
        .flatMapLatest { favourites ->
            favourites
                .map { favourite ->
                    onlineRepo.onlineBus(favourite.busName)
                }
                .combine {
                    it.nullable()
                }
                .onEmpty {
                    emit(null)
                }
                .map {
                    it?.zip(favourites) { onlineConn, favourite ->
                        val bus = try {
                            repo.favouriteBus(favourite.busName, SystemClock.todayHere())
                        } catch (e: Exception) {
                            Firebase.crashlytics.recordException(e)
                            return@zip null
                        }
                        Quintuple(onlineConn, bus.first, bus.second, repo.doesConnRunAt(favourite.busName), favourite)
                    }
                }
        }
        .map { buses ->
            (buses ?: emptyList()).filterNotNull().map { (onlineConn, info, stops, runsAt, favourite) ->
                if (onlineConn?.delayMin != null) FavouriteState.Online(
                    busName = info.connName,
                    line = info.line,
                    delay = onlineConn.delayMin,
                    vehicle = onlineConn.vehicle,
                    originStopName = (stops.getOrNull(favourite.start) ?: stops.last()).name,
                    originStopTime = (stops.getOrNull(favourite.start) ?: stops.last()).time,
                    currentStopName = stops.last { it.time == onlineConn.nextStop }.name,
                    currentStopTime = stops.last { it.time == onlineConn.nextStop }.time,
                    destinationStopName = (stops.getOrNull(favourite.end) ?: stops.last()).name,
                    destinationStopTime = (stops.getOrNull(favourite.end) ?: stops.last()).time,
                    positionOfCurrentStop = when {
                        stops.indexOfLast { it.time == onlineConn.nextStop } < favourite.start -> -1
                        stops.indexOfLast { it.time == onlineConn.nextStop } > favourite.end -> 1
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
            }
        }
        .map { buses ->

            if (buses.isEmpty()) return@map FavouritesState.NoFavourites

            val today = buses
                .filter { it.nextWillRun == SystemClock.todayHere() }
                .sortedBy { it.originStopTime }
            val otherDay = buses
                .filter { it.nextWillRun != SystemClock.todayHere() }
                .sortedWith(compareBy<FavouriteState> { it.nextWillRun }
                    .thenBy { it.originStopTime })
                .filterIsInstance<FavouriteState.Offline>() // To by mělo být vždy

            if (today.isEmpty()) return@map FavouritesState.NothingRunsToday(otherDay)

            if (otherDay.isEmpty()) return@map FavouritesState.RunsJustToday(today)

            return@map FavouritesState.RunsAnytime(today, otherDay)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavouritesState.Loading)
}