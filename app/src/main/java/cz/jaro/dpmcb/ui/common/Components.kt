package cz.jaro.dpmcb.ui.common

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
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
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.regN
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechAccusative
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDelay
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.work
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.ui.bus.BusEvent
import cz.jaro.dpmcb.ui.bus.BusState
import cz.jaro.dpmcb.ui.common.icons.Empty
import cz.jaro.dpmcb.ui.common.icons.LeftHalfDisk
import cz.jaro.dpmcb.ui.common.icons.RightHalfDisk
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.sequence.BusInSequence
import cz.jaro.dpmcb.ui.sequence.SequenceState
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

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
    Column(Modifier.weight(1F)) {
        stops.forEachIndexed { index, stop ->
            val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
            TimetableText(
                text = stop.name,
                navigate = navigate,
                time = stop.time,
                stopName = stop.name,
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
            val color = if (nextStopIndex != null && index == nextStopIndex)
                MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            StopTypeIcon(stop.type, color = color)
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        stops.forEachIndexed { index, stop ->
            val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
            TimetableText(
                text = stop.time.toString(),
                navigate = navigate,
                time = stop.time,
                stopName = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop?.platform ?: "",
                Modifier,
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
                stopName = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop.platform,
                Modifier,
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
        Modifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StopTypeIcon(stopType: StopType, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    when (stopType) {
        StopType.Normal -> IconWithTooltip(
            imageVector = Icons.Default.Empty,
            contentDescription = null,
            modifier,
            tint = color,
        )

        StopType.GetOnOnly -> IconWithTooltip(
            imageVector = Icons.Default.RightHalfDisk,
            contentDescription = "Zastávka pouze pro nástup",
            modifier,
            tint = color,
        )

        StopType.GetOffOnly -> IconWithTooltip(
            imageVector = Icons.Default.LeftHalfDisk,
            contentDescription = "Zastávka pouze pro výstup",
            modifier,
            tint = color,
        )
    }
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
fun Favouritificator(
    onEvent: (BusEvent) -> Unit,
    busName: String,
    favouritePartOfConn: PartOfConn?,
    stops: List<BusStop>,
) {
    var show by remember { mutableStateOf(false) }
    var part by remember { mutableStateOf(PartOfConn(busName, -1, -1)) }

    fun isDisabled(i: Int) = when {
        part.start == -1 && i == stops.lastIndex -> true
        part.start == -1 && stops[i].type == StopType.GetOffOnly -> true

        part.start == -1 -> false
        part.start == i -> false
        part.end == i -> false

        i < part.start -> true
        stops[i].type == StopType.GetOnOnly -> true
        else -> false
    }

    FilledIconToggleButton(checked = favouritePartOfConn != null, onCheckedChange = {
        part = favouritePartOfConn ?: PartOfConn(busName, -1, -1)
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
                            part.start == -1 && stops[i].type == StopType.GetOffOnly -> {}
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

                            i < part.start -> {}
                            stops[i].type == StopType.GetOnOnly -> {}

                            else -> {
                                part = part.copy(end = i)
                                end.animateTo(part.end.toFloat())
                            }
                        }
                    }

                    val selectedColor = MaterialTheme.colorScheme.tertiary
                    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val disabledColor1 = MaterialTheme.colorScheme.onSurface
                    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38F)
                    val stopCount = stops.count()

                    var canvasHeight by remember { mutableFloatStateOf(0F) }
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(32.dp)
                            .padding(end = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { (_, y) ->
                                    val rowHeight = canvasHeight / stopCount
                                    val i = (y / rowHeight).toInt()
                                    click(i)
                                }
                            }
                    ) {
                        Canvas(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    this.alpha = .38F
                                }
                        ) {
                            canvasHeight = size.height
                            val rowHeight = canvasHeight / stopCount
                            val lineWidth = 4.5.dp.toPx()
                            val smallCircleRadius = 6.5.dp.toPx()

                            translate(left = 8.dp.toPx(), top = rowHeight * .5F) {

                                drawLine(
                                    color = disabledColor1,
                                    start = UtilFunctions.Offset(),
                                    end = UtilFunctions.Offset(y = canvasHeight - rowHeight),
                                    strokeWidth = lineWidth,
                                )

                                repeat(stopCount) { i ->
                                    val isInFavouritePart = end.value <= i.toFloat() || i.toFloat() <= start.value
                                    translate(top = i * rowHeight) {
                                        if (isInFavouritePart && isDisabled(i)) drawCircle(
                                            color = disabledColor1,
                                            radius = smallCircleRadius,
                                            center = UtilFunctions.Offset(),
                                            style = Fill,
                                        )
                                    }
                                }
                            }
                        }
                        Canvas(
                            Modifier
                                .fillMaxSize()
                        ) {
                            canvasHeight = size.height
                            val rowHeight = canvasHeight / stopCount
                            val lineWidth = 4.5.dp.toPx()
                            val smallCircleRadius = 6.5.dp.toPx()
                            val bigCircleRadius = 9.5.dp.toPx()

                            translate(left = 8.dp.toPx(), top = rowHeight * .5F) {

                                val iMin = stops.indices.first { !isDisabled(it) }
                                val iMax = stops.indices.last { !isDisabled(it) }
                                drawLine(
                                    color = lineColor,
                                    start = UtilFunctions.Offset(y = iMin * rowHeight),
                                    end = UtilFunctions.Offset(y = iMax * rowHeight),
                                    strokeWidth = lineWidth,
                                )

                                repeat(stopCount) { i ->
                                    val isInFavouritePart = end.value <= i.toFloat() || i.toFloat() <= start.value
                                    translate(top = i * rowHeight) {
                                        if (isInFavouritePart && !isDisabled(i)) drawCircle(
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
                    }
                    Column(
                        Modifier
                            .weight(1F)
                    ) {
                        stops.forEachIndexed { i, stop ->
                            Row(
                                Modifier
                                    .weight(1F)
                                    .defaultMinSize(32.dp, 32.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                Text(
                                    text = stop.name,
                                    Modifier
                                        .clickable(enabled = !isDisabled(i)) {
                                            click(i)
                                        }
                                        .weight(1F)
                                        .defaultMinSize(24.dp, 24.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = when {
                                        isDisabled(i) -> disabledColor
                                        i == part.start || i == part.end -> selectedColor
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (stop.type != StopType.Normal) StopTypeIcon(stop.type, color = when {
                                    isDisabled(i) -> disabledColor
                                    i == part.start || i == part.end -> selectedColor
                                    else -> MaterialTheme.colorScheme.onSurface
                                })
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
    graphicsLayer: GraphicsLayer?,
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
                            IconWithTooltip(Icons.AutoMirrored.Filled.OpenInNew, "Otevřít")
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
                            val scope = rememberCoroutineScope()
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        // todo sdílet (-> sdílet část spoje) -> _/sdílet s obrázkem
                                        context.startActivity(Intent.createChooser(Intent().apply {
                                            action = Intent.ACTION_SEND

                                            graphicsLayer?.toImageBitmap()?.let { bitmap ->
                                                val d = context.filesDir
                                                val f = File(d, "spoj_${state.busName.replace('/', '_')}.png")
                                                f.createNewFile()

                                                f.outputStream().use { os ->
                                                    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, os)
                                                    os.flush()
                                                }

                                                val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", f)

                                                putExtra(Intent.EXTRA_STREAM, uri)
                                            }

                                            putExtra(Intent.EXTRA_TEXT, state.deeplink)
                                            type = "image/png"
                                        }, "Sdílet spoj"))
                                        showMenu = false
                                    }

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
fun TimetableText(
    text: String,
    navigate: NavigateFunction,
    time: LocalTime,
    stopName: String,
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
                Text("$stopName $platform")
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
                        stop = stopName,
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
                            stop = stopName,
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
                            stop = stopName,
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

@Composable
fun FABs(state: SequenceState.OK, lazyListState: LazyListState, date: LocalDate) {
    fun Int.busIndexToListIndex() = 3 + state.before.count() * 2 + this * 4

    val now = remember(state.buses) {
        if (date != LocalDate.now()) null
        else state.buses.indexOfFirst {
            it.isRunning
        }.takeUnless {
            it == -1
        } ?: state.buses.indexOfFirst {
            LocalTime.now() < it.stops.last().time
        }.takeIf {
            state.runsToday && state.buses.first().stops.first().time < LocalTime.now() && LocalTime.now() < state.buses.last().stops.last().time
        }
    }

    val scope = rememberCoroutineScope()
    Column {
        SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null
            )
        }
        if (now != null) SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    lazyListState.animateScrollToItem(now.busIndexToListIndex())
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null
            )
        }
        SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    lazyListState.animateScrollToItem(Int.MAX_VALUE)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null
            )
        }
    }
}

@Composable
fun BusButton(
    navigate: NavigateFunction,
    bus: BusInSequence,
) = TextButton(
    onClick = {
        navigate(Route.Bus(busName = bus.busName))
    }
) {
    Text("Detail spoje")
}

@Composable
fun Connection(
    navigate: NavigateFunction,
    sequence: Pair<String, String>,
) = TextButton(
    onClick = {
        navigate(Route.Sequence(sequence.first))
    }
) {
    Text(sequence.second)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Vehicle(vehicle: Int?) {
    if (vehicle != null) {
        Text(
            text = "ev. č. ${vehicle.regN()}",
            Modifier.padding(horizontal = 8.dp),
        )
        val context = LocalContext.current
        IconWithTooltip(
            Icons.Default.Info,
            "Zobrazit informace o voze",
            Modifier.clickable {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(context, Uri.parse("https://seznam-autobusu.cz/seznam?operatorName=DP+města+České+Budějovice&prov=1&evc=${vehicle.regN()}"))
            },
        )
    }
}

@Composable
fun DelayBubble(delayMin: Float) {
    Badge(
        Modifier,
        containerColor = UtilFunctions.colorOfDelayBubbleContainer(delayMin),
        contentColor = UtilFunctions.colorOfDelayBubbleText(delayMin),
    ) {
        Text(
            text = delayMin.toDouble().minutes.toDelay(),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Wheelchair(
    lowFloor: Boolean,
    confirmedLowFloor: Boolean?,
    modifier: Modifier = Modifier,
    enableCart: Boolean = false,
) {
    IconWithTooltip(
        imageVector = remember(lowFloor, confirmedLowFloor) {
            when {
                enableCart && Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                confirmedLowFloor == true -> Icons.AutoMirrored.Filled.Accessible
                confirmedLowFloor == false -> Icons.Default.NotAccessible
                lowFloor -> Icons.AutoMirrored.Filled.Accessible
                else -> Icons.Default.NotAccessible
            }
        },
        contentDescription = when {
            confirmedLowFloor == true -> "Potvrzený nízkopodlažní vůz"
            confirmedLowFloor == false -> "Potvrzený vysokopodlažní vůz"
            lowFloor -> "Plánovaný nízkopodlažní vůz"
            else -> "Nezaručený nízkopodlažní vůz"
        },
        modifier,
        tint = when {
            confirmedLowFloor == false && lowFloor -> MaterialTheme.colorScheme.error
            confirmedLowFloor != null -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
}

@Composable
fun Name(name: String, modifier: Modifier = Modifier, subName: String? = null) {
    Text(buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 24.sp)) {
            append(name)
        }
        if (subName != null) withStyle(style = SpanStyle(fontSize = 14.sp)) {
            append(subName)
        }
    }, modifier, color = MaterialTheme.colorScheme.primary)
}