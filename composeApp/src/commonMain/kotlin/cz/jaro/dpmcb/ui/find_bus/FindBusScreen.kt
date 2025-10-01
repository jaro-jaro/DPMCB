package cz.jaro.dpmcb.ui.find_bus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.durationUntil
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.TextField
import cz.jaro.dpmcb.ui.common.autoFocus
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.datetime.DayOfWeek
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun FindBusScreen(
    state: FindBusState,
    onEvent: (FindBusEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        FlowRow(
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Najít spoj",
                Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.weight(1F))
            DateSelector(
                date = state.date,
                onDateChange = {
                    onEvent(FindBusEvent.ChangeDate(it))
                },
                Modifier.padding(bottom = 16.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    focusManager.moveFocus(FocusDirection.Next)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
            )
            Text("/", Modifier.padding(horizontal = 16.dp))
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
                    imeAction = if (state.line.text.isNotEmpty() && state.number.text.isNotEmpty()) ImeAction.Go else ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
            )
        }
        Text(
            text = "Najít kurz",
            Modifier.padding(bottom = 16.dp, top = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                state = state.sequenceNumber,
                Modifier
                    .weight(1F),
                label = {
                    Text("Číslo kurzu")
                },
                onKeyboardAction = {
                    focusManager.moveFocus(FocusDirection.Next)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
            )
            Text("/", Modifier.padding(horizontal = 16.dp))
            TextField(
                state = state.sequenceLine,
                Modifier
                    .weight(1F),
                label = {
                    Text("Linka")
                },
                onKeyboardAction = {
                    if (state.sequenceLine.text.isNotEmpty()) {
                        onEvent(FindBusEvent.ConfirmSequence)
                    } else
                        focusManager.moveFocus(FocusDirection.Down)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = if (state.sequenceLine.text.isNotEmpty()) ImeAction.Go else ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
            )
        }
        Text(
            text = "Najít vůz",
            Modifier.padding(bottom = 16.dp, top = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
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
        FlowRow(
            Modifier.fillMaxWidth(),
        ) {
            if (SystemClock.todayHere().durationUntil(state.date).inWholeDays in -6L..0L)
                VehicleDownloader(onEvent, state, Modifier.padding(top = 8.dp))
            Spacer(Modifier.weight(1F))
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
            FindBusResult.Offline -> Text("Jste offline!")
            is FindBusResult.LineNotFound -> Text("Linka \"${r.line}\" neexistuje")
            is FindBusResult.SequenceNotFound if r.source == SequenceSource.Vehicle -> Text("Vůz ev. č. \"${r.input}\" nebyl nalezen.")
            is FindBusResult.SequenceNotFound -> Text("Kurz \"${r.input}\" neexistuje.")
            is FindBusResult.SequencesFound -> {
                if (r.source == SequenceSource.Vehicle) Text("Vůz ev. č. \"${r.input}\" byl nalezen na následujících kurzech:")
                r.sequences.forEach {
                    HorizontalDivider(Modifier.fillMaxWidth())
                    ListItem(
                        headlineContent = {
                            TextButton(
                                onClick = {
                                    onEvent(FindBusEvent.SelectSequence(it.first))
                                }
                            ) {
                                Text(it.second)
                            }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun VehicleDownloader(onEvent: (FindBusEvent) -> Unit, state: FindBusState, modifier: Modifier = Modifier) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var fail by rememberSaveable { mutableStateOf(false) }

    if (searching) CircularProgressIndicator(modifier.padding(horizontal = 8.dp))
    else {
        TextButton(
            onClick = {
                fail = false
                searching = true
                onEvent(
                    FindBusEvent.DownloadVehicles(
                        onFail = {
                            searching = false
                            fail = true
                        },
                        onSuccess = {
                            searching = false
                        },
                    )
                )
            },
            modifier,
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        ) {
            Text(
                text = "Stáhnout " + when (SystemClock.todayHere().durationUntil(state.date).inWholeDays) {
                    0L -> "dnešní"
                    -1L -> "včerejší"
                    else -> when (state.date.dayOfWeek) {
                        DayOfWeek.MONDAY -> "pondělní"
                        DayOfWeek.TUESDAY -> "úterní"
                        DayOfWeek.WEDNESDAY -> "středeční"
                        DayOfWeek.THURSDAY -> "čtvrteční"
                        DayOfWeek.FRIDAY -> "páteční"
                        DayOfWeek.SATURDAY -> "sobotní"
                        DayOfWeek.SUNDAY -> "nedělní"
                    }
                } + " vypravenost"
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            IconWithTooltip(Icons.Default.Download, null, Modifier.size(ButtonDefaults.IconSize))
        }
    }
    if (fail) Text("Stahování se nezdařilo")
}