package cz.jaro.dpmcb.ui.favourites

/*@OptIn(ExperimentalTime::class)
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
                            currentStopTime = online.nextStop,
                        ) else null
                    }
                }
        }
            .distinctUntilChanged()
            .stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val states = combine(
        allBuses, onlineBuses, repo.vehicleNumbersOnSequences
    ) { buses, onlineBuses, vehicles ->
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
            val vehicleNumber = vehicles[SystemClock.todayHere()]?.get(info.sequence)
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
                        currentStopName = currentNext.value.name,
                        currentStopTime = currentNext.value.time,
                        positionOfCurrentStop = when {
                            currentNext.index < favourite.start -> -1
                            favourite.end < currentNext.index -> 1
                            else -> 0
                        },
                    )
                },
                vehicleNumber = vehicleNumber,
                vehicleName = vehicleNumber?.let(repo::vehicleName),
                vehicleTraction = vehicleNumber?.let(repo::vehicleTraction)
                    ?: repo.lineTraction(info.line, info.vehicleType),
            )
        }.filterNotNull()
    }
        .distinctUntilChanged()
        .stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), null)

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
        val currentStopTime: LocalTime?,
    )
}*/
