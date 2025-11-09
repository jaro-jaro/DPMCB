package cz.jaro.dpmcb.ui.connection_search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoTransfer
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TransferWithinAStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.nowHere
import cz.jaro.dpmcb.data.helperclasses.rowItem
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.truncatedToMinutes
import cz.jaro.dpmcb.data.helperclasses.two
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.ChooserResult
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.TimePickerDialog
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.getResult
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number
import kotlin.time.ExperimentalTime

@Suppress("unused")
@Composable
fun ConnectionSearch(
    args: Route.ConnectionSearch,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: ConnectionSearchViewModel = viewModel(args),
) {
    AppState.title = "Vyhledávání spojení"
    AppState.selected = DrawerAction.Connection

    LifecycleResumeEffect(Unit) {
        val result = navigator.getResult<ChooserResult>()

        if (result != null) viewModel.onEvent(
            ConnectionSearchEvent.WentBack(type = result.chooserType, stop = result.value)
        )

        onPauseOrDispose {
            navigator.clearResult()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ConnectionSearchScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ConnectionSearchScreen(
    state: ConnectionSearchState,
    onEvent: (ConnectionSearchEvent) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showSettings) AlertDialog(
        onDismissRequest = { showSettings = false },
        confirmButton = {
            TextButton(onClick = {
                showSettings = false
            }) {
                Text("OK")
            }
        },
        title = { Text(text = "Nastavení vyhledávání spojení") },
        text = {
            SearchSettings(state, onEvent)
        }
    )

    val time by derivedStateOf { state.settings.datetime.time }
    val timeState = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = true,
    )
    LaunchedEffect(time) {
        timeState.hour = time.hour
        timeState.minute = time.minute
    }
    if (showTimePicker) TimePickerDialog(
        onDismissRequest = { showTimePicker = false },
        confirmButton = {
            TextButton(onClick = {
                showTimePicker = false
                val time = LocalTime(timeState.hour, timeState.minute)
                onEvent(ConnectionSearchEvent.ChangeTime(time))
            }) { Text("OK") }
        },
        dismissButton = { TextButton({ showTimePicker = false }) { Text("Zrušit") } },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Změnit čas")
            TextButton(
                onClick = {
                    onEvent(ConnectionSearchEvent.ChangeTime(SystemClock.timeHere().truncatedToMinutes()))
                    onEvent(ConnectionSearchEvent.ChangeDate(SystemClock.todayHere()))
                    showTimePicker = false
                }
            ) {
                Text("Teď")
            }
        }
        TimePicker(timeState)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rowItem {
            DateSelector(
                date = state.settings.datetime.date,
                onDateChange = {
                    onEvent(ConnectionSearchEvent.ChangeDate(it))
                },
            )

            TextButton(
                onClick = {
                    showTimePicker = true
                },
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 12.dp),
            ) {
                IconWithTooltip(Icons.Default.AccessTime, "Změnit čas", Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = state.settings.datetime.time.toString())
            }
        }

        rowItem(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onEvent(ConnectionSearchEvent.SwitchStops)
                },
            ) {
                IconWithTooltip(Icons.Default.SwapVert, "Prohodit start a cíl")
            }
            Column(
                Modifier.width(IntrinsicSize.Max)
            ) {
                TextFieldButton(
                    text = state.settings.start,
                    onClick = {
                        onEvent(ConnectionSearchEvent.ChoseStop(ChooserType.ReturnStop1))
                    },
                    label = { Text("Odkud") },
                    Modifier.fillMaxWidth(),
                )
                TextFieldButton(
                    text = state.settings.destination,
                    onClick = {
                        onEvent(ConnectionSearchEvent.ChoseStop(ChooserType.ReturnStop2))
                    },
                    label = { Text("Kam") },
                    Modifier.fillMaxWidth(),
                )
            }
            IconButton(
                onClick = {},
                enabled = false
            ) {
                IconWithTooltip(Icons.Default.SwapVert, null, tint = MaterialTheme.colorScheme.surface)
            }
        }

        rowItem(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (state.settingsModified) Badge()
                },
            ) {
                OutlinedIconButton(
                    onClick = {
                        showSettings = true
                    }
                ) {
                    IconWithTooltip(Icons.Default.Settings, "Nastavení")
                }
            }
            Button(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    onEvent(ConnectionSearchEvent.Search)
                },
            ) {
                Text("Vyhledat spojení")
            }
            OutlinedIconButton(
                onClick = {
                    onEvent(ConnectionSearchEvent.ClearAll)
                }
            ) {
                IconWithTooltip(Icons.Default.PlaylistRemove, "Vymazat vše")
            }
        }

        if (state.favourites.isNotEmpty()) item {
            Text(text = "Oblíbené:", style = MaterialTheme.typography.titleMedium)
        }

        itemsIndexed(state.favourites) { i, f ->
            FavouriteItem(f, onEvent, i)
        }

        if (state.history.isNotEmpty()) item {
            Text(text = "Historie vyhledávání:", style = MaterialTheme.typography.titleMedium)
        }

        itemsIndexed(state.history) { i, s ->
            HistoryItem(s, onEvent, i)
        }
    }
}

