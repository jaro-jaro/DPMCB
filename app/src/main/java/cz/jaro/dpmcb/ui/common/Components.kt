package cz.jaro.dpmcb.ui.common

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechAccusative
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.work
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.ui.bus.BusEvent
import cz.jaro.dpmcb.ui.bus.BusState
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun BusDoesNotExist(
    busName: String,
) = ErrorMessage("Tento spoj ($busName) bohužel neexistuje :(\n Asi jste zadali špatně název.")

context(ColumnScope)
@Composable
fun BusDoesNotRun(
    runsNextTimeAfterToday: LocalDate?,
    onEvent: (BusEvent) -> Unit,
    runsNextTimeAfterDate: LocalDate?,
    busName: String,
    date: LocalDate,
) {
    ErrorMessage(
        when {
            runsNextTimeAfterDate == null && runsNextTimeAfterToday == null ->
                "Tento spoj ($busName) bohužel ${date.toCzechLocative()} nejede :(\nZkontrolujte, zda jste zadali správné datum."

            runsNextTimeAfterDate != null && runsNextTimeAfterToday != null && runsNextTimeAfterDate != runsNextTimeAfterToday ->
                "Tento spoj ($busName) ${date.toCzechLocative()} nejede. Jede mimo jiné ${runsNextTimeAfterDate.toCzechLocative()} nebo ${runsNextTimeAfterToday.toCzechLocative()}"

            runsNextTimeAfterDate == null && runsNextTimeAfterToday != null ->
                "Tento spoj ($busName) ${date.toCzechLocative()} nejede, ale pojede ${runsNextTimeAfterToday.toCzechLocative()}."

            runsNextTimeAfterDate != null ->
                "Tento spoj ($busName) ${date.toCzechLocative()} nejede, ale pojede ${runsNextTimeAfterDate.toCzechLocative()}."

            else -> throw IllegalArgumentException()
        }
    )

    if (runsNextTimeAfterToday != null) {
        ChangeDateButton(onEvent, runsNextTimeAfterToday)
    }
    if (runsNextTimeAfterDate != null && runsNextTimeAfterToday != runsNextTimeAfterDate) {
        ChangeDateButton(onEvent, runsNextTimeAfterDate)
    }
}

@Composable
fun ChangeDateButton(
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
fun ErrorMessage(
    text: String,
) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    Text(text)
}

