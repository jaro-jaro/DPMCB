package cz.jaro.dpmcb.ui.now_running

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.textItem
import cz.jaro.dpmcb.data.helperclasses.toDelay
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
@Composable
fun NowRunning(
    args: Route.NowRunning,
    navController: NavHostController,
    superNavController: NavHostController,
    viewModel: NowRunningViewModel = run {
        koinViewModel {
            parametersOf(NowRunningViewModel.Parameters(filters = args.filters.toList(), type = args.type))
        }
    },
) {
    LaunchedEffect(Unit) {
        viewModel.navigate = navController.navigateFunction
        viewModel.getNavDestination = { navController.currentBackStackEntry?.destination }
    }

    App.title = R.string.now_running
    App.selected = DrawerAction.NowRunning

    val state by viewModel.state.collectAsStateWithLifecycle()

    NowRunningScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NowRunningScreen(
    state: NowRunningState,
    onEvent: (NowRunningEvent) -> Unit,
) {
    when (state) {
        NowRunningState.IsNotToday -> Text(
            text = "Pro zobrazení právě jedoucích spojů si změňte datum na dnešek",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        NowRunningState.Offline -> Text(
            text = "Jste offline :(",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        NowRunningState.NoLines -> Text(
            text = "Bohužel, zdá se že právě nejede žádná linka. Toto může také nastat pokud má Dopravní podnik výpadek svých informačních serverů. V takovém případě nefungují aktuální informace o spojích ani kdekoliv jinde, včetně zastávkových označníků ve městě.",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        is NowRunningState.HasType -> Column {
            Text(
                "Řadit podle:", modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                NowRunningType.entries.forEach { type ->
                    FilterChip(
                        modifier = Modifier
                            .padding(all = 4.dp),
                        selected = type == state.type,
                        onClick = {
                            onEvent(NowRunningEvent.ChangeType(type))
                        },
                        label = { Text(type.label) }
                    )
                }
            }
            when (state) {
                is NowRunningState.LoadingLines -> Text(text = "Načítání...")

                is NowRunningState.HasFilters -> Column {
                    Text(
                        "Filtr linek:", modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        state.lineNumbers.forEach { line ->
                            Chip(
                                list = state.filters,
                                lineNumber = line,
                                onClick = {
                                    onEvent(NowRunningEvent.ChangeFilter(line))
                                }
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        busResult(state, onEvent)

                        notRunning(state, onEvent)
                    }
                }
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.busResult(
    state: NowRunningState.HasFilters,
    onEvent: (NowRunningEvent) -> Unit,
) = when (state) {
    is NowRunningState.NothingRunningNow -> textItem("Od vybraných linek právě nic nejede")
    is NowRunningState.NothingRunsToday -> textItem("Od vybraných linek v blízké době nejede")
    is NowRunningState.Loading -> textItem("Načítání...")
    is NowRunningState.OK -> when (state.result) {
        is NowRunningResults.Lines -> state.result.list.forEach { line ->
            stickyHeader(key = "OL ${line.lineNumber.value} -> ${line.destination}") {
                Column(
                    Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "${line.lineNumber} -> ${line.destination}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(text = "Vůz: příští zastávka", modifier = Modifier.weight(1F), style = MaterialTheme.typography.labelMedium)
                        Text(text = "odjezd", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
            items(line.buses, key = { "OL" + it.busName.value }) { bus ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onEvent(NowRunningEvent.NavToBus(bus.busName))
                        }
                ) {
                    Text(text = "${bus.vehicle}: ${bus.nextStopName}", modifier = Modifier.weight(1F))
                    Text(text = bus.nextStopTime.toString())
                    bus.delay?.let {
                        Text(
                            text = (bus.nextStopTime + bus.delay.toInt().minutes).toString(),
                            color = colorOfDelayText(bus.delay),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        is NowRunningResults.RegN -> items(state.result.list, key = { "OR" + it.busName.value }) { bus ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NowRunningEvent.NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", modifier = Modifier.weight(1F))
                Text(text = "${bus.vehicle}")
            }
        }

        is NowRunningResults.Delay -> items(state.result.list, key = { "OD" + it.busName.value }) { bus ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NowRunningEvent.NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", modifier = Modifier.weight(1F))
                bus.delay?.let {
                    Text(
                        text = bus.delay.toDouble().minutes.toDelay(),
                        color = colorOfDelayText(bus.delay)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.notRunning(
    state: NowRunningState.HasFilters,
    onEvent: (NowRunningEvent) -> Unit,
) = if (state is NowRunningState.HasNotRunning && state.nowNotRunning.list.isNotEmpty()) {
    stickyHeader(key = "NH") {
        Surface {
            Text(
                text = "Jedoucí spoje, které nejsou online:",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
    when (val result = state.nowNotRunning) {
        is NowRunningResults.Lines -> result.list.forEach { line ->
            stickyHeader(key = "NL ${line.lineNumber.value} -> ${line.destination}") {
                Column(
                    Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "${line.lineNumber} -> ${line.destination}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(text = "Příští zastávka", modifier = Modifier.weight(1F), style = MaterialTheme.typography.labelMedium)
                        Text(text = "odjezd", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
            items(line.buses, key = { "NL" + it.busName.value }) { bus ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onEvent(NowRunningEvent.NavToBus(bus.busName))
                        }
                ) {
                    Text(text = bus.nextStopName, modifier = Modifier.weight(1F))
                    Text(text = bus.nextStopTime.toString())
                }
            }
        }

        is NowRunningResults.RegN -> items(result.list, key = { "NR" + it.busName.value }) { bus ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NowRunningEvent.NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", modifier = Modifier.weight(1F))
            }
        }

        is NowRunningResults.Delay -> items(result.list, key = { "ND" + it.busName.value }) { bus ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NowRunningEvent.NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", modifier = Modifier.weight(1F))
            }
        }
    }
} else Unit

@Composable
fun Chip(
    list: List<ShortLine>,
    lineNumber: ShortLine,
    onClick: (Boolean) -> Unit,
) = FilterChip(
    modifier = Modifier
        .padding(all = 4.dp),
    selected = lineNumber in list,
    onClick = {
        onClick(lineNumber !in list)
    },
    label = { Text("$lineNumber") }
)