@Composable
private fun FavouriteItem(
    f: Favourite,
    onEvent: (ConnectionSearchEvent) -> Unit,
    i: Int,
) = ListItem(
    headlineContent = {
        Column {
            f.forEach {
                Text(text = "${it.start} -> ${it.destination}")
            }
        }
    },
    Modifier.clickable {
        onEvent(
            ConnectionSearchEvent.SearchFavourite(i)
        )
    },
)

@Suppress("AssignedValueIsNeverRead")
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
private fun HistoryItem(
    s: SearchSettings,
    onEvent: (ConnectionSearchEvent) -> Unit,
    i: Int,
) {
    val datetime =
        "${s.datetime.date.day}. ${s.datetime.date.month.number}. ${s.datetime.time.hour.two()}:${s.datetime.time.minute.two()}"
    val isInFuture = s.datetime > SystemClock.nowHere()
    ListItem(
        headlineContent = {
            Text(text = "${s.start} -> ${s.destination}")
        },
        Modifier.clickable {
            onEvent(
                ConnectionSearchEvent.SearchFromHistory(i, includeDatetime = isInFuture)
            )
        },
        supportingContent = {
            Text(text = datetime)
        },
        trailingContent = {
            var showDetails by remember { mutableStateOf(false) }
            DropdownMenu(
                expanded = showDetails,
                onDismissRequest = { showDetails = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Vyhledat nyní") },
                    onClick = { onEvent(ConnectionSearchEvent.SearchFromHistory(i, includeDatetime = false)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                )
                DropdownMenuItem(
                    text = { Text("Vyhledat $datetime") },
                    onClick = { onEvent(ConnectionSearchEvent.SearchFromHistory(i, includeDatetime = true)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { Icon(Icons.Default.Event, null) }
                )
                DropdownMenuItem(
                    text = { Text("Vyplnit zastávky a nastavení") },
                    onClick = { onEvent(ConnectionSearchEvent.FillFromHistory(i, includeDatetime = false)) },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                )
                DropdownMenuItem(
                    text = { Text("Vyplnit vše") },
                    onClick = { onEvent(ConnectionSearchEvent.FillFromHistory(i, includeDatetime = true)) },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    trailingIcon = { Icon(Icons.Default.Event, null) }
                )
                DropdownMenuItem(
                    text = { Text("Odstranit") },
                    onClick = { onEvent(ConnectionSearchEvent.DeleteFromHistory(i)) },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                )
            }
            IconButton(onClick = { showDetails = true }) {
                IconWithTooltip(Icons.Default.MoreVert, "Další možnosti")
            }
        },
        colors = ListItemDefaults.colors(
            supportingColor = if (isInFuture) LocalContentColor.current else Color.Unspecified,
        )
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TextFieldButton(
    text: StopName,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) = OutlinedTextField(
    value = text,
    onValueChange = {},
    modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (upEvent != null)
                    onClick()
            }
        }
        .semantics {
            role = Role.Button
            onClick {
                onClick()
                true
            }
        },
    readOnly = true,
    label = label,
    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchSettings(
    state: ConnectionSearchState,
    onEvent: (ConnectionSearchEvent) -> Unit,
) = Column(Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Pouze přímá spojení")

        Switch(
            checked = state.settings.directOnly,
            onCheckedChange = { prima ->
                onEvent(ConnectionSearchEvent.SetOnlyDirect(prima))
            },
            thumbContent = {
                IconWithTooltip(
                    if (state.settings.directOnly) Icons.Default.NoTransfer else Icons.Default.TransferWithinAStation,
                    contentDescription = null,
                    modifier = Modifier.padding(2.dp)
                )
            },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Zobrazit nevýhodná spojení")

        Switch(
            checked = state.settings.showInefficientConnections,
            onCheckedChange = { show ->
                onEvent(ConnectionSearchEvent.SetShowInefficientConnections(show))
            },
        )
    }
}