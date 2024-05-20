package cz.jaro.dpmcb.ui.departures

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.marosseleng.compose.material3.datetimepickers.time.ui.dialog.TimePickerDialog
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.Result
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.now
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.departures.DeparturesEvent.Canceled
import cz.jaro.dpmcb.ui.departures.DeparturesEvent.ChangeCompactMode
import cz.jaro.dpmcb.ui.departures.DeparturesEvent.ChangeJustDepartures
import cz.jaro.dpmcb.ui.departures.DeparturesEvent.ChangeTime
import cz.jaro.dpmcb.ui.departures.DeparturesEvent.WentBack
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun Departures(
    args: Route.Departures,
    viewModel: DeparturesViewModel = koinViewModel {
        parametersOf(
            DeparturesViewModel.Parameters(
                stop = args.stop,
                time = args.time ?: now,
                line = args.line,
                via = args.via
            )
        )
    },
    navController: NavHostController,
) {
    LifecycleResumeEffect(Unit) {
        val result = navController.currentBackStackEntry?.savedStateHandle?.get<Result>("result")

        if (result != null) viewModel.onEvent(WentBack(result))

        onPauseOrDispose {
            navController.currentBackStackEntry?.savedStateHandle?.remove<Result>("result")
        }
    }

    title = R.string.departures
    App.selected = DrawerAction.Departures

    val info by viewModel.info.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val listState = rememberLazyListState(info.scrollIndex)

    LaunchedEffect(Unit) {
        viewModel.scroll = {
            delay(500)
            listState.scrollToItem(it)
        }
    }
    viewModel.navigate = navController.navigateFunction

    LaunchedEffect(listState) {
        withContext(Dispatchers.IO) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .flowOn(Dispatchers.IO)
                .collect {
                    viewModel.onEvent(DeparturesEvent.Scroll(it))
                }
        }
    }

    val isOnline by viewModel.hasMapAccess.collectAsStateWithLifecycle()
    val date by viewModel.datum.collectAsStateWithLifecycle()

    DeparturesScreen(
        info = info,
        state = state,
        stop = args.stop,
        onEvent = viewModel::onEvent,
        listState = listState,
        date = date,
        navigate = navController.navigateFunction,
        isOnline = isOnline,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(
    state: DeparturesState,
    info: DeparturesInfo,
    stop: String,
    listState: LazyListState,
    isOnline: Boolean,
    date: LocalDate,
    onEvent: (DeparturesEvent) -> Unit,
    navigate: NavigateFunction,
) = Scaffold(
    floatingActionButton = {
        if (state is DeparturesState.Runs) {
            val scope = rememberCoroutineScope()
            val i by remember { derivedStateOf { listState.firstVisibleItemIndex } }
            val jeDole = !listState.canScrollForward

            val hideBecauseTooLow = jeDole && i < state.line.home(info)
            val hideBecauseNear = abs(i - state.line.home(info)) <= 1

            AnimatedVisibility(
                visible = !hideBecauseNear && !hideBecauseTooLow,
                enter = fadeIn(spring(stiffness = Spring.StiffnessVeryLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessVeryLow)),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch(Dispatchers.Main) {
                            listState.animateScrollToItem(state.line.home(info))
                        }
                    },
                ) {
                    IconWithTooltip(
                        Icons.Default.ArrowUpward,
                        "Scrollovat",
                        Modifier.rotate(
                            animateFloatAsState(
                                when {
                                    i > state.line.home(info) -> 0F
                                    i < state.line.home(info) -> 180F
                                    else -> 90F
                                }, label = "TOČENÍ"
                            ).value
                        )
                    )
                }
            }
        }
    }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    navigate(Route.Chooser(ChooserType.Stops))
                },
            ) {
                Text(
                    text = stop,
                    fontSize = 20.sp
                )
            }
            var showDialog by rememberSaveable { mutableStateOf(false) }
            if (showDialog) TimePickerDialog(
                onDismissRequest = {
                    showDialog = false
                },
                onTimeChange = {
                    onEvent(ChangeTime(it))
                    showDialog = false
                },
                title = {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Změnit čas")
                        TextButton(
                            onClick = {
                                onEvent(ChangeTime(now))
                                showDialog = false
                            }
                        ) {
                            Text("Teď")
                        }
                    }
                },
                initialTime = info.time,
            )

            TextButton(
                onClick = {
                    showDialog = true
                }
            ) {
                Text(text = info.time.toString())
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOnline) Surface(
                checked = info.compactMode,
                onCheckedChange = {
                    onEvent(ChangeCompactMode)
                },
                Modifier.padding(all = 8.dp),
                shape = CircleShape,
            ) {
                Row(
                    Modifier.padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = info.compactMode, onCheckedChange = {
                        onEvent(ChangeCompactMode)
                    })
                    Text("Zjednodušit")
                }
            }

            Spacer(modifier = Modifier.weight(1F))

            Surface(
                checked = info.justDepartures,
                onCheckedChange = {
                    onEvent(ChangeJustDepartures)
                },
                Modifier.padding(all = 8.dp),
                shape = CircleShape,
            ) {
                Row(
                    Modifier.padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = info.justDepartures, onCheckedChange = {
                        onEvent(ChangeJustDepartures)
                    })
                    Text("Pouze odjezdy")
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            val lineSource = remember { MutableInteractionSource() }
            val containerColor = MaterialTheme.colorScheme.surfaceVariant
            TextField(
                value = info.lineFilter?.toString() ?: "Všechny",
                onValueChange = {},
                Modifier
                    .fillMaxWidth(),
                label = {
                    Text(text = "Linka:")
                },
                interactionSource = lineSource,
                readOnly = true,
                trailingIcon = {
                    if (info.lineFilter != null) IconButton(onClick = {
                        onEvent(Canceled(ChooserType.ReturnLine))
                    }) {
                        IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = info.lineFilter?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = info.lineFilter?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    disabledContainerColor = containerColor,
                ),
            )
            val linePressedState by lineSource.interactions.collectAsStateWithLifecycle(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
            if (linePressedState is PressInteraction.Release) {
                navigate(
                    Route.Chooser(
                        type = ChooserType.ReturnLine,
                    )
                )
                lineSource.tryEmit(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
            }
            val stopSource = remember { MutableInteractionSource() }
            TextField(
                value = info.stopFilter ?: "Cokoliv",
                onValueChange = {},
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),

                label = {
                    Text(text = "Pojede přes:")
                },
                readOnly = true,
                trailingIcon = {
                    if (info.stopFilter != null) IconButton(onClick = {
                        onEvent(Canceled(ChooserType.ReturnStop))
                    }) {
                        IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                    }
                },
                interactionSource = stopSource,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = info.stopFilter?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = info.stopFilter?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    disabledContainerColor = containerColor,
                ),
            )
            val stopPressedState by stopSource.interactions.collectAsStateWithLifecycle(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
            if (stopPressedState is PressInteraction.Release) {
                navigate(
                    Route.Chooser(
                        type = ChooserType.ReturnStop,
                    )
                )
                stopSource.tryEmit(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
            }
        }
        when (state) {
            DeparturesState.Loading -> Row(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            is DeparturesState.NothingRuns -> Row(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    when (state) {
                        DeparturesState.NothingRunsAtAll -> "Přes tuto zastávku ${date.toCzechLocative()} nic nejede"
                        DeparturesState.NothingRunsHere -> "Přes tuto zastávku nejede žádný spoj, který bude zastavovat na zastávce ${info.stopFilter}"
                        DeparturesState.LineDoesNotRun -> "Přes tuto zastávku nejede žádný spoj linky ${info.lineFilter}"
                        DeparturesState.LineDoesNotRunHere -> "Přes tuto zastávku nejede žádný spoj linky ${info.lineFilter}, který bude zastavovat na zastávce ${info.stopFilter}"
                    },
                    Modifier.padding(horizontal = 16.dp)
                )
            }

            is DeparturesState.Runs -> LazyColumn(
                state = listState,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                items(
                    count = state.line.size + 2,
                    key = { i ->
                        when (i) {
                            0 -> 0
                            state.line.lastIndex + 2 -> Int.MAX_VALUE
                            else -> state.line[i - 1].busId to state.line[i - 1].time
                        }
                    },
                    itemContent = { i ->
                        when (i) {
                            0 -> {
                                Surface(
                                    modifier = Modifier.clickable {
                                        onEvent(DeparturesEvent.PreviousDay)
                                    },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .weight(1F),
                                            text = "Předchozí den…",
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }

                            state.line.lastIndex + 2 -> {
                                HorizontalDivider()
                                Surface(
                                    modifier = Modifier.clickable {
                                        onEvent(DeparturesEvent.NextDay)
                                    },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .weight(1F),
                                            text = "Následující den…",
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }

                            else -> Card(
                                state.line[i - 1], onEvent, info.compactMode, modifier = Modifier
                                    .animateContentSize()
//                                    .animateItemPlacement(spring(stiffness = Spring.StiffnessMediumLow))
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Card(
    departureState: DepartureState,
    onEvent: (DeparturesEvent) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    var showDropDown by rememberSaveable { mutableStateOf(false) }
    HorizontalDivider()
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = {
                onEvent(DeparturesEvent.GoToBus(departureState))
            },
            onLongClick = {
                showDropDown = true
            }
        )
    ) {
        departureState.nextStop?.let {
            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = {
                    showDropDown = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Detail spoje")
                    },
                    onClick = {
                        onEvent(DeparturesEvent.GoToBus(departureState))
                        showDropDown = false
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text("Zobrazit zastávkové JŘ")
                    },
                    onClick = {
                        onEvent(DeparturesEvent.GoToTimetable(departureState))
                        showDropDown = false
                    },
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            val nextStop = departureState.currentNextStop
            val delay = departureState.delay
            val runsIn = departureState.runsIn

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                IconWithTooltip(
                    when {
                        departureState.confirmedLowFloor == true -> Icons.AutoMirrored.Filled.Accessible
                        departureState.confirmedLowFloor == false -> Icons.Default.NotAccessible
                        departureState.lowFloor -> Icons.AutoMirrored.Filled.Accessible
                        else -> Icons.Default.NotAccessible
                    },
                    when {
                        departureState.confirmedLowFloor == true -> "Potvrzený nízkopodlažní vůz"
                        departureState.confirmedLowFloor == false -> "Potvrzený vysokopodlažní vůz"
                        departureState.lowFloor -> "Plánovaný nízkopodlažní vůz"
                        else -> "Nezaručený nízkopodlažní vůz"
                    },
                    tint = when {
                        departureState.confirmedLowFloor == false && departureState.lowFloor -> MaterialTheme.colorScheme.error
                        departureState.confirmedLowFloor != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = departureState.lineNumber.toString(),
                    fontSize = 30.sp
                )
                Text(
                    modifier = Modifier,
                    text = " -> ",
                    fontSize = 20.sp
                )
                Text(
                    modifier = Modifier
                        .weight(1F),
                    text = departureState.destination,
                    fontSize = 20.sp
                )

                if (compact) {
                    Text(
                        text = if (runsIn < Duration.ZERO || (runsIn > Duration.ofHours(1L) && delay == null)) "${departureState.time}" else runsIn.asString(),
                        color = if (delay == null || runsIn < Duration.ZERO) MaterialTheme.colorScheme.onSurface else colorOfDelayText(delay),
                    )
                } else {
                    Text(
                        text = "${departureState.time}"
                    )
                    if (delay != null && runsIn > Duration.ZERO) Text(
                        text = "${departureState.time.plusMinutes(delay.toLong())}",
                        color = colorOfDelayText(delay),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (nextStop != null && delay != null && !compact) {
                Text(text = "Následující zastávka:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1F),
                        text = nextStop.first,
                        fontSize = 20.sp
                    )
                    Text(
                        text = nextStop.second.plusMinutes(delay.roundToLong()).toString(),
                        color = colorOfDelayText(delay)
                    )
                }
            }
        }
    }
}

fun Duration.asString(): String {
    val hours = toHours()
    val minutes = (toMinutes() % 60).toInt()

    return when {
        hours == 0L && minutes == 0 -> "<1 min"
        hours == 0L -> "$minutes min"
        minutes == 0 -> "$hours hod"
        else -> "$hours hod $minutes min"
    }
}
