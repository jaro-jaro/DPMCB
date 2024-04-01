package cz.jaro.dpmcb.ui.bus

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.PartOfConn
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.Offset
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechAccusative
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.work
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.LineTimeNameConnIdNextStop
import cz.jaro.dpmcb.ui.destinations.DeparturesDestination
import cz.jaro.dpmcb.ui.destinations.TimetableDestination
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.sequence.DelayBubble
import cz.jaro.dpmcb.ui.sequence.Name
import cz.jaro.dpmcb.ui.sequence.Vehicle
import cz.jaro.dpmcb.ui.sequence.Wheelchair
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import java.time.LocalDate
import java.time.LocalTime

@Destination
@Composable
fun Bus(
    busId: String,
    navigator: DestinationsNavigator,
    viewModel: BusViewModel = run {
        val navigate = navigator.navigateFunction
        koinViewModel {
            ParametersHolder(mutableListOf(busId, navigate))
        }
    },
) {
    title = R.string.detail_spoje
    App.selected = DrawerAction.FindBus

    val state by viewModel.state.collectAsStateWithLifecycle()

    BusScreen(
        state = state,
        navigate = navigator.navigateFunction,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun BusScreen(
    state: BusState,
    navigate: NavigateFunction,
    onEvent: (BusEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        when (state) {
            is BusState.Loading -> Loading()

            is BusState.DoesNotExist -> DoesNotExist(state.busId)

            is BusState.DoesNotRun -> {
                Errors(state.runsNextTimeAfterToday, onEvent, state.runsNextTimeAfterDate, state.busId, state.date)

                CodesAndShare(state)
            }

            is BusState.OK -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Name("${state.lineNumber}")
                    Wheelchair(
                        lowFloor = state.lowFloor,
                        confirmedLowFloor = (state as? BusState.OnlineRunning)?.confirmedLowFloor,
                        Modifier.padding(start = 8.dp),
                        enableCart = true,
                    )

                    if (state is BusState.OnlineRunning) DelayBubble(state.delayMin)
                    if (state is BusState.OnlineRunning) Vehicle(state.vehicle)

                    Spacer(Modifier.weight(1F))

                    Favouritificator(
                        onEvent = onEvent,
                        busId = state.busId,
                        favouritePartOfConn = state.favourite,
                        stops = state.stops
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    SequenceRow(onEvent, state.sequenceName, state.nextBus != null, state.previousBus != null)
                    if (state.restriction) Restriction()
                    if (state !is BusState.OnlineRunning && state.error) Error()
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Timetable(
                            navigate = navigate,
                            onlineConnStops = (state as? BusState.Online)?.onlineConnStops,
                            nextStopIndex = (state as? BusState.OnlineRunning)?.nextStopIndex,
                            stops = state.stops,
                            traveledSegments = state.traveledSegments,
                            height = state.lineHeight,
                            isOnline = state is BusState.OnlineRunning
                        )
                    }

                    CodesAndShare(state)
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DoesNotExist(
    busId: String,
) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    Text("Tento kurz ($busId) bohužel neexistuje :(\nAsi jste zadali špatně ID.")
}

context(ColumnScope)
@Composable
private fun Errors(
    runsNextTimeAfterToday: LocalDate?,
    onEvent: (BusEvent) -> Unit,
    runsNextTimeAfterDate: LocalDate?,
    busId: String,
    date: LocalDate,
) {
    runsNextTimeAfterDate.work(runsNextTimeAfterToday)
    ErrorMessage(
        when {
            runsNextTimeAfterDate == null && runsNextTimeAfterToday == null ->
                "Tento spoj (ID $busId) bohužel ${date.toCzechLocative()} nejede :(\nZkontrolujte, zda jste zadali správné datum."

            runsNextTimeAfterDate != null && runsNextTimeAfterToday != null && runsNextTimeAfterDate != runsNextTimeAfterToday ->
                "Tento spoj (ID $busId) ${date.toCzechLocative()} nejede. Jede mimo jiné ${runsNextTimeAfterDate.toCzechLocative()} nebo ${runsNextTimeAfterToday.toCzechLocative()}"

            runsNextTimeAfterDate == null && runsNextTimeAfterToday != null ->
                "Tento spoj (ID $busId) ${date.toCzechLocative()} nejede, ale pojede ${runsNextTimeAfterToday.toCzechLocative()}."

            runsNextTimeAfterDate != null ->
                "Tento spoj (ID $busId) ${date.toCzechLocative()} nejede, ale pojede ${runsNextTimeAfterDate.toCzechLocative()}."

            else -> throw IllegalArgumentException()
        }
    )

    if (runsNextTimeAfterToday != null) {
        ChangeDate(onEvent, runsNextTimeAfterToday)
    }
    if (runsNextTimeAfterDate != null && runsNextTimeAfterToday != runsNextTimeAfterDate) {
        ChangeDate(onEvent, runsNextTimeAfterDate)
    }
}

@Composable
private fun ChangeDate(
    onEvent: (BusEvent) -> Unit,
    date: LocalDate,
) {
    Button(
        onClick = {
            onEvent(BusEvent.ChangeDate(date))
        },
        Modifier.padding(top = 16.dp)
    ) {
        Text("Změnit datum na ${date.toCzechAccusative()}")
    }
}

@Composable
private fun ErrorMessage(
    text: String,
) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    Text(text)
}

@Composable
fun Timetable(
    stops: List<LineTimeNameConnIdNextStop>,
    navigate: NavigateFunction,
    onlineConnStops: List<OnlineConnStop>?,
    nextStopIndex: Int?,
    showLine: Boolean = true,
    traveledSegments: Int = 0,
    height: Float = 0F,
    isOnline: Boolean = false,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Max)
        .padding(12.dp)
) {
    Column(
        Modifier.weight(1F)
    ) {
        stops.forEachIndexed { index, stop ->
            val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
            MyText(
                text = stop.name,
                navigate = navigate,
                time = stop.time,
                stop = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop?.platform ?: "",
                Modifier.fillMaxWidth(1F),
                color = if (nextStopIndex != null && index == nextStopIndex)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        stops.forEachIndexed { index, stop ->
            val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
            MyText(
                text = stop.time.toString(),
                navigate = navigate,
                time = stop.time,
                stop = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop?.platform ?: "",
                color = if (nextStopIndex != null && index == nextStopIndex)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (onlineConnStops != null) Column(Modifier.padding(start = 8.dp)) {
        stops.forEach { stop ->
            val onlineStop = onlineConnStops.find { it.scheduledTime == stop.time }
            if (onlineStop != null) MyText(
                text = stop.time.plusMinutes(onlineStop.delay.toLong()).toString(),
                navigate = navigate,
                time = stop.time.plusMinutes(onlineStop.delay.toLong()),
                stop = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop.platform,
                color = colorOfDelayText(onlineStop.delay.toFloat()),
            )
            else Text("", Modifier.defaultMinSize(24.dp, 24.dp),)
        }
    }

    if (showLine) Line(
        stops = stops,
        traveledSegments = traveledSegments,
        height = height,
        isOnline = isOnline
    )
}

@Composable
private fun Line(
    stops: List<LineTimeNameConnIdNextStop>,
    traveledSegments: Int,
    height: Float,
    isOnline: Boolean,
) {
    val passedColor = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val busColor = if (isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface
    val lineColor = MaterialTheme.colorScheme.surfaceVariant
    val stopCount = stops.count()

    val animatedHeight by animateFloatAsState(height, label = "HeightAnimation")

    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .width(20.dp)
            .padding(horizontal = 8.dp),
        contentDescription = "Poloha spoje"
    ) {
        val canvasHeight = size.height
        val lineWidth = 3.dp.toPx()
        val lineXOffset = 7.dp.toPx()
        val rowHeight = canvasHeight / stopCount
        val circleRadius = 5.5.dp.toPx()
        val circleStrokeWidth = 3.dp.toPx()

        translate(left = lineXOffset, top = rowHeight * .5F) {
            drawLine(
                color = lineColor,
                start = Offset(),
                end = Offset(y = canvasHeight - rowHeight),
                strokeWidth = lineWidth,
            )

            repeat(stopCount) { i ->
                translate(top = i * rowHeight) {
                    val passed = traveledSegments >= i

                    drawCircle(
                        color = if (passed) passedColor else bgColor,
                        radius = circleRadius,
                        center = Offset(),
                        style = Fill
                    )
                    drawCircle(
                        color = if (passed) passedColor else lineColor,
                        radius = circleRadius,
                        center = Offset(),
                        style = Stroke(
                            width = circleStrokeWidth
                        )
                    )
                }
            }

            drawLine(
                color = passedColor,
                start = Offset(),
                end = Offset(y = rowHeight * animatedHeight),
                strokeWidth = lineWidth,
            )

            if (height > 0F) drawCircle(
                color = busColor,
                radius = circleRadius - circleStrokeWidth * .5F,
                center = Offset(y = rowHeight * animatedHeight)
            )
        }
    }
}

@Composable
private fun Error(
) = Card(
    Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    )
) {
    Row(
        Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.GpsOff, null, Modifier.padding(horizontal = 8.dp))
        Text(text = "Offline", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall)
    }
    Text(
        text = "Pravděpodobně spoj neodesílá data o své poloze, nebo má zpoždění a ještě nevyjel z výchozí zastávky. Často se také stává, že spoj je přibližně první tři minuty své jízdy offline a až poté začne odesílat aktuální data",
        Modifier.padding(all = 8.dp)
    )
}

@Composable
private fun Restriction(
) = Card(
    Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
    )
) {
    Row(
        Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WarningAmber, null, Modifier.padding(horizontal = 8.dp))
        Text(text = "Výluka", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall)
    }
    Text(text = "Tento spoj jede podle výlukového jízdního řádu!", Modifier.padding(all = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SequenceRow(
    onEvent: (BusEvent) -> Unit,
    sequenceName: String?,
    hasNextBus: Boolean,
    hasPreviousBus: Boolean,
) {
    if (sequenceName != null) Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(
            onClick = {
                if (hasPreviousBus) onEvent(BusEvent.PreviousBus)
            },
            enabled = hasPreviousBus,
        ) {
            IconWithTooltip(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Předchozí spoj kurzu"
            )
        }
        Box(
            Modifier.weight(1F),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                onClick = {
                    onEvent(BusEvent.ShowSequence)
                }
            ) {
                Text("Kurz: $sequenceName")
            }
        }
        IconButton(
            onClick = {
                if (hasNextBus) onEvent(BusEvent.NextBus)
            },
            enabled = hasNextBus,
        ) {
            IconWithTooltip(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Následující spoj kurzu"
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Favouritificator(
    onEvent: (BusEvent) -> Unit,
    busId: String,
    favouritePartOfConn: PartOfConn?,
    stops: List<LineTimeNameConnIdNextStop>,
) {
    var show by remember { mutableStateOf(false) }
    var part by remember { mutableStateOf(PartOfConn(busId, -1, -1)) }

    FilledIconToggleButton(checked = favouritePartOfConn != null, onCheckedChange = {
        part = favouritePartOfConn ?: PartOfConn(busId, -1, -1)
        show = true
    }) {
        IconWithTooltip(Icons.Default.Star, "Oblíbené")
    }

    if (show) AlertDialog(
        onDismissRequest = {
            show = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onEvent(BusEvent.ChangeFavourite(part))
                    show = false
                },
                enabled = part.start != -1 && part.end != -1
            ) {
                Text("Potvrdit")
            }
        },
        dismissButton = {
            if (favouritePartOfConn == null) TextButton(
                onClick = {
                    show = false
                }
            ) {
                Text("Zrušit")
            }
            else TextButton(
                onClick = {
                    onEvent(BusEvent.RemoveFavourite)
                    show = false
                }
            ) {
                Text("Odstranit")
            }
        },
        title = {
            Text("Upravit oblíbený spoj")
        },
        icon = {
            Icon(Icons.Default.Star, null)
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
//                                    .verticalScroll(rememberScrollState())
            ) {
                Text("Vyberte Váš oblíbený úsek tohoto spoje:")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .height(IntrinsicSize.Max)
                        .padding(8.dp)
                ) {
                    val start = remember { Animatable(part.start.toFloat()) }
                    val end = remember { Animatable(part.end.toFloat()) }
                    val alpha by animateFloatAsState(if (part.start == -1) 0F else 1F, label = "AlphaAnimation")

                    val scope = rememberCoroutineScope()
                    fun click(i: Int) = scope.launch {
                        when {
                            part.start == -1 && i == stops.lastIndex -> {}
                            part.start == -1 -> {
                                start.snapTo(i.toFloat())
                                end.snapTo(i.toFloat())
                                part = part.copy(start = i)
                            }

                            part.start == i -> {
                                part = part.copy(start = -1, end = -1)
                                end.snapTo(i.toFloat())
                            }

                            part.end == i -> {
                                part = part.copy(end = -1)
                                end.animateTo(part.start.toFloat())
                            }

                            i < part.start -> {
                                part = part.copy(start = i)
                                if (part.end == -1) launch { end.animateTo(part.start.toFloat()) }
                                start.animateTo(part.start.toFloat())
                            }

                            else /*cast.start < i*/ -> {
                                part = part.copy(end = i)
                                end.animateTo(part.end.toFloat())
                            }
                        }
                    }

                    val selectedColor = MaterialTheme.colorScheme.secondary
                    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val stopCount = stops.count()

                    var canvasHeight by remember { mutableFloatStateOf(0F) }
                    Canvas(
                        Modifier
                            .fillMaxHeight()
                            .width(24.dp)
                            .padding(horizontal = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { (_, y) ->
                                    val rowHeight = canvasHeight / stopCount
                                    val i = (y / rowHeight).toInt()
                                    click(i)
                                }
                            }
                    ) {
                        canvasHeight = size.height
                        val rowHeight = canvasHeight / stopCount
                        val lineWidth = 4.5.dp.toPx()
                        val lineXOffset = 0F
                        val smallCircleRadius = 6.5.dp.toPx()
                        val bigCircleRadius = 9.5.dp.toPx()

                        translate(left = lineXOffset, top = rowHeight * .5F) {
                            drawLine(
                                color = lineColor,
                                start = Offset(),
                                end = Offset(y = canvasHeight - rowHeight),
                                strokeWidth = lineWidth,
                            )

                            repeat(stopCount) { i ->
                                translate(top = i * rowHeight) {
                                    if (i.toFloat() <= start.value || end.value <= i.toFloat()) drawCircle(
                                        color = lineColor,
                                        radius = smallCircleRadius,
                                        center = Offset(),
                                        style = Fill,
                                    )
                                }
                            }

                            drawCircle(
                                color = selectedColor,
                                radius = bigCircleRadius,
                                center = Offset(y = rowHeight * end.value),
                                style = Fill,
                                alpha = alpha,
                            )
                            drawCircle(
                                color = selectedColor,
                                radius = bigCircleRadius,
                                center = Offset(y = rowHeight * start.value),
                                style = Fill,
                                alpha = alpha,
                            )

                            if (part.start != -1) drawLine(
                                color = selectedColor,
                                start = Offset(y = start.value * rowHeight),
                                end = Offset(y = end.value * rowHeight),
                                strokeWidth = lineWidth,
                            )
                        }
                    }
                    Column(
                        Modifier
                            .weight(1F)
                    ) {
                        stops.forEachIndexed { i, stop ->
                            Box(
                                Modifier
                                    .weight(1F)
                                    .defaultMinSize(32.dp, 32.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = stop.name,
                                    Modifier
                                        .clickable {
                                            click(i)
                                        },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (i == part.start || i == part.end) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

context(ColumnScope)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CodesAndShare(
    state: BusState.Exists,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        var showMenu by remember { mutableStateOf(false) }
        val context = LocalContext.current

        TextButton(onClick = {
            showMenu = true
        }) {
            Text("ID: ${state.busId}")
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = {
                    showMenu = false
                }
            ) {
                val clipboardManager = LocalClipboardManager.current
                DropdownMenuItem(
                    text = {
                        Text("Zobrazit v mapě")
                    },
                    onClick = {},
                    enabled = false
                )
                DropdownMenuItem(
                    text = {
                        Text("ID: ${state.busId}")
                    },
                    onClick = {},
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.busId)
                                        type = "text/plain"
                                    }, "Sdílet ID spoje"))
                                    showMenu = false
                                }
                            ) {
                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.busId))
                                    showMenu = false
                                }
                            ) {
                                IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
                            }
                        }
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("Název: ${state.busName}")
                    },
                    onClick = {},
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.busName)
                                        type = "text/plain"
                                    }, "Sdílet název spoje"))
                                    showMenu = false
                                }
                            ) {
                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.busName))
                                    showMenu = false
                                }
                            ) {
                                IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
                            }
                        }
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("Link: ${state.deeplink.removePrefix("https://jaro-jaro.github.io/DPMCB")}")
                    },
                    onClick = {},
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.deeplink)
                                        type = "text/uri-list"
                                    }, "Sdílet deeplink"))
                                    showMenu = false
                                }
                            ) {
                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.deeplink))
                                    showMenu = false
                                }
                            ) {
                                IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
                            }
                        }
                    }
                )
            }
        }

    }

    Column {
        state.fixedCodes.forEach {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.timeCodes.forEach {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(state.lineCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyText(
    text: String,
    navigate: NavigateFunction,
    time: LocalTime,
    stop: String,
    nextStop: String?,
    line: Int,
    platform: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) = Box {
    var showDropDown by rememberSaveable { mutableStateOf(false) }

    DropdownMenu(
        expanded = showDropDown,
        onDismissRequest = {
            showDropDown = false
        }
    ) {
        DropdownMenuItem(
            text = {
                Text("$stop $platform")
            },
            onClick = {},
            enabled = false
        )
        DropdownMenuItem(
            text = {
                Text("Zobrazit odjezdy")
            },
            onClick = {
                navigate(
                    DeparturesDestination(
                        time = time,
                        stop = stop,
                    )
                )
                showDropDown = false
            },
        )
        nextStop?.let {
            DropdownMenuItem(
                text = {
                    Text("Zobrazit zastávkové JŘ")
                },
                onClick = {
                    navigate(
                        TimetableDestination(
                            lineNumber = line,
                            stop = stop,
                            nextStop = nextStop,
                        )
                    )
                    showDropDown = false
                },
            )
        }
    }
    Text(
        text = text,
        color = color,
        modifier = modifier
            .combinedClickable(
                onClick = {
                    navigate(
                        DeparturesDestination(
                            time = time,
                            stop = stop,
                        )
                    )
                },
                onLongClick = {
                    showDropDown = true
                },
            )
            .defaultMinSize(24.dp, 24.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}