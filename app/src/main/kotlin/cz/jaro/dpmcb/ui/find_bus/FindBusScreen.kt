package cz.jaro.dpmcb.ui.find_bus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.TextField
import cz.jaro.dpmcb.ui.common.autoFocus
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@Composable
fun FindBus(
    args: Route.FindBus,
    navController: NavHostController,
    viewModel: FindBusViewModel = run {
        val navigate = navController.navigateFunction
        koinViewModel {
            ParametersHolder(mutableListOf(navigate, args.date))
        }
    },
) {
    title = R.string.find_bus_by_id
    App.selected = DrawerAction.FindBus

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
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
        Row {
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
        TextField(
            state = state.vehicle,
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = {
                Text("Ev. č. vozu")
            },
            onKeyboardAction = {
                if (state.vehicle.text.isNotEmpty()) {
                    onEvent(FindBusEvent.ConfirmVehicle)
                } else
                    focusManager.moveFocus(FocusDirection.Down)
            },
            keyboardOptions = KeyboardOptions(
                imeAction = if (state.vehicle.text.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                keyboardType = KeyboardType.Number,
            ),
        )
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
        Text(
            text = "Najít kurz",
            Modifier.padding(bottom = 16.dp, top = 16.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        TextField(
            state = state.sequence,
            Modifier
                .fillMaxWidth(),
            label = {
                Text("Linka nebo název kurzu")
            },
            onKeyboardAction = {
                onEvent(FindBusEvent.ConfirmSequence)
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
        )
        Row(
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
            FindBusResult.Offline -> Text("Jste offline!")
            is FindBusResult.LineNotFound -> Text("Linka \"${r.line}\" neexistuje")
            is FindBusResult.InvalidBusName -> Text("Jméno spoje \"${r.name}\" je neplatné")
            is FindBusResult.VehicleNotFound -> Text("Vůz ev. č. \"${r.regN}\" nebyl nalezen.")
            is FindBusResult.SequenceNotFound -> Text("Kurz \"${r.seq}\" neexistuje.")
            is FindBusResult.MoreSequencesFound -> {
                Text("\"${r.seq}\" by mohlo označovat více kurzů, vyberte který jste měli na mysli:")
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
    }
}