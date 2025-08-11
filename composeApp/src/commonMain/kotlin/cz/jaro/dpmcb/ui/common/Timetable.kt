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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.Offset
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.inMinutes
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.onSecondaryClick
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.data.realtions.favourites.isNotEmpty
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
    val filteredStops = if (part != null && part.isNotEmpty()) stops.slice(part.iterator()) else stops
    Column(Modifier.weight(1F)) {
        filteredStops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
                val previousOnlineStop = onlineConnStops?.getOrNull(onlineConnStops.indexOf(onlineStop) - 1)
                val defaultColor = if (movedNextStopIndex != null && index == movedNextStopIndex)
                    MaterialTheme.colorScheme.secondary else LocalContentColor.current

                @Composable
                fun TT(
                    time: LocalTime,
                    modifier: Modifier = Modifier.padding(start = 8.dp),
                    boxModifier: Modifier = Modifier,
                    text: String = time.toString(),
                    color: Color = defaultColor,
                ) = TimetableText(
                    text = text,
                    onEvent = onEvent,
                    time = time,
                    stopName = stop.name,
                    nextStop = stop.nextStop,
                    line = stop.line,
                    platform = onlineStop?.platform ?: "",
                    modifier,
                    boxModifier,
                    color = color,
                )

                Row(Modifier.weight(1F), verticalAlignment = Alignment.CenterVertically) {
                    TT(time = stop.time, Modifier, Modifier.weight(1F, fill = false), text = stop.name)
                    if (stop.type != StopType.Normal) StopTypeIcon(stop.type, Modifier.padding(start = 8.dp), color = defaultColor)
                }
                if (stop.arrival != null) TT(time = stop.arrival)
                if (previousOnlineStop != null && stop.arrival != null) TT(
                    time = stop.arrival + previousOnlineStop.delay.minutes,
                    color = colorOfDelayText(previousOnlineStop.delay.toFloat()),
                )
                if (stop.arrival != null) Text(
                    text = "-",
                    Modifier.defaultMinSize(minHeight = 24.dp).padding(start = 8.dp),
                    color = defaultColor,
                )
                TT(time = stop.time)
                if (onlineStop != null && stop.arrival != null) TT(
                    time = stop.time.coerceAtLeast(stop.arrival + onlineStop.delay.minutes),
                    color = colorOfDelayText(
                        (stop.arrival + onlineStop.delay.minutes - stop.time).inMinutes.coerceAtLeast(0F)
                    ),
                ) else if (onlineStop != null) TT(
                    time = stop.time + onlineStop.delay.minutes,
                    color = colorOfDelayText(onlineStop.delay.toFloat()),
                )
            }
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
                start = Offset(y = if (drawPrevious) -rowHeight else 0F),
                end = Offset(y = if (drawNext) canvasHeight else canvasHeight - rowHeight),
                strokeWidth = lineWidth,
            )

            repeat(stopCount) { i ->
                translate(top = i * rowHeight) {
                    val passed = (traveledSegments - part.start) >= i

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
                start = Offset(y = if (drawPrevious) -rowHeight else 0F),
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
    boxModifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) = Box(boxModifier) {
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
            .onSecondaryClick(Unit) {
                showDropDown = true
            }
            .defaultMinSize(24.dp, 24.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}