package cz.jaro.dpmcb.ui.departures

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.entities.isInvalid
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.nowFlow
import cz.jaro.dpmcb.data.helperclasses.onSecondaryClick
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.timeFlow
import cz.jaro.dpmcb.data.helperclasses.toCzechLocative
import cz.jaro.dpmcb.data.helperclasses.truncatedToSeconds
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.ChooserResult
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.StopTypeIcon
import cz.jaro.dpmcb.ui.common.VehicleIcon
import cz.jaro.dpmcb.ui.common.toLocalTime
import cz.jaro.dpmcb.ui.departures.DeparturesEvent.ChangeTime
import cz.jaro.dpmcb.ui.departures.DeparturesEvent.WentBack
import cz.jaro.dpmcb.ui.departures.DeparturesState.NothingRunsReason.LineDoesNotRun
import cz.jaro.dpmcb.ui.departures.DeparturesState.NothingRunsReason.LineDoesNotRunHere
import cz.jaro.dpmcb.ui.departures.DeparturesState.NothingRunsReason.NothingRunsAtAll
import cz.jaro.dpmcb.ui.departures.DeparturesState.NothingRunsReason.NothingRunsHere
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.getResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atDate
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
@Composable
fun Departures(
    args: Route.Departures,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: DeparturesViewModel = viewModel(
        DeparturesViewModel.Parameters(
            stop = args.stop,
            time = args.time.takeUnless { it.isInvalid() }?.toLocalTime(),
            line = args.line.takeUnless { it.isInvalid() },
            via = args.via,
            onlyDepartures = args.onlyDepartures,
            simple = args.simple,
            date = args.date,
        )
    ),
) {
    AppState.title = "Odjezdy"
    AppState.selected = DrawerAction.Departures

    LifecycleResumeEffect(Unit) {
        val result = navigator.getResult<ChooserResult>()

        if (result != null) viewModel.onEvent(WentBack(result))

        onPauseOrDispose {
            navigator.clearResult()
        }
    }

    val info by viewModel.info.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val listState = rememberLazyListState(remember { info.scrollIndex })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.setScroll { i, animate ->
            scope.launch {
                if (listState.firstVisibleItemIndex != i)
                    if (animate)
                        listState.scrollToItem(i)
                    else
                        listState.animateScrollToItem(i)
            }
        }
        viewModel.navigator = navigator
    }

    LaunchedEffect(listState) {
        withContext(Dispatchers.IO) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .flowOn(Dispatchers.IO)
                .collect {
                    viewModel.onEvent(DeparturesEvent.Scroll(it))
                }
        }
    }

    DeparturesScreen(
        state = state,
        onEvent = viewModel::onEvent,
        listState = listState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(
    state: DeparturesState,
    listState: LazyListState,
    onEvent: (DeparturesEvent) -> Unit,
) = Scaffold(
    floatingActionButton = {
        if (state is DeparturesState.Runs) {
            val scope = rememberCoroutineScope()
            val i = listState.firstVisibleItemIndex
            val isAtBottom by remember { derivedStateOf { !listState.canScrollForward } }
            val now by timeFlow.collectAsStateWithLifecycle()
            val home by derivedStateOf { state.departures.indexOfNext(state.info.time ?: now, now) }
            val hideBecauseTooLow by remember(home, i, isAtBottom) { derivedStateOf { isAtBottom && i < home } }
            val hideBecauseNear by remember(home, i) { derivedStateOf { (i - home).absoluteValue <= 1 } }

            AnimatedVisibility(
                visible = !hideBecauseNear && !hideBecauseTooLow,
                enter = fadeIn(spring(stiffness = Spring.StiffnessVeryLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessVeryLow)),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(home)
                        }
                    },
                ) {
                    IconWithTooltip(
                        Icons.Default.ArrowUpward,
                        "Scrollovat",
                        Modifier.rotate(
                            animateFloatAsState(
                                remember(i, home) {
                                    when {
                                        i > home -> 0F
                                        i < home -> 180F
                                        else -> 90F
                                    }
                                }, label = "TOČENÍ"
                            ).value
                        )
                    )
                }
            }
        }
    },
    contentWindowInsets = WindowInsets(0)
) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TextButton(
            onClick = {
                onEvent(DeparturesEvent.ChangeStop)
            },
            Modifier
        ) {
            Text(
                text = state.info.stop,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            IconWithTooltip(Icons.Default.Edit, "Změnit zastávku")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            DateSelector(
                date = state.info.date,
                onDateChange = {
                    onEvent(DeparturesEvent.ChangeDate(it))
                },
            )
            var showDialog by rememberSaveable { mutableStateOf(false) }
            val now by timeFlow.collectAsStateWithLifecycle()
            val time = state.info.time ?: now
            val timeState = rememberTimePickerState(
                initialHour = time.hour,
                initialMinute = time.minute,
                is24Hour = true,
            )
            if (showDialog) TimePickerDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        val time = LocalTime(timeState.hour, timeState.minute)
                        onEvent(ChangeTime(time))
                    }) { Text("OK") }
                },
                dismissButton = { TextButton({ showDialog = false }) { Text("Zrušit") } },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Změnit čas")
                    TextButton(
                        onClick = {
                            onEvent(ChangeTime(null))
                            showDialog = false
                        }
                    ) {
                        Text("Teď")
                    }
                }
                TimePicker(timeState)
            }

            TextButton(
                onClick = {
                    showDialog = true
                },
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 12.dp),
            ) {
                IconWithTooltip(Icons.Default.AccessTime, "Změnit čas", Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = state.info.time?.toString() ?: "Nyní")
            }
        }

        LazyRow(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.info.compactMode,
                    onClick = {
                        onEvent(DeparturesEvent.ChangeCompactMode)
                    },
                    label = {
                        Text("Zjednodušit")
                    },
                    leadingIcon = {
                        if (state.info.compactMode) Icon(Icons.Default.Check, null)
                    },
                )
            }
            item {
                FilterChip(
                    selected = state.info.justDepartures,
                    onClick = {
                        onEvent(DeparturesEvent.ChangeJustDepartures)
                    },
                    label = {
                        Text("Pouze odjezdy")
                    },
                    leadingIcon = {
                        if (state.info.justDepartures) Icon(Icons.Default.Check, null)
                    },
                )
            }
            item {
                FilterChip(
                    selected = state.info.stopFilter != null,
                    onClick = {
                        onEvent(
                            if (state.info.stopFilter != null)
                                DeparturesEvent.Canceled(ChooserType.ReturnStop)
                            else DeparturesEvent.ChangeVia
                        )
                    },
                    label = {
                        Text(state.info.stopFilter?.let { "Pojede přes: $it" } ?: "Pojede přes")
                    },
                    leadingIcon = {
                        if (state.info.lineFilter != null) Icon(Icons.Default.Check, null)
                    },
                    trailingIcon = {
                        if (state.info.lineFilter == null) Icon(Icons.Default.ArrowDropDown, null)
                    },
                )
            }
            item {
                FilterChip(
                    selected = state.info.lineFilter != null,
                    onClick = {
                        onEvent(
                            if (state.info.lineFilter != null)
                                DeparturesEvent.Canceled(ChooserType.ReturnLine)
                            else DeparturesEvent.ChangeLine
                        )
                    },
                    label = {
                        Text(state.info.lineFilter?.let { "Linka: $it" } ?: "Linka")
                    },
                    leadingIcon = {
                        if (state.info.lineFilter != null) Icon(Icons.Default.Check, null)
                    },
                    trailingIcon = {
                        if (state.info.lineFilter == null) Icon(Icons.Default.ArrowDropDown, null)
                    },
                )
            }
        }
        when (state) {
            is DeparturesState.Loading -> Row(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            is DeparturesState.Loaded -> LazyColumn(
                state = listState,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                item { // Seen in DeparturesStateKt::indexOfNext
                    HorizontalDivider()
                    DaySwitcher(onEvent, DeparturesEvent.PreviousDay, "Předchozí den…")
                }
                if (state is DeparturesState.NothingRuns) item {
                    HorizontalDivider()
                    Row(
                        Modifier
                            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            when (state.reason) {
                                NothingRunsAtAll -> "Přes tuto zastávku ${state.info.date.toCzechLocative()} nic nejede"
                                NothingRunsHere -> "Přes tuto zastávku nejede žádný spoj, který bude zastavovat na zastávce ${state.info.stopFilter}"
                                LineDoesNotRun -> "Přes tuto zastávku nejede žádný spoj linky ${state.info.lineFilter}"
                                LineDoesNotRunHere -> "Přes tuto zastávku nejede žádný spoj linky ${state.info.lineFilter}, který bude zastavovat na zastávce ${state.info.stopFilter}"
                            },
                            Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                else if (state is DeparturesState.Runs) items(
                    items = state.departures,
                    key = { state ->
                        state.busName to state.time
                    },
                ) { itemState ->
                    Card(
                        itemState, state.info, onEvent, Modifier
                            .animateContentSize()
                            .animateItem()
//                            .animateItemPlacement(spring(stiffness = Spring.StiffnessMediumLow))
                    )
                }
                item {
                    HorizontalDivider()
                    DaySwitcher(onEvent, DeparturesEvent.NextDay, "Následující den…")
                    HorizontalDivider()
                }
            }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)))
    }
}

@Composable
private fun DaySwitcher(onEvent: (DeparturesEvent) -> Unit, event: DeparturesEvent, text: String) {
    Surface(
        modifier = Modifier.clickable {
            onEvent(event)
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
                text = text,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Card(
    departureState: DepartureState,
    info: DeparturesInfo,
    onEvent: (DeparturesEvent) -> Unit,
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
        ).onSecondaryClick(Unit) {
            showDropDown = true
        }
    ) {
        departureState.directionIfNotLast?.let {
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
            val now by nowFlow.collectAsStateWithLifecycle()
            val delay = departureState.delay.takeUnless { info.date != now.date }
            val runsIn = departureState.time.atDate(info.date) + (delay ?: 0.minutes) - now

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                VehicleIcon(departureState.lineTraction, departureState.vehicleTraction)
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

                if (departureState.stopType != StopType.Normal) StopTypeIcon(departureState.stopType, Modifier.padding(horizontal = 8.dp))

                if (info.compactMode) {
                    Text(
                        text = if (runsIn < Duration.ZERO || (runsIn > 1.hours && delay == null)) "${departureState.time}" else runsIn.asString(),
                        color = if (delay == null || runsIn < Duration.ZERO) MaterialTheme.colorScheme.onSurface else colorOfDelayText(delay),
                    )
                } else {
                    Text(
                        text = "${departureState.time}"
                    )
                    if (delay != null && runsIn > Duration.ZERO) Text(
                        text = "${departureState.time + delay.truncatedToSeconds()}",
                        color = colorOfDelayText(delay),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (nextStop != null && !info.compactMode) {
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
                    if (delay != null) Text(
                        text = (nextStop.second + delay.truncatedToSeconds()).toString(),
                        color = colorOfDelayText(delay)
                    )
                    else Text(nextStop.second.toString())
                }
            }
        }
    }
}

fun Duration.asString(): String {
    val hours = inWholeHours
    val minutes = (inWholeMinutes % 60).toInt()

    return when {
        hours == 0L && minutes == 0 -> "<1 min"
        hours == 0L -> "$minutes min"
        minutes == 0 -> "$hours hod"
        else -> "$hours hod $minutes min"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit,
) = BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier.wrapContentHeight(),
    properties = properties,
) {
    Surface(
        modifier = Modifier
            .requiredWidth(360.0.dp)
            .heightIn(min = 40.0.dp),
        shape = shape,
        color = colors.containerColor,
        tonalElevation = tonalElevation,
    ) {
        Column(
            Modifier.padding(all = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
            Box(
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                val mergedStyle = LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge)
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.primary,
                    LocalTextStyle provides mergedStyle,
                ) {
                    Row(modifier = Modifier.height(40.dp).fillMaxWidth()) {
                        dismissButton?.invoke()
                        Spacer(modifier = Modifier.weight(1f))
                        confirmButton()
                    }
                }
            }
        }
    }
}