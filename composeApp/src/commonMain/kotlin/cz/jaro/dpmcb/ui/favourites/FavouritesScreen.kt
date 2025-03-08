package cz.jaro.dpmcb.ui.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.toRegNum
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.rowItem
import cz.jaro.dpmcb.data.helperclasses.toCzechLocative
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.common.DelayBubble
import cz.jaro.dpmcb.ui.common.Name
import cz.jaro.dpmcb.ui.common.Vehicle
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Composable
@Suppress("unused")
fun Favourites(
    args: Route.Favourites,
    navController: NavHostController,
    superNavController: NavHostController,
    viewModel: FavouritesViewModel = viewModel(),
) {
    AppState.title = "Lepší DPMCB"
    AppState.selected = DrawerAction.Favourites

    LaunchedEffect(Unit) {
        viewModel.navigate = navController.navigateFunction
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
) = if (state.isOnline()) ElevatedCard(
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
        if (isOnline()) DelayBubble(delay)
        if (isOnline()) Vehicle(vehicle)
    }
    if (isOnline() && this.positionOfCurrentStop == -1) this.CurrentStop()
    Stop(
        stopName = originStopName,
        stopTime = originStopTime,
    ) { positionOfCurrentStop < 0 }
    if (isOnline() && this.positionOfCurrentStop == 0) this.CurrentStop()
    Stop(
        stopName = destinationStopName,
        stopTime = destinationStopTime,
    ) { positionOfCurrentStop < 1 }
    if (isOnline() && this.positionOfCurrentStop == 1) this.CurrentStop()

    if (showNextWillRun && nextWillRun != null) {
        Text(
            text = "Další pojede ${nextWillRun!!.toCzechLocative()}", Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
        )
    }
}

@Composable
private fun FavouriteState.Online.CurrentStop() = Stop(
    stopName = currentStopName,
    stopTime = currentStopTime,
)

@Composable
private fun FavouriteState.Stop(
    stopName: String,
    stopTime: LocalTime,
    showActualTime: FavouriteState.Online.() -> Boolean = { true },
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, end = 8.dp),
) {
    Text(text = stopName)
    Spacer(modifier = Modifier.weight(1F))
    if (isOnline() && showActualTime()) Text(
        text = "${stopTime + delay.toInt().minutes}",
        color = colorOfDelayText(delay),
        modifier = Modifier.padding(start = 8.dp)
    ) else Text(text = "$stopTime")
}

private val onlinePreviewBus = FavouriteState.Online(
    busName = LongLine(325009) / 92,
    line = LongLine(325009).toShortLine(),
    delay = 1.36F,
    vehicle = "02".toRegNum(),
    originStopName = "Suché Vrbné",
    originStopTime = LocalTime(7, 46),
    currentStopName = "Pětidomí",
    currentStopTime = LocalTime(7, 48),
    destinationStopName = "U Koníčka",
    destinationStopTime = LocalTime(7, 55),
    positionOfCurrentStop = 0,
)

private val offlinePreviewBus = FavouriteState.Offline(
    busName = LongLine(325009) / 286,
    line = LongLine(325009).toShortLine(),
    originStopName = "Suché Vrbné",
    originStopTime = LocalTime(14, 58),
    destinationStopName = "U Hvízdala",
    destinationStopTime = LocalTime(15, 20),
    nextWillRun = SystemClock.todayHere() + 1.days,
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
    runsOtherDay: List<FavouriteState.Offline>,
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
}