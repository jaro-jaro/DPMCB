package cz.jaro.dpmcb.ui.timetable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App.Companion.selected
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.navigateFunction
import cz.jaro.dpmcb.data.realtions.timetable.BusInTimetable
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Timetable(
    args: Route.Timetable,
    navController: NavHostController,
    superNavController: NavHostController,
) {
    title = R.string.timetable
    selected = DrawerAction.Timetable

    val viewModel: TimetableViewModel = koinViewModel {
        parametersOf(
            TimetableViewModel.Parameters(
                lineNumber = args.lineNumber,
                stop = args.stop,
                nextStop = args.nextStop,
                date = args.date,
                navigate = navController.navigateFunction,
            )
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    TimetableScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}


@Composable
fun TimetableScreen(
    state: TimetableState,
    onEvent: (TimetableEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        ) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.lineNumber.toString(),
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${state.stop} -> ${state.nextStop}",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        var showLowFloor by remember(state.showLowFloorFromLastTime) { mutableStateOf(state.showLowFloorFromLastTime) }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = showLowFloor, onCheckedChange = {
                showLowFloor = it
                onEvent(TimetableEvent.EditShowLowFloor(it))
            })
            Text("Zobrazit nízkopodlažnost", Modifier.clickable {
                showLowFloor = !showLowFloor
                onEvent(TimetableEvent.EditShowLowFloor(showLowFloor))
            })

            Spacer(Modifier.weight(1F))

            DateSelector(
                date = state.date,
                onDateChange = {
                    onEvent(TimetableEvent.ChangeDate(it))
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (state) {
            is TimetableState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator()
            }

            is TimetableState.Success -> Row(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState())
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                ) {
                    repeat(24) { h ->
                        Box(
                            modifier = Modifier
                                .weight(1F)
                                .fillMaxWidth()
                                .padding(4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = h.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .horizontalScroll(state = rememberScrollState())
                        .fillMaxWidth()
                ) {
                    repeat(24) { h ->
                        DepartureRow(
                            onEvent = onEvent,
                            result = state.data.filter { it.departure.hour == h },
                            showLowFloor = showLowFloor,
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.DepartureRow(
    onEvent: (TimetableEvent) -> Unit,
    result: List<BusInTimetable>,
    showLowFloor: Boolean,
) {
    Row(
        modifier = Modifier.weight(1F),
    ) {
        if (result.isEmpty())
            Text(
                text = "",
                modifier = Modifier
                    .padding(4.dp),
                color = Color.Transparent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        result.forEach { (time, lowFloor, busName, destination) ->
            var showDropDown by rememberSaveable { mutableStateOf(false) }
            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = {
                    showDropDown = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("-> $destination")
                    },
                    onClick = {},
                    enabled = false
                )
                DropdownMenuItem(
                    text = {
                        Text("Detail spoje")
                    },
                    onClick = {
                        onEvent(TimetableEvent.GoToBus(busName))
                        showDropDown = false
                    },
                )
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            onEvent(TimetableEvent.GoToBus(busName))
                        },
                        onLongClick = {
                            showDropDown = true
                        }
                    )
                    .padding(4.dp)
                    .requiredSizeIn(minHeight = 32.dp, minWidth = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = time.minute.let { if ("$it".length <= 1) "0$it" else "$it" },
                    color = if (showLowFloor && lowFloor) MaterialTheme.colorScheme.tertiary else Color.Unspecified,
                    fontSize = 20.sp,
                    fontWeight = if (showLowFloor && lowFloor) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
