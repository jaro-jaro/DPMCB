package cz.jaro.dpmcb.ui.favourites

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.combineAll
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.stateIn
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.data.recordException
import cz.jaro.dpmcb.data.vehicleName
import cz.jaro.dpmcb.data.vehicleTraction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FavouritesViewModel(
    private val repo: SpojeRepository,
    private val onlineRepo: OnlineRepository,
) : ViewModel() {

    lateinit var navigator: Navigator

    fun onEvent(e: FavouritesEvent) {
        when (e) {
            is FavouritesEvent.NavToBus -> {
                navigator.navigate(Route.Bus(e.nextWillRun ?: SystemClock.todayHere(), e.name))
            }
        }
    }

    private val recentBusesCount = repo.settings.mapState { it.recentBusesCount }

    private val trimmedRecents = combineStates(
        recentBusesCount, repo.recents
    ) { recentBusesCount, recents ->
        recents.take(recentBusesCount)
    }

    private val allBuses = combineStates(
        repo.favourites, trimmedRecents
    ) { favourites, recents ->
        favourites.map {
            FavouriteBus(it.busName, FavouriteType.Favourite, it)
        } + recents.map {
            FavouriteBus(it, FavouriteType.Recent)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val onlineBuses =
        allBuses.mapState { buses ->
            buses.map { it.busName }.toSet()
        }.flatMapMerge { buses ->
            buses
                .map { busName ->
                    onlineRepo.onlineBus(busName).map {
                        busName to it
                    }
                }
                .combineAll { onlineBuses ->
                    onlineBuses.toMap().mapValues { (busName, online) ->
                        if (online?.delayMin != null) FavouriteOnlineBusInfo(
                            delay = online.delayMin.toDouble().minutes,
                            vehicleNumber = online.vehicle,
                            currentStopTime = online.nextStop,
                        ) else null
                    }
                }
        }
            .distinctUntilChanged()
            .stateIn(SharingStarted.WhileSubscribed(5.seconds), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val states = combine(
        allBuses, onlineBuses
    ) { buses, onlineBuses ->
        buses.map { bus ->
            val (info, stops) = try {
                repo.favouriteBus(bus.busName, SystemClock.todayHere())
            } catch (e: Exception) {
                recordException(e)
                return@map null
            }
            val favourite = bus.favourite ?: PartOfConn(info.connName, 0, stops.lastIndex)
            val origin = stops.getOrNull(favourite.start) ?: stops.last()
            val destination = stops.getOrNull(favourite.end) ?: stops.last()
            val runsAt = repo.doesConnRunAt(bus.busName)
            FavouriteState(
                busName = info.connName,
                line = info.line.toShortLine(),
                lineTraction = repo.lineTraction(info.line, info.vehicleType),
                originStopName = origin.name,
                originStopTime = origin.time,
                destinationStopName = destination.name,
                destinationStopTime = destination.time,
                nextWillRun = List(365) { SystemClock.todayHere() + it.days }.firstOrNull { runsAt(it) },
                type = bus.type,
                online = onlineBuses[bus.busName]?.let { online ->
                    val currentNext =
                        stops.withIndex().last { it.value.time == online.currentStopTime }

                    OnlineFavouriteState(
                        delay = online.delay,
                        vehicleNumber = online.vehicleNumber,
                        vehicleName = online.vehicleNumber?.let(repo::vehicleName),
                        vehicleTraction = online.vehicleNumber?.let(repo::vehicleTraction)
                            ?: repo.lineTraction(info.line, info.vehicleType),
                        currentStopName = currentNext.value.name,
                        currentStopTime = currentNext.value.time,
                        positionOfCurrentStop = when {
                            currentNext.index < favourite.start -> -1
                            favourite.end < currentNext.index -> 1
                            else -> 0
                        },
                    )
                },
            )
        }.filterNotNull()
    }
        .distinctUntilChanged()
        .stateIn(SharingStarted.WhileSubscribed(5.seconds), null)

    val state =
        combineStates(states, recentBusesCount) { buses, recentBusesCount ->
            if (buses == null) return@combineStates FavouritesState.Loading

            val (favourites, recents) = buses.partition { it.type == FavouriteType.Favourite }

            val (today, otherDay) = favourites.partition { it.nextWillRun == SystemClock.todayHere() }

            FavouritesState.Loaded(
                recents = recents.takeUnless { recentBusesCount == 0 },
                runsToday = today.sortedBy { it.originStopTime },
                runsOtherDay = otherDay.sortedWith(
                    compareBy<FavouriteState> { it.nextWillRun }.thenBy { it.originStopTime }
                )
            )
        }

    data class FavouriteBus(
        val busName: BusName,
        val type: FavouriteType,
        val favourite: PartOfConn? = null,
    )

    data class FavouriteOnlineBusInfo(
        val delay: Duration,
        val vehicleNumber: RegistrationNumber?,
        val currentStopTime: LocalTime?,
    )
}
