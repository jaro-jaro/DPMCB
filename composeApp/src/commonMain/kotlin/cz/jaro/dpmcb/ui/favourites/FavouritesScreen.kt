package cz.jaro.dpmcb.ui.favourites

/*
@Composable
@Suppress("unused")
fun Favourites(
    args: Route.Favourites,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: FavouritesViewModel = viewModel(),
) {
    AppState.title = "Lepší DPMCB"
    AppState.selected = DrawerAction.Favourites

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    FavouritesScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun FavouritesScreen(
    state: FavouritesState,
    onEvent: (FavouritesEvent) -> Unit,
) = LazyColumn(
    modifier = Modifier.fillMaxSize()
) {
    if (state == FavouritesState.Loading) rowItem(
        Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
    if (state is FavouritesState.Loaded) loaded(state, onEvent)
    item {
        Spacer(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)))
    }
}

private fun LazyListScope.loaded(
    state: FavouritesState.Loaded,
    onEvent: (FavouritesEvent) -> Unit,
) {
    if (state.recents != null && state.recents.isEmpty()) item {
        Text(
            text = "V nedávné době jste nenavštívili žádný spoj",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    } else if (state.recents != null) {
        item {
            Text(
                text = "Nedávno navštívené spoje",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(state.recents, key = { "N" + it.busName.value }) {
            Card(it, onEvent, false)
        }
    }

    if (state.recents != null) item {
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }

    if (state.runsToday.isEmpty() && state.runsOtherDay.isEmpty()) item {
        Text(
            text = "Zatím nemáte žádné oblíbené spoje. Přidejte si je kliknutím na ikonu hvězdičky v detailu spoje",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    } else item {
        Text(
            text = "Oblíbené spoje",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (state.runsToday.isEmpty() && state.runsOtherDay.isNotEmpty()) item {
        Text(
            text = "Dnes nejede žádný z vašich oblíbených spojů",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    } else if (state.runsToday.isNotEmpty()) {
        item {
            Text(
                text = "Jede dnes",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(state.runsToday, key = { "D" + it.busName.value }) {
            Card(it, onEvent, false)
        }
    }

    if (state.runsOtherDay.isNotEmpty()) {
        item {
            Text(
                text = "Jede jindy",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(state.runsOtherDay, key = { "J" + it.busName.value }) {
            Card(it, onEvent, true)
        }
    }
}

@Composable
private fun Card(
    state: FavouriteState,
    onEvent: (FavouritesEvent) -> Unit,
    showNextWillRun: Boolean,
) = if (state.online != null) ElevatedCard(
    onClick = {
        onEvent(FavouritesEvent.NavToBus(state.busName, state.nextWillRun))
    },
    Modifier
        .fillMaxWidth()
        .padding(all = 8.dp),
    content = {
        state.CardContent(showNextWillRun)
    },
)
else OutlinedCard(
    onClick = {
        onEvent(FavouritesEvent.NavToBus(state.busName, state.nextWillRun))
    },
    Modifier
        .fillMaxWidth()
        .padding(all = 8.dp),
    content = {
        state.CardContent(showNextWillRun)
        Spacer(Modifier.height(8.dp))
    },
)

@Composable
private fun FavouriteState.CardContent(
    showNextWillRun: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Name("$line", Modifier.padding(end = 8.dp))
        VehicleIcon(lineTraction, vehicleTraction, Modifier.padding(horizontal = 8.dp))
        if (online != null) DelayBubble(online.delay)
        Vehicle(vehicleNumber, vehicleName)
    }
    if (online != null && online.positionOfCurrentStop == -1) CurrentStop()
    Stop(
        stopName = originStopName, stopTime = originStopTime,
        showActualTime = { positionOfCurrentStop < 0 },
    )
    if (online?.positionOfCurrentStop == 0) CurrentStop()
    Stop(
        stopName = destinationStopName, stopTime = destinationStopTime,
        showActualTime = { positionOfCurrentStop < 1 },
    )
    if (online?.positionOfCurrentStop == 1) CurrentStop()

    if (showNextWillRun && nextWillRun != null) {
        Text(
            text = "Další pojede ${nextWillRun.toCzechLocative()}", Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
        )
    }
}

@Composable
private fun FavouriteState.CurrentStop() = Stop(
    stopName = online?.currentStopName ?: error("online may not be null"),
    stopTime = online.currentStopTime,
    showActualTime = { true },
)

@Composable
private fun FavouriteState.Stop(
    stopName: String,
    stopTime: LocalTime,
    showActualTime: OnlineFavouriteState.() -> Boolean,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, end = 8.dp),
) {
    Text(text = stopName)
    Spacer(modifier = Modifier.weight(1F))
    if (online != null && online.showActualTime()) Text(
        text = "${stopTime + online.delay.truncatedToMinutes()}",
        color = colorOfDelayText(online.delay),
        modifier = Modifier.padding(start = 8.dp)
    ) else Text(text = "$stopTime")
}

@OptIn(ExperimentalTime::class)
private val onlinePreviewBus = FavouriteState(
    busName = LongLine(325009) / 92,
    line = LongLine(325009).toShortLine(),
    lineTraction = Traction.Trolleybus,
    originStopName = "Suché Vrbné",
    originStopTime = LocalTime(7, 46),
    destinationStopName = "U Koníčka",
    destinationStopTime = LocalTime(7, 55),
    type = FavouriteType.Favourite,
    online = OnlineFavouriteState(
        delay = 1.36.minutes,
        currentStopName = "Pětidomí",
        currentStopTime = LocalTime(7, 48),
        positionOfCurrentStop = 0,
    ),
    nextWillRun = SystemClock.todayHere(),
    vehicleNumber = "02".toRegNum(),
    vehicleName = "Rába",
    vehicleTraction = Traction.Trolleybus,
)

@OptIn(ExperimentalTime::class)
private val offlinePreviewBus = FavouriteState(
    busName = LongLine(325009) / 286,
    line = LongLine(325009).toShortLine(),
    lineTraction = Traction.Trolleybus,
    originStopName = "Suché Vrbné",
    originStopTime = LocalTime(14, 58),
    destinationStopName = "U Hvízdala",
    destinationStopTime = LocalTime(15, 20),
    nextWillRun = SystemClock.todayHere() + 1.days,
    type = FavouriteType.Recent,
    online = null,
    vehicleNumber = null,
    vehicleName = null,
    vehicleTraction = null,
)

@Preview
@Composable
private fun FavouritesPreview1R0() = FavouritesPreview(
    recents = null,
    runsToday = listOf(onlinePreviewBus, offlinePreviewBus),
    runsOtherDay = listOf(offlinePreviewBus),
)
@Preview
@Composable
private fun FavouritesPreview2R0() = FavouritesPreview(
    recents = null,
    runsToday = listOf(),
    runsOtherDay = listOf(offlinePreviewBus),
)
@Preview
@Composable
private fun FavouritesPreview3R0() = FavouritesPreview(
    recents = null,
    runsToday = listOf(onlinePreviewBus, offlinePreviewBus),
    runsOtherDay = listOf(),
)
@Preview
@Composable
private fun FavouritesPreview4R0() = FavouritesPreview(
    recents = null,
    runsToday = listOf(),
    runsOtherDay = listOf(),
)
@Preview
@Composable
private fun FavouritesPreview1R1() = FavouritesPreview(
    recents = listOf(),
    runsToday = listOf(onlinePreviewBus, offlinePreviewBus),
    runsOtherDay = listOf(offlinePreviewBus),
)
@Preview
@Composable
private fun FavouritesPreview2R1() = FavouritesPreview(
    recents = listOf(),
    runsToday = listOf(),
    runsOtherDay = listOf(offlinePreviewBus),
)
@Preview
@Composable
private fun FavouritesPreview3R1() = FavouritesPreview(
    recents = listOf(),
    runsToday = listOf(onlinePreviewBus, offlinePreviewBus),
    runsOtherDay = listOf(),
)
@Preview
@Composable
private fun FavouritesPreview4R1() = FavouritesPreview(
    recents = listOf(),
    runsToday = listOf(),
    runsOtherDay = listOf(),
)
@Preview
@Composable
private fun FavouritesPreview1R2() = FavouritesPreview(
    recents = listOf(offlinePreviewBus, onlinePreviewBus),
    runsToday = listOf(onlinePreviewBus, offlinePreviewBus),
    runsOtherDay = listOf(offlinePreviewBus),
)
@Preview
@Composable
private fun FavouritesPreview2R2() = FavouritesPreview(
    recents = listOf(offlinePreviewBus, onlinePreviewBus),
    runsToday = listOf(),
    runsOtherDay = listOf(offlinePreviewBus),
)
@Preview
@Composable
private fun FavouritesPreview3R2() = FavouritesPreview(
    recents = listOf(offlinePreviewBus, onlinePreviewBus),
    runsToday = listOf(onlinePreviewBus, offlinePreviewBus),
    runsOtherDay = listOf(),
)
@Preview
@Composable
private fun FavouritesPreview4R2() = FavouritesPreview(
    recents = listOf(offlinePreviewBus, onlinePreviewBus),
    runsToday = listOf(),
    runsOtherDay = listOf(),
)

@Composable
private fun FavouritesPreview(
    recents: List<FavouriteState>?,
    runsToday: List<FavouriteState>,
    runsOtherDay: List<FavouriteState>,
) {
    Surface {
        FavouritesScreen(
            state = FavouritesState.Loaded(
                recents = recents,
                runsToday = runsToday,
                runsOtherDay = runsOtherDay,
            ),
            onEvent = {}
        )
    }
}*/
