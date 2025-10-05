package cz.jaro.dpmcb.ui.find_bus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.helperclasses.rowItem
import cz.jaro.dpmcb.data.helperclasses.textItem
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.TextField
import cz.jaro.dpmcb.ui.common.autoFocus
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route

@Suppress("unused")
@Composable
fun FindBus(
    args: Route.FindBus,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: FindBusViewModel = viewModel(args.date),
) {
    AppState.title = "Najít spoj"
    AppState.selected = DrawerAction.FindBus

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    FindBusScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FindBusScreen(
    state: FindBusState,
    onEvent: (FindBusEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        Modifier
            .padding(horizontal = 16.dp),
        contentPadding = WindowInsets.safeContent.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        item {
            FlowRow(
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = "Najít bus",
                    Modifier.padding(bottom = 16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.weight(1F))
                DateSelector(
                    date = state.date,
                    onDateChange = {
                        onEvent(FindBusEvent.ChangeDate(it))
                    },
                    Modifier.padding(bottom = 8.dp),
                )
            }
        }
        rowItem {
            TextField(
                state = state.line,
                Modifier
                    .weight(1F)
                    .padding(end = 8.dp)
                    .autoFocus(state.date),
                label = {
                    Text("Linka")
                },
                onKeyboardAction = {
                    focusManager.moveFocus(FocusDirection.Right)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
            )
            TextField(
                state = state.number,
                Modifier.weight(1F),
                label = {
                    Text("Č. spoje")
                },
                onKeyboardAction = {
                    if (state.line.text.isNotEmpty() && state.number.text.isNotEmpty())
                        onEvent(FindBusEvent.ConfirmLine)
                    else
                        focusManager.moveFocus(FocusDirection.Down)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = if (state.line.text.isNotEmpty() && state.number.text.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
            )
        }
        item {
            TextField(
                state = state.name,
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = {
                    Text("Jméno spoje")
                },
                onKeyboardAction = {
                    onEvent(FindBusEvent.ConfirmName)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
            )
        }
        item {
            Text(
                text = "Najít kurz",
                Modifier.padding(bottom = 16.dp, top = 16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        item {
            TextField(
                state = state.sequence,
                Modifier
                    .fillMaxWidth(),
                label = {
                    Text("Linka nebo název kurzu")
                },
                onKeyboardAction = {
                    if (state.sequence.text.isNotEmpty()) {
                        onEvent(FindBusEvent.ConfirmSequence)
                    } else
                        focusManager.moveFocus(FocusDirection.Down)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = if (state.sequence.text.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                ),
            )
        }
        item {
            TextField(
                state = state.vehicle,
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = {
                    Text("Ev. č. vozu")
                },
                onKeyboardAction = {
                    onEvent(FindBusEvent.ConfirmVehicle)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Number,
                ),
            )
        }
        rowItem(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    onEvent(FindBusEvent.Confirm)
                },
                Modifier.padding(top = 8.dp),
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
            ) {
                Text("Vyhledat")
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                IconWithTooltip(Icons.Default.Search, null, Modifier.size(ButtonDefaults.IconSize))
            }
        }
        when (val r = state.result) {
            FindBusResult.None -> Unit
            FindBusResult.Offline -> textItem("Jste offline!")
            is FindBusResult.LineNotFound -> textItem("Linka \"${r.line}\" neexistuje")
            is FindBusResult.InvalidBusName -> textItem("Jméno spoje \"${r.name}\" je neplatné")
            is FindBusResult.SequenceNotFound if r.source == SequenceSource.Vehicle -> textItem("Vůz ev. č. \"${r.input}\" nebyl nalezen.")
            is FindBusResult.SequenceNotFound -> textItem("Kurz \"${r.input}\" neexistuje.")
            is FindBusResult.MoreSequencesFound -> {
                if (r.source == SequenceSource.Vehicle) textItem("Vůz ev. č. \"${r.input}\" byl nalezen na následujících kurzech:")
                else textItem("\"${r.input}\" by mohlo označovat více kurzů, vyberte který jste měli na mysli:")
                items(r.sequences) {
                    HorizontalDivider(Modifier.fillMaxWidth())
                    ListItem(
                        headlineContent = {
                            TextButton(
                                onClick = {
                                    onEvent(FindBusEvent.SelectSequence(it.first))
                                },
                                Modifier.fillMaxWidth(),
                            ) {
                                Text(it.second)
                            }
                        },
                        Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}