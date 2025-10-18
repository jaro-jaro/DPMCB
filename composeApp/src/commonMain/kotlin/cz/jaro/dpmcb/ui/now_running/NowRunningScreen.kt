package cz.jaro.dpmcb.ui.now_running

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
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
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.textItem
import cz.jaro.dpmcb.data.helperclasses.toDelay
import cz.jaro.dpmcb.data.helperclasses.truncatedToSeconds
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.now_running.NowRunningEvent.ChangeFilter
import cz.jaro.dpmcb.ui.now_running.NowRunningEvent.ChangeType
import cz.jaro.dpmcb.ui.now_running.NowRunningEvent.NavToBus

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
        Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    else Column {
        when (state) {
            is NowRunningState.LoadingLines -> Text(text = "Načítání...")

            is NowRunningState.OK ->
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                        .padding(horizontal = 8.dp),
                    contentPadding = WindowInsets.safeContent.only(WindowInsetsSides.Bottom).asPaddingValues()
                ) {
                    textItem("Řadit podle:", Modifier.fillMaxWidth())
                    item {
                        FlowRow {
                            NowRunningType.entries(state.isOnline).forEach { type ->
                                FilterChip(
                                    selected = type == state.type,
                                    onClick = {
                                        onEvent(ChangeType(type))
                                    },
                                    label = { Text(type.label) },
                                    Modifier
                                        .padding(all = 4.dp),
                                )
                            }
                        }
                    }
                    textItem("Filtr linek:", Modifier.fillMaxWidth())
                    item {
                        FlowRow {
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
                    }
                    if (state.isOnline) busResult(state, onEvent)

                    notRunning(state, onEvent)
                }

            NowRunningState.NoLines -> Unit
        }
    }
}

fun NowRunningType.Companion.entries(online: Boolean) =
    if (online) listOf(NowRunningType.Line, NowRunningType.Delay, NowRunningType.RegN)
    else listOf(NowRunningType.Line, NowRunningType.RegN)

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.busResult(
    state: NowRunningState.OK,
    onEvent: (NowRunningEvent) -> Unit,
) = when {
    state.result.online == null -> textItem("Načítání…")
    state.result.online!!.isEmpty() -> textItem("Od vybraných linek v blízké době nejede")
    else -> when (state.result) {
        is NowRunningResults.Lines -> state.result.online!!.forEach { line ->
            line(line, onEvent, true)
        }

        is NowRunningResults.RegN -> items(state.result.online!!, key = { "OR" + it.busName.value }) { bus ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", Modifier.weight(1F))
                Text(text = "${bus.vehicle}")
            }
        }

        is NowRunningResults.Delay -> items(state.result.online!!, key = { "OD" + it.busName.value }) { bus ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", Modifier.weight(1F))
                bus.delay?.let {
                    Text(
                        text = bus.delay.toDelay(),
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
    online: Boolean,
) {
    stickyHeader(key = "${if (online) "OL" else "NL"} ${line.lineNumber.value} -> ${line.destination}") {
        Column(
            Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = "${line.lineNumber} -> ${line.destination}",
                style = MaterialTheme.typography.titleLarge,
                color = if (online) MaterialTheme.colorScheme.primary else Color.Unspecified,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Vůz: příští zastávka",
                    Modifier.weight(1F),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(text = "odjezd", style = MaterialTheme.typography.bodySmall)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }
    }
    items(line.buses, key = { (if (online) "OL" else "NL") + it.busName.value }) { bus ->
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onEvent(NavToBus(bus.busName))
                }
        ) {
            if (bus.vehicle != null)
                Text(text = "${bus.vehicle}: ${bus.nextStopName}", Modifier.weight(1F))
            else
                Text(text = bus.nextStopName, Modifier.weight(1F))
            Text(text = bus.nextStopTime.toString())
            if (online) bus.delay?.let { delay ->
                Text(
                    text = (bus.nextStopTime + delay.truncatedToSeconds()).toString(),
                    Modifier.padding(start = 8.dp),
                    color = colorOfDelayText(delay),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.notRunning(
    state: NowRunningState.OK,
    onEvent: (NowRunningEvent) -> Unit,
) {
    if (state.isOnline && (state.result.offlineNotOnline == null || state.result.offlineNotOnline!!.isNotEmpty())) stickyHeader(key = "NH") {
        Surface(
            Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Jedoucí spoje, které nejsou online:",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
    if (state.result.offlineNotOnline == null) textItem("Načítání…")
    else if (!state.isOnline && state.result.offlineNotOnline!!.isEmpty()) textItem("Od vybraných linek v blízké době nejede")
    else when (val result = state.result) {
        is NowRunningResults.Lines -> result.offlineNotOnline!!.forEach { line ->
            line(line, onEvent, false)
        }

        is NowRunningResults.RegN -> items(result.offlineNotOnline!!, key = { "NR" + it.busName.value }) { bus ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", Modifier.weight(1F))
                if (bus.vehicle != null) Text(text = "${bus.vehicle}")
            }
        }

        is NowRunningResults.Delay -> items(result.offlineNotOnline!!, key = { "ND" + it.busName.value }) { bus ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(NavToBus(bus.busName))
                    }
            ) {
                Text(text = "${bus.lineNumber} -> ${bus.destination}", Modifier.weight(1F))
            }
        }
    }
}

@Composable
fun Chip(
    list: List<ShortLine>,
    lineNumber: ShortLine,
    onClick: (Boolean) -> Unit,
) = FilterChip(
    selected = lineNumber in list,
    onClick = {
        onClick(lineNumber !in list)
    },
    label = { Text("$lineNumber") },
    Modifier
        .padding(all = 4.dp),
)