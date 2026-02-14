package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.TextField
import cz.jaro.dpmcb.ui.common.autoFocus
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlin.time.ExperimentalTime

@Suppress("unused")
@Composable
fun Chooser(
    args: Route.Chooser,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: ChooserViewModel = viewModel(
        ChooserViewModel.Parameters(
            type = args.type,
            lineNumber = args.lineNumber,
            stop = args.stop,
            date = args.date,
        )
    ),
) {
    AppState.title = when (args.type) {
        ChooserType.Lines,
        ChooserType.LineStops,
        ChooserType.Platforms,
             -> "Jízdní řády"
        ChooserType.ReturnStop1,
        ChooserType.ReturnStop2,
            -> "Vyhledat spojení"
        ChooserType.Stops,
        ChooserType.ReturnLine,
        ChooserType.ReturnStop,
        ChooserType.ReturnStopVia,
        ChooserType.ReturnPlatform,
            -> "Odjezdy"
    }
    AppState.selected = when (args.type) {
        ChooserType.Lines,
        ChooserType.LineStops,
        ChooserType.Platforms,
            -> DrawerAction.Timetable

        ChooserType.Stops,
        ChooserType.ReturnLine,
        ChooserType.ReturnStop,
        ChooserType.ReturnStopVia,
        ChooserType.ReturnPlatform,
            -> DrawerAction.Departures

        ChooserType.ReturnStop1,
        ChooserType.ReturnStop2,
            -> DrawerAction.Connection
    }

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ChooserScreen(
        state = state,
        onEvent = viewModel::onEvent,
//        navigateUp = navigator::navigateUp,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChooserScreen(
    state: ChooserState,
    onEvent: (ChooserEvent) -> Unit,
//    navigateUp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        val dateDialogShown = rememberSaveable { mutableStateOf(false) }
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
                    text = when (state.type) {
                        ChooserType.Stops,
                        ChooserType.LineStops,
                        ChooserType.ReturnStop,
                        ChooserType.ReturnStop1,
                        ChooserType.ReturnStop2,
                        ChooserType.ReturnStopVia,
                            -> "Vyberte zastávku"

                        ChooserType.Lines,
                        ChooserType.ReturnLine,
                            -> "Vyberte linku"

                        ChooserType.Platforms,
                        ChooserType.ReturnPlatform,
                            -> "Vyberte stanoviště"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                )
            }

            DateSelector(
                date = state.date,
                onDateChange = {
                    onEvent(ChooserEvent.ChangeDate(it))
                },
                showDialog = dateDialogShown,
            )
        }
//        val imeVisible = isImeVisible
//        var wasShown by remember { mutableStateOf(false) }
//        LaunchedEffect(imeVisible) {
//            when {
//                imeVisible -> wasShown = true
//                wasShown && AppState.menuState == DrawerValue.Closed && !dateDialogShown.value -> navigateUp()
//            }
//        }
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
                    ChooserType.Platforms,
                    ChooserType.ReturnStop1,
                    ChooserType.ReturnStop2,
                    ChooserType.ReturnStop,
                    ChooserType.ReturnStopVia,
                    ChooserType.ReturnPlatform,
                        -> KeyboardType.Text

                    ChooserType.Lines,
                    ChooserType.ReturnLine,
                        -> KeyboardType.Number
                },
                imeAction = when (state.type) {
                    ChooserType.Stops,
                    ChooserType.Platforms,
                        -> ImeAction.Search

                    ChooserType.Lines,
                    ChooserType.LineStops,
                        -> ImeAction.Next

                    ChooserType.ReturnStop1,
                    ChooserType.ReturnStop2,
                    ChooserType.ReturnLine,
                    ChooserType.ReturnStop,
                    ChooserType.ReturnStopVia,
                    ChooserType.ReturnPlatform,
                        -> ImeAction.Done
                },
            ),
            onKeyboardAction = {
                onEvent(ChooserEvent.Confirm)
            },
            singleLine = true,
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = WindowInsets.safeContent.only(WindowInsetsSides.Bottom).asPaddingValues(),
        ) {
            itemsIndexed(state.list, key = { _, it -> it.textValue }) { i, item ->
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
                        text = { Text(item.textValue) },
                        onClick = {
                            onEvent(ChooserEvent.ClickedOnListItem(item))
                        },
                        contentPadding = PaddingValues(12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
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
                    list = listOf("U Koníčka", "Dobrá Voda, U Kapličky", "Dobrá Voda, U Křížku").map { ChooserViewModel.Item(it, StopName("", "", it)) },
                    info = "6: ? -> ?",
                ),
                onEvent = {},
//                navigateUp = {},
            )
        }
    }
}