context(TransitionScope)
@Composable
fun Timetable(
    stops: List<BusStop>,
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
            TimetableText(
                text = stop.name,
                navigate = navigate,
                time = stop.time,
                stop = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop?.platform ?: "",
                Modifier.fillMaxWidth(1F).sharedElement("timetable-${stop.connName}-stopName-${stop.name}-${stop.time}"),
                color = if (nextStopIndex != null && index == nextStopIndex)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        stops.forEachIndexed { index, stop ->
            val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
            TimetableText(
                text = stop.time.toString(),
                navigate = navigate,
                time = stop.time,
                stop = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop?.platform ?: "",
                Modifier.sharedElement("timetable-${stop.connName}-time-${stop.name}-${stop.time}"),
                color = if (nextStopIndex != null && index == nextStopIndex)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (onlineConnStops != null) Column(Modifier.padding(start = 8.dp)) {
        stops.forEach { stop ->
            val onlineStop = onlineConnStops.find { it.scheduledTime == stop.time }
            if (onlineStop != null) TimetableText(
                text = stop.time.plusMinutes(onlineStop.delay.toLong()).toString(),
                navigate = navigate,
                time = stop.time.plusMinutes(onlineStop.delay.toLong()),
                stop = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop.platform,
                Modifier.sharedElement("timetable-${stop.connName}-actualTime-${stop.name}-${stop.time}"),
                color = UtilFunctions.colorOfDelayText(onlineStop.delay.toFloat()),
            )
            else Text("", Modifier.defaultMinSize(24.dp, 24.dp))
        }
    }

    if (showLine) Line(
        stops = stops,
        traveledSegments = traveledSegments,
        height = height,
        isOnline = isOnline,
        Modifier.sharedElement("timetable-${stops.first().connName}-line"),
    )
}

@Composable
fun Line(
    stops: List<BusStop?>,
    traveledSegments: Int,
    height: Float,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val passedColor = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val busColor = if (isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface
    val lineColor = MaterialTheme.colorScheme.surfaceVariant
    val stopCount = stops.count()

    val animatedHeight by animateFloatAsState(height, label = "HeightAnimation")

    Canvas(
        modifier = modifier
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
                start = UtilFunctions.Offset(),
                end = UtilFunctions.Offset(y = canvasHeight - rowHeight),
                strokeWidth = lineWidth,
            )

            repeat(stopCount) { i ->
                translate(top = i * rowHeight) {
                    val passed = traveledSegments >= i

                    drawCircle(
                        color = if (passed) passedColor else bgColor,
                        radius = circleRadius,
                        center = UtilFunctions.Offset(),
                        style = Fill
                    )
                    drawCircle(
                        color = if (passed) passedColor else lineColor,
                        radius = circleRadius,
                        center = UtilFunctions.Offset(),
                        style = Stroke(
                            width = circleStrokeWidth
                        )
                    )
                }
            }

            drawLine(
                color = passedColor,
                start = UtilFunctions.Offset(),
                end = UtilFunctions.Offset(y = rowHeight * animatedHeight),
                strokeWidth = lineWidth,
            )

            if (height > 0F) drawCircle(
                color = busColor,
                radius = circleRadius - circleStrokeWidth * .5F,
                center = UtilFunctions.Offset(y = rowHeight * animatedHeight)
            )
        }
    }
}

@Composable
fun Error(
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
fun Restriction(
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

context(TransitionScope)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceRow(
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
            UtilFunctions.IconWithTooltip(
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
            UtilFunctions.IconWithTooltip(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Následující spoj kurzu"
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Favouritificator(
    onEvent: (BusEvent) -> Unit,
    busName: String,
    favouritePartOfConn: PartOfConn?,
    stops: List<BusStop>,
) {
    var show by remember { mutableStateOf(false) }
    var part by remember { mutableStateOf(PartOfConn(busName, -1, -1)) }

    FilledIconToggleButton(checked = favouritePartOfConn != null, onCheckedChange = {
        part = favouritePartOfConn ?: PartOfConn(busName, -1, -1)
        show = true
    }) {
        UtilFunctions.IconWithTooltip(Icons.Default.Star, "Oblíbené")
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
                                start = UtilFunctions.Offset(),
                                end = UtilFunctions.Offset(y = canvasHeight - rowHeight),
                                strokeWidth = lineWidth,
                            )

                            repeat(stopCount) { i ->
                                translate(top = i * rowHeight) {
                                    if (i.toFloat() <= start.value || end.value <= i.toFloat()) drawCircle(
                                        color = lineColor,
                                        radius = smallCircleRadius,
                                        center = UtilFunctions.Offset(),
                                        style = Fill,
                                    )
                                }
                            }

                            drawCircle(
                                color = selectedColor,
                                radius = bigCircleRadius,
                                center = UtilFunctions.Offset(y = rowHeight * end.value),
                                style = Fill,
                                alpha = alpha,
                            )
                            drawCircle(
                                color = selectedColor,
                                radius = bigCircleRadius,
                                center = UtilFunctions.Offset(y = rowHeight * start.value),
                                style = Fill,
                                alpha = alpha,
                            )

                            if (part.start != -1) drawLine(
                                color = selectedColor,
                                start = UtilFunctions.Offset(y = start.value * rowHeight),
                                end = UtilFunctions.Offset(y = end.value * rowHeight),
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
fun CodesAndShare(
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
            Text("Spoj ${state.busName}")
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = {
                    showMenu = false
                }
            ) {
                val clipboardManager = LocalClipboardManager.current
                DropdownMenuItem(
                    text = {
                        Text("Zobrazit na mapě")
                    },
                    onClick = {},
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                    .launchUrl(context, Uri.parse("https://jih.mpvnet.cz/Jikord/map/Route?mode=0,0,2,0,${state.busName.replace('/', ',')},0"))

                                showMenu = false
                            }
                        ) {
                            UtilFunctions.IconWithTooltip(Icons.AutoMirrored.Filled.OpenInNew, "Otevřít")
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
                                UtilFunctions.IconWithTooltip(Icons.Default.Share, "Sdílet")
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.busName))
                                    showMenu = false
                                }
                            ) {
                                UtilFunctions.IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
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
                                UtilFunctions.IconWithTooltip(Icons.Default.Share, "Sdílet")
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.deeplink))
                                    showMenu = false
                                }
                            ) {
                                UtilFunctions.IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
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
fun TimetableText(
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
                    Route.Departures(
                        time = time.toSimpleTime().work(),
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
                        Route.Timetable(
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
                        Route.Departures(
                            time = time.toSimpleTime(),
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