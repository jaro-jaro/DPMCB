package cz.jaro.dpmcb.ui.common

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDelay
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.ui.common.icons.Empty
import cz.jaro.dpmcb.ui.common.icons.LeftHalfDisk
import cz.jaro.dpmcb.ui.common.icons.RightHalfDisk
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import cz.jaro.dpmcb.ui.theme.Theme
import kotlinx.datetime.LocalTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

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
    part: PartOfConn? = null,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Max)
        .padding(12.dp)
) {
    val movedNextStopIndex = if (part != null) nextStopIndex?.minus(part.start) else nextStopIndex
    val filteredStops = if (part != null) stops.filterIndexed { i, _ -> i in part } else stops
    Column(Modifier.weight(1F)) {
        filteredStops.forEachIndexed { index, stop ->
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
                color = if (movedNextStopIndex != null && index == movedNextStopIndex)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        filteredStops.forEachIndexed { index, stop ->
            val color = if (movedNextStopIndex != null && index == movedNextStopIndex)
                MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            StopTypeIcon(stop.type, color = color)
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        filteredStops.forEachIndexed { index, stop ->
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
                color = if (movedNextStopIndex != null && index == movedNextStopIndex)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (onlineConnStops != null) Column(Modifier.padding(start = 8.dp)) {
        filteredStops.forEach { stop ->
            val onlineStop = onlineConnStops.find { it.scheduledTime == stop.time }
            if (onlineStop != null) TimetableText(
                text = (stop.time + onlineStop.delay.minutes).toString(),
                navigate = navigate,
                time = stop.time + onlineStop.delay.minutes,
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

    if (showLine) if (part != null) PartialLine(
        stops = stops,
        traveledSegments = traveledSegments,
        height = height,
        isOnline = isOnline,
        part = part,
        Modifier
    ) else Line(
        stops = stops,
        traveledSegments = traveledSegments,
        height = height,
        isOnline = isOnline,
        Modifier
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
fun PartialLine(
    stops: List<BusStop?>,
    traveledSegments: Int,
    height: Float,
    isOnline: Boolean,
    part: PartOfConn,
    modifier: Modifier = Modifier,
) {
    val filteredStops = stops.filterIndexed { i, _ -> i in part }
    val passedColor = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val busColor = if (isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface
    val lineColor = MaterialTheme.colorScheme.surfaceVariant
    val stopCount = filteredStops.count()

    val animatedHeight by animateFloatAsState(height - part.start, label = "HeightAnimation")

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
            val drawPrevious = part.start != 0
            val drawNext = part.end != stops.lastIndex
            drawLine(
                color = lineColor,
                start = UtilFunctions.Offset(y = if (drawPrevious) -rowHeight else 0F),
                end = UtilFunctions.Offset(y = if (drawNext) canvasHeight else canvasHeight - rowHeight),
                strokeWidth = lineWidth,
            )

            repeat(stopCount) { i ->
                translate(top = i * rowHeight) {
                    val passed = (traveledSegments - part.start) >= i

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
                start = UtilFunctions.Offset(y = if (drawPrevious) -rowHeight else 0F),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimetableText(
    text: String,
    navigate: NavigateFunction,
    time: LocalTime,
    stopName: String,
    nextStop: String?,
    line: ShortLine,
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
                        time = time.toSimpleTime(),
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
@OptIn(ExperimentalMaterial3Api::class)
fun Vehicle(vehicle: RegistrationNumber?) {
    if (vehicle != null) {
        Text(
            text = "ev. č. ${vehicle.asString()}",
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
                    .launchUrl(context, Uri.parse("https://seznam-autobusu.cz/seznam?operatorName=DP+města+České+Budějovice&prov=1&evc=${vehicle.asString()}"))
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

@ExperimentalMaterial3Api
@Composable
fun IconWithTooltip(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tooltipText: String? = contentDescription,
    tint: Color = LocalContentColor.current,
) = if (tooltipText != null) TooltipBox(
    tooltip = {
        DPMCBTheme(
            useDarkTheme = isSystemInDarkTheme(),
            useDynamicColor = true,
            theme = Theme.Yellow,
            doTheThing = false,
        ) {
            PlainTooltip {
                Text(text = tooltipText)
            }
        }
    },
    state = rememberTooltipState(),
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
else
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )