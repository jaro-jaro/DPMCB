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
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.Offset
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.middleDestination
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.onSecondaryClick
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.data.realtions.favourites.isNotEmpty
import cz.jaro.dpmcb.ui.theme.Colors
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.minutes

sealed interface TimetableEvent {
    data class StopClick(val stopName: String, val time: LocalTime) : TimetableEvent
    data class TimetableClick(val line: ShortLine, val stop: String, val direction: Direction) : TimetableEvent
}

@Composable
fun Timetable(
    stops: List<BusStop>,
    onEvent: (TimetableEvent) -> Unit,
    onlineConnStops: List<OnlineConnStop>?,
    nextStopIndex: Int?,
    direction: Direction,
    isOneWay: Boolean,
    showLine: Boolean = true,
    traveledSegments: Int = 0,
    height: Float = 0F,
    isOnline: Boolean = false,
    part: PartOfConn? = null,
    highlight: IntRange? = null,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Max)
        .padding(12.dp)
) {
    val movedNextStopIndex = if (part != null) nextStopIndex?.minus(part.start) else nextStopIndex
    val filteredStops = if (part != null && part.isNotEmpty()) stops.slice(part.iterator()) else stops

    Column(Modifier.width(IntrinsicSize.Max)) {
        filteredStops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                val middleDest = middleDestination(isOneWay, stops.map { it.name }, index)
                val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
                val previousOnlineStop = onlineConnStops?.getOrNull(onlineConnStops.indexOf(onlineStop) - 1)
                val highlighted = highlight == null || index in highlight
                val defaultColor =
                    if (movedNextStopIndex != null && index == movedNextStopIndex) MaterialTheme.colorScheme.secondary
                    else if (highlighted) MaterialTheme.colorScheme.onSurface else Colors.dimmedContent
                val a = if (movedNextStopIndex != null && index == movedNextStopIndex || highlighted) 1F else .5F

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
                    departs = stop.type != StopType.GetOffOnly && index < filteredStops.lastIndex,
                    direction = if (middleDest != null) Direction.NEGATIVE else direction,
                    line = stop.line,
                    platform = onlineStop?.platform ?: "",
                    modifier,
                    boxModifier,
                    color = color,
                )

                TT(time = stop.arrival ?: stop.time, Modifier)
                if (stop.arrival != null) {
                    if (previousOnlineStop != null) TT(
                        time = stop.arrival + previousOnlineStop.delay,
                        color = colorOfDelayText(previousOnlineStop.delay).copy(alpha = a),
                    )
                } else if (onlineStop != null) TT(
                    time = stop.time + onlineStop.delay,
                    color = colorOfDelayText(onlineStop.delay).copy(alpha = a),
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
        Modifier,
        highlight = highlight,
    )

    Column(Modifier.weight(1F)) {
        filteredStops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                val middleDest = middleDestination(isOneWay, stops.map { it.name }, index)
                val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time }
                val highlighted = highlight == null || index in highlight
                val defaultColor =
                    if (movedNextStopIndex != null && index == movedNextStopIndex) MaterialTheme.colorScheme.secondary
                    else if (highlighted) MaterialTheme.colorScheme.onSurface else Colors.dimmedContent
                val a = if (movedNextStopIndex != null && index == movedNextStopIndex || highlighted) 1F else .5F

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
                    departs = stop.type != StopType.GetOffOnly && index < filteredStops.lastIndex,
                    direction = if (middleDest != null) Direction.NEGATIVE else direction,
                    line = stop.line,
                    platform = onlineStop?.platform ?: "",
                    modifier,
                    boxModifier,
                    color = color,
                )

                if (stop.arrival != null) {
                    TT(time = stop.time, Modifier.padding(end = 8.dp))
                    if (onlineStop != null) TT(
                        time = stop.time.coerceAtLeast(stop.arrival + onlineStop.delay),
                        Modifier.padding(end = 8.dp),
                        color = colorOfDelayText(
                            (stop.arrival + onlineStop.delay - stop.time).coerceAtLeast(0.minutes)
                        ).copy(alpha = a),
                    )
                }

                Row(Modifier.weight(1F), verticalAlignment = Alignment.CenterVertically) {
                    TT(time = stop.time, Modifier, Modifier.weight(1F, fill = false), text = stop.name)
                    if (stop.type != StopType.Normal) StopTypeIcon(stop.type, Modifier.padding(start = 8.dp), color = defaultColor)
                }
            }
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
    val bgColor = backgroundColorFor(MaterialTheme.colorScheme.onSurface)
    val lineColor = variantColorFor(MaterialTheme.colorScheme.onSurface)
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
    departs: Boolean,
    direction: Direction,
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
        if (departs) {
            DropdownMenuItem(
                text = {
                    Text("Zobrazit zastávkové JŘ")
                },
                onClick = {
                    onEvent(
                        TimetableEvent.TimetableClick(
                            line = line,
                            stop = stopName,
                            direction = direction,
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