package cz.jaro.dpmcb.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.Quintuple
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nullable
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.LocalDate

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
                params.navigate(Route.Bus(e.id))
            }

            is FavouritesEvent.NavToBusOtherDay -> {
                params.navigate(Route.Bus(e.id))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = repo.favourites
        .flatMapLatest { favourites ->
            favourites
                .map { favourite ->
                    onlineRepo.busOnMapById(favourite.busId)
                }
                .combine {
                    it.nullable()
                }
                .onEmpty {
                    emit(null)
                }
                .combine(repo.date) { it, date ->
                    it?.zip(favourites) { onlineConn, favourite ->
                        val bus = try {
                            repo.favouriteBus(favourite.busId, date)
                        } catch (e: Exception) {
                            Firebase.crashlytics.recordException(e)
                            return@zip null
                        }
                        Quintuple(onlineConn, bus.first, bus.second, repo.doesConnRunAt(favourite.busId), favourite)
                    }
                }
        }
        .combine(repo.date) { buses, date ->
            (buses ?: emptyList()).filterNotNull().map { (onlineConn, info, stops, runsAt, favourite) ->
                if (onlineConn?.delayMin != null && date == LocalDate.now()) FavouriteState.Online(
                    busId = info.connId,
                    line = info.line,
                    delay = onlineConn.delayMin,
                    vehicle = onlineConn.vehicle,
                    originStopName = stops[favourite.start].name,
                    originStopTime = stops[favourite.start].time,
                    currentStopName = stops.last { it.time == onlineConn.nextStop }.name,
                    currentStopTime = stops.last { it.time == onlineConn.nextStop }.time,
                    destinationStopName = stops[favourite.end].name,
                    destinationStopTime = stops[favourite.end].time,
                    positionOfCurrentStop = when {
                        stops.indexOfLast { it.time == onlineConn.nextStop } < favourite.start -> -1
                        stops.indexOfLast { it.time == onlineConn.nextStop } > favourite.end -> 1
                        else -> 0
                    },
                )
                else FavouriteState.Offline(
                    busId = info.connId,
                    line = info.line,
                    originStopName = stops[favourite.start].name,
                    originStopTime = stops[favourite.start].time,
                    destinationStopName = stops[favourite.end].name,
                    destinationStopTime = stops[favourite.end].time,
                    nextWillRun = List(365) { date.plusDays(it.toLong()) }.firstOrNull { runsAt(it) },
                )
            } to date
        }
        .map { (buses, date) ->

            if (buses.isEmpty()) return@map FavouritesState.NoFavourites

            val today = buses
                .filter { it.nextWillRun == date }
                .sortedBy { it.originStopTime }
            val otherDay = buses
                .filter { it.nextWillRun != date }
                .sortedWith(compareBy<FavouriteState> { it.nextWillRun }
                    .thenBy { it.originStopTime })
                .filterIsInstance<FavouriteState.Offline>() // To by mělo být vždy

            if (today.isEmpty()) return@map FavouritesState.NothingRunsToday(otherDay, date)

            if (otherDay.isEmpty()) return@map FavouritesState.RunsJustToday(today, date)

            return@map FavouritesState.RunsAnytime(today, otherDay, date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavouritesState.Loading)
}