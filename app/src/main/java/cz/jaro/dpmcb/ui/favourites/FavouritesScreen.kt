package cz.jaro.dpmcb.ui.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App.Companion.selected
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.common.DelayBubble
import cz.jaro.dpmcb.ui.common.Name
import cz.jaro.dpmcb.ui.common.Vehicle
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes

@Composable
@Suppress("UNUSED_PARAMETER")
fun Favourites(
    args: Route.Favourites,
    navController: NavHostController,
    viewModel: FavouritesViewModel = run {
        koinViewModel {
            parametersOf(
                FavouritesViewModel.Parameters
            )
        }
    },
) {
    title = R.string.app_name
    selected = DrawerAction.Favourites

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

    if (state == FavouritesState.NoFavourites) item {
        Text(
            text = "Zatím nemáte žádné oblíbené spoje. Přidejte si je kliknutím na ikonu hvězdičky v detailu spoje",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (state is FavouritesState.NothingRunsToday) item {
        Text(
            text = "${state.today.toCzechLocative().replaceFirstChar { it.titlecase() }} nejede žádný z vašich oblíbených spojů",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (state is FavouritesState.SomethingRunsToday) {
        item {
            Text(
                text = "Jede ${state.today.toCzechLocative()}",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(state.runsToday, key = { it.busName.value }) {
            val content: @Composable ColumnScope.() -> Unit = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Name("${it.line}", Modifier.padding(end = 8.dp))
                    if (it is FavouriteState.Online) DelayBubble(it.delay)
                    if (it is FavouriteState.Online) Vehicle(it.vehicle)
                }
                @Composable
                fun CurrentStop() {
                    if (it is FavouriteState.Online) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp),
                        ) {
                            Text(text = it.currentStopName, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.weight(1F))
                            Text(
                                text = "${it.currentStopTime + it.delay.toInt().minutes}",
                                color = colorOfDelayText(it.delay),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (it is FavouriteState.Online && it.positionOfCurrentStop == -1) CurrentStop()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                ) {
                    Text(text = it.originStopName)
                    Spacer(modifier = Modifier.weight(1F))
                    if (it is FavouriteState.Online && it.positionOfCurrentStop == -1) Text(
                        text = "${it.originStopTime + it.delay.toInt().minutes}",
                        color = colorOfDelayText(it.delay),
                        modifier = Modifier.padding(start = 8.dp)
                    ) else Text(text = "${it.originStopTime}")
                }
                if (it is FavouriteState.Online && it.positionOfCurrentStop == 0) CurrentStop()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                ) {
                    Text(text = it.destinationStopName)
                    Spacer(modifier = Modifier.weight(1F))
                    if (it is FavouriteState.Online && it.positionOfCurrentStop < 1) Text(
                        text = "${it.destinationStopTime + it.delay.toInt().minutes}",
                        color = colorOfDelayText(it.delay),
                        modifier = Modifier.padding(start = 8.dp)
                    ) else Text(text = "${it.destinationStopTime}")
                }
                if (it is FavouriteState.Online && it.positionOfCurrentStop == 1) CurrentStop()

                Spacer(Modifier.height(8.dp))
            }

            if (it is FavouriteState.Online) ElevatedCard(
                onClick = {
                    onEvent(FavouritesEvent.NavToBusToday(it.busName))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                content = content,
            )
            else OutlinedCard(
                onClick = {
                    onEvent(FavouritesEvent.NavToBusToday(it.busName))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                content = content,
            )
        }
    }

    if (state is FavouritesState.SomethingRunsOtherDay) {
        item {
            Text(
                text = "Jede jindy",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(state.runsOtherDay, key = { it.busName.value }) {

            OutlinedCard(
                onClick = {
                    onEvent(FavouritesEvent.NavToBusOtherDay(it.busName, it.nextWillRun))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                ) {
                    Name("${it.line}")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = it.originStopName)
                    Text(text = it.originStopTime.toString())
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = it.destinationStopName)
                    Text(text = "${it.destinationStopTime}")
                }
                if (it.nextWillRun != null) {
                    Text(
                        text = "Další pojede ${if (state.today != SystemClock.todayHere()) it.nextWillRun.asString() else it.nextWillRun.toCzechLocative()}", Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
                    )
                }
            }
        }
    }
}