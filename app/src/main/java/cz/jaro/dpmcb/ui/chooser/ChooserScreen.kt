package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalWindowInfo
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
import cz.jaro.dpmcb.data.App.Companion.selected
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.Result
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Chooser(
    navController: NavHostController,
    args: Route.Chooser,
    viewModel: ChooserViewModel = run {
        val navigate = navController.navigateFunction
        koinViewModel {
            parametersOf(ChooserViewModel.Parameters(
                type = args.type,
                lineNumber = args.lineNumber,
                stop = args.stop,
                navigate = navigate,
                navigateBack = { it: Result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("result", it)
                }
            ))
        }
    },
) {
    title = when (args.type) {
        ChooserType.Stops -> R.string.departures
        ChooserType.Lines -> R.string.timetable
        ChooserType.LineStops -> R.string.timetable
        ChooserType.NextStop -> R.string.timetable
        ChooserType.ReturnStop1 -> R.string.vyhledat_spojeni
        ChooserType.ReturnStop2 -> R.string.vyhledat_spojeni
        ChooserType.ReturnLine -> R.string.departures
        ChooserType.ReturnStop -> R.string.departures
    }
    selected = when (args.type) {
        ChooserType.Stops -> DrawerAction.Departures
        ChooserType.Lines -> DrawerAction.Timetable
        ChooserType.LineStops -> DrawerAction.Timetable
        ChooserType.NextStop -> DrawerAction.Timetable
        ChooserType.ReturnStop1 -> null
        ChooserType.ReturnStop2 -> null
        ChooserType.ReturnLine -> DrawerAction.Departures
        ChooserType.ReturnStop -> DrawerAction.Departures
    }

    val search by viewModel.search.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()

    ChooserScreen(
        type = args.type,
        info = viewModel.info,
        search = search,
        wroteSomething = viewModel::wroteSomething,
        clickedEnter = viewModel::confirm,
        clickedOnListItem = viewModel::clickedOnListItem,
        list = list,
    )
}

@Composable
fun ChooserScreen(
    type: ChooserType,
    info: String,
    search: String,
    wroteSomething: (String) -> Unit,
    clickedEnter: () -> Unit,
    clickedOnListItem: (String) -> Unit,
    list: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(text = info, Modifier.padding(horizontal = 16.dp))
        Text(
            text = stringResource(
                id = when (type) {
                    ChooserType.Stops -> R.string.vyberte_zastavku
                    ChooserType.Lines -> R.string.vyberte_linku
                    ChooserType.LineStops -> R.string.vyberte_zastavku
                    ChooserType.NextStop -> R.string.vyberte_dalsi_zastávku
                    ChooserType.ReturnStop1 -> R.string.vyberte_linku
                    ChooserType.ReturnStop2 -> R.string.vyberte_zastavku
                    ChooserType.ReturnLine -> R.string.vyberte_linku
                    ChooserType.ReturnStop -> R.string.vyberte_zastavku
                }
            ),
            Modifier.padding(all = 16.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
        )
        TextField(
            value = search,
            onValueChange = { wroteSomething(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .autoFocus()
                .onKeyEvent {
                    if (it.key == Key.Enter) {
                        clickedEnter()
                    }
                    return@onKeyEvent it.key == Key.Enter
                },
            label = { Text("Vyhledávání") },
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None,
                keyboardType = when (type) {
                    ChooserType.Stops -> KeyboardType.Text
                    ChooserType.Lines -> KeyboardType.Number
                    ChooserType.LineStops -> KeyboardType.Text
                    ChooserType.NextStop -> KeyboardType.Text
                    ChooserType.ReturnStop1 -> KeyboardType.Text
                    ChooserType.ReturnStop2 -> KeyboardType.Text
                    ChooserType.ReturnLine -> KeyboardType.Number
                    ChooserType.ReturnStop -> KeyboardType.Text
                },
                imeAction = when (type) {
                    ChooserType.Stops -> ImeAction.Search
                    ChooserType.Lines -> ImeAction.Next
                    ChooserType.LineStops -> ImeAction.Next
                    ChooserType.NextStop -> ImeAction.Search
                    ChooserType.ReturnStop1 -> ImeAction.Done
                    ChooserType.ReturnStop2 -> ImeAction.Done
                    ChooserType.ReturnLine -> ImeAction.Done
                    ChooserType.ReturnStop -> ImeAction.Done
                }
            ),
            keyboardActions = KeyboardActions {
                clickedEnter()
            },
            singleLine = true,
            maxLines = 1,
        )


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            itemsIndexed(list) { i, item ->
                Surface(
                    onClick = {
                        clickedOnListItem(item)
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    shape = CircleShape,
                    color = if (i == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            clickedOnListItem(item)
                        },
                    )
                }
            }
        }
    }
}

fun Modifier.autoFocus() = composed {

    val focusRequester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current

    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { isWindowFocused ->
            if (isWindowFocused) {
                awaitFrame()
                delay(250)
                focusRequester.requestFocus()
            }
        }
    }

    focusRequester(focusRequester)
}

@Preview
@Composable
fun ChooserPreview() {
    MaterialTheme {
        Surface {
            ChooserScreen(
                type = ChooserType.Stops,
                info = "",
                search = "u ko",
                wroteSomething = {},
                clickedEnter = {},
                clickedOnListItem = {},
                list = listOf("U Koníčka"),
            )
        }
    }
}
