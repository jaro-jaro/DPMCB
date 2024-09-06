package cz.jaro.dpmcb.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.minutes

sealed interface TimetableEvent {
    data class StopClick(val stopName: String, val time: LocalTime) : TimetableEvent
    data class TimetableClick(val line: ShortLine, val stop: String, val nextStop: String) : TimetableEvent
}

@Composable
fun Timetable(
    stops: List<BusStop>,
    onEvent: (TimetableEvent) -> Unit,
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
                onEvent = onEvent,
                time = stop.time,
                stopName = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop?.platform ?: "",
                Modifier.fillMaxWidth(1F),
                color = if (movedNextStopIndex != null && index == movedNextStopIndex)
                    MaterialTheme.colorScheme.secondary else LocalContentColor.current
            )
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        filteredStops.forEachIndexed { index, stop ->
            val color = if (movedNextStopIndex != null && index == movedNextStopIndex)
                MaterialTheme.colorScheme.secondary else LocalContentColor.current
            StopTypeIcon(stop.type, color = color)
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        filteredStops.forEachIndexed { index, stop ->
            val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
            TimetableText(
                text = stop.time.toString(),
                onEvent = onEvent,
                time = stop.time,
                stopName = stop.name,
                nextStop = stop.nextStop,
                line = stop.line,
                platform = onlineStop?.platform ?: "",
                Modifier,
                color = if (movedNextStopIndex != null && index == movedNextStopIndex)
                    MaterialTheme.colorScheme.secondary else LocalContentColor.current,
            )
        }
    }
    if (onlineConnStops != null) Column(Modifier.padding(start = 8.dp)) {
        filteredStops.forEach { stop ->
            val onlineStop = onlineConnStops.find { it.scheduledTime == stop.time }
            if (onlineStop != null) TimetableText(
                text = (stop.time + onlineStop.delay.minutes).toString(),
                onEvent = onEvent,
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
fun PartialLine(
    stops: List<BusStop?>,
    traveledSegments: Int,
    height: Float,
    isOnline: Boolean,
    part: PartOfConn,
    modifier: Modifier = Modifier,
) {
    val filteredStops = stops.filterIndexed { i, _ -> i in part }
    val passedColor = if (isOnline) MaterialTheme.colorScheme.primary else LocalContentColor.current
    val busColor = if (isOnline) MaterialTheme.colorScheme.secondary else LocalContentColor.current
    val bgColor = backgroundColorFor(LocalContentColor.current)
    val lineColor = variantColorFor(LocalContentColor.current)
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
    onEvent: (TimetableEvent) -> Unit,
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
                onEvent(
                    TimetableEvent.StopClick(
                        stopName = stopName,
                        time = time,
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
                    onEvent(
                        TimetableEvent.TimetableClick(
                            line = line,
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
                    onEvent(
                        TimetableEvent.StopClick(
                            stopName = stopName,
                            time = time,
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