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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.textItem
import cz.jaro.dpmcb.data.helperclasses.toDelay
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.now_running.NowRunningEvent.ChangeFilter
import cz.jaro.dpmcb.ui.now_running.NowRunningEvent.ChangeType
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
@Composable
fun NowRunning(
    args: Route.NowRunning,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: NowRunningViewModel = viewModel(
        NowRunningViewModel.Parameters(filters = args.filters.toList(), type = args.type)
    ),
) {
    AppState.title = "Právě jedoucí"
    AppState.selected = DrawerAction.NowRunning

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
    }

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
    if (state == NowRunningState.NoLines) Text(
        text = "Bohužel, zdá se že právě nejede žádná linka. Prosím, aktualizujte jízdní řády v aplikaci.",
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    else Column {
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
                        onEvent(ChangeType(type))
                    },
                    label = { Text(type.label) }
                )
            }
        }
        when (state) {
            is NowRunningState.LoadingLines -> Text(text = "Načítání...")

            is NowRunningState.LinesLoaded -> Column {
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
                                onEvent(ChangeFilter(line))
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

            NowRunningState.NoLines -> Unit
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)))
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.busResult(
    state: NowRunningState.LinesLoaded,
    onEvent: (NowRunningEvent) -> Unit,
) = when (state) {
    is NowRunningState.Loading -> textItem("Načítání...")
    is NowRunningState.OK if state.result.online.isEmpty() -> textItem("Od vybraných linek v blízké době nejede")
    is NowRunningState.OK -> when (state.result) {
        is NowRunningResults.Lines -> state.result.online.forEach { line ->
            line(line, onEvent, key = "OL")
        }

        is NowRunningResults.RegN -> items(state.result.online, key = { "OR" + it.busName.value }) { bus ->
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

        is NowRunningResults.Delay -> items(state.result.online, key = { "OD" + it.busName.value }) { bus ->
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
private fun LazyListScope.line(
    line: RunningLineInDirection,
    onEvent: (NowRunningEvent) -> Unit,
    key: String,
) {
    val online = line.buses.none { it.vehicle.value == -1 }
    stickyHeader(key = "$key ${line.lineNumber.value} -> ${line.destination}") {
        Column(
            Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = "${line.lineNumber} -> ${line.destination}",
                style = MaterialTheme.typography.titleLarge,
                color = if (online) MaterialTheme.colorScheme.primary else Color.Unspecified,
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
    items(line.buses, key = { key + it.busName.value }) { bus ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onEvent(NowRunningEvent.NavToBus(bus.busName))
                }
        ) {
            if (online)
                Text(text = "${bus.vehicle}: ${bus.nextStopName}", modifier = Modifier.weight(1F))
            else
                Text(text = bus.nextStopName, modifier = Modifier.weight(1F))
            Text(text = bus.nextStopTime.toString())
            if (online) bus.delay?.let { delay ->
                Text(
                    text = (bus.nextStopTime + delay.toInt().minutes).toString(),
                    color = colorOfDelayText(delay),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.notRunning(
    state: NowRunningState.LinesLoaded,
    onEvent: (NowRunningEvent) -> Unit,
) = if (state is NowRunningState.OK && state.result.offlineNotOnline.isNotEmpty()) {
    stickyHeader(key = "NH") {
        Surface {
            Text(
                text = "Jedoucí spoje, které nejsou online:",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
    when (val result = state.result) {
        is NowRunningResults.Lines -> result.offlineNotOnline.forEach { line ->
            line(line, onEvent, key = "NL")
        }

        is NowRunningResults.RegN -> items(result.offlineNotOnline, key = { "NR" + it.busName.value }) { bus ->
            OfflineBus(onEvent, bus.busName, bus.lineNumber, bus.destination)
        }

        is NowRunningResults.Delay -> items(result.offlineNotOnline, key = { "ND" + it.busName.value }) { bus ->
            OfflineBus(onEvent, bus.busName, bus.lineNumber, bus.destination)
        }
    }
} else Unit

@Composable
private fun OfflineBus(onEvent: (NowRunningEvent) -> Unit, busName: BusName, line: ShortLine, destination: String) =
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onEvent(NowRunningEvent.NavToBus(busName))
            }
    ) {
        Text(text = "$line -> $destination", modifier = Modifier.weight(1F))
    }

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