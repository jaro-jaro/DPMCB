package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.common.ChooserResult
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.TextField
import cz.jaro.dpmcb.ui.common.autoFocus
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Suppress("unused")
@Composable
fun Chooser(
    args: Route.Chooser,
    navController: NavHostController,
    superNavController: NavHostController,
    viewModel: ChooserViewModel = koinViewModel {
        parametersOf(
            ChooserViewModel.Parameters(
                type = args.type,
                lineNumber = args.lineNumber,
                stop = args.stop,
                date = args.date,
            )
        )
    },
) {
    AppState.title = when (args.type) {
        ChooserType.Stops -> "Odjezdy"
        ChooserType.Lines -> "Jízdní řády"
        ChooserType.LineStops -> "Jízdní řády"
        ChooserType.NextStop -> "Jízdní řády"
        ChooserType.ReturnStop1 -> "Vyhledat spojení"
        ChooserType.ReturnStop2 -> "Vyhledat spojení"
        ChooserType.ReturnLine -> "Odjezdy"
        ChooserType.ReturnStop -> "Odjezdy"
    }
    AppState.selected = when (args.type) {
        ChooserType.Lines,
        ChooserType.LineStops,
        ChooserType.NextStop,
            -> DrawerAction.Timetable

        ChooserType.Stops,
        ChooserType.ReturnLine,
        ChooserType.ReturnStop,
            -> DrawerAction.Departures

        ChooserType.ReturnStop1,
        ChooserType.ReturnStop2,
            -> null
    }

    LaunchedEffect(Unit) {
        viewModel.navigate = navController.navigateFunction
        viewModel.navigateBack = { it: ChooserResult ->
            navController.previousBackStackEntry?.savedStateHandle?.set("result", it)
            navController.popBackStack()
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ChooserScreen(
        state = state,
        onEvent = viewModel::onEvent,
        navigateUp = navController::navigateUp,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChooserScreen(
    state: ChooserState,
    onEvent: (ChooserEvent) -> Unit,
    navigateUp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Column(
                Modifier
                    .weight(1F)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.info.isNotBlank()) Text(text = state.info, Modifier.padding(vertical = 8.dp))

                Text(
                    text = stringResource(
                        id = when (state.type) {
                            ChooserType.Stops,
                            ChooserType.LineStops,
                            ChooserType.ReturnStop,
                            ChooserType.ReturnStop1,
                            ChooserType.ReturnStop2,
                                -> R.string.vyberte_zastavku

                            ChooserType.Lines,
                            ChooserType.ReturnLine,
                                -> R.string.vyberte_linku

                            ChooserType.NextStop,
                                -> R.string.vyberte_dalsi_zastávku
                        }
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                )
            }

            DateSelector(
                date = state.date,
                onDateChange = {
                    onEvent(ChooserEvent.ChangeDate(it))
                }
            )
        }
        val imeVisible = WindowInsets.isImeVisible
        var wasShown by remember { mutableStateOf(false) }
        LaunchedEffect(imeVisible) {
            when {
                imeVisible -> wasShown = true
                wasShown -> navigateUp()
            }
        }
        TextField(
            state = state.search,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .autoFocus(),
            label = { Text("Vyhledávání") },
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None,
                keyboardType = when (state.type) {
                    ChooserType.Stops,
                    ChooserType.LineStops,
                    ChooserType.NextStop,
                    ChooserType.ReturnStop1,
                    ChooserType.ReturnStop2,
                    ChooserType.ReturnStop,
                        -> KeyboardType.Text

                    ChooserType.Lines,
                    ChooserType.ReturnLine,
                        -> KeyboardType.Number
                },
                imeAction = when (state.type) {
                    ChooserType.Stops,
                    ChooserType.NextStop,
                        -> ImeAction.Search

                    ChooserType.Lines,
                    ChooserType.LineStops,
                        -> ImeAction.Next

                    ChooserType.ReturnStop1,
                    ChooserType.ReturnStop2,
                    ChooserType.ReturnLine,
                    ChooserType.ReturnStop,
                        -> ImeAction.Done
                }
            ),
            onKeyboardAction = {
                onEvent(ChooserEvent.Confirm)
            },
            singleLine = true,
            lineLimits = TextFieldLineLimits.SingleLine,
        )


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            itemsIndexed(state.list, key = { _, it -> it }) { i, item ->
                Surface(
                    onClick = {
                        onEvent(ChooserEvent.ClickedOnListItem(item))
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = CircleShape,
                    color = if (i == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onEvent(ChooserEvent.ClickedOnListItem(item))
                        },
                    )
                }
            }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)))
    }
}

@Preview
@Composable
fun ChooserPreview() {
    MaterialTheme {
        Surface {
            ChooserScreen(
                state = ChooserState(
                    date = SystemClock.todayHere(),
                    type = ChooserType.LineStops,
                    search = TextFieldState("u k"),
                    list = listOf("U Koníčka", "Dobrá Voda, U Kapličky", "Dobrá Voda, U Křížku"),
                    info = "6: ? -> ?",
                ),
                onEvent = {},
                navigateUp = {},
            )
        }
    }
}
