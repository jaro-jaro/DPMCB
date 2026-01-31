package cz.jaro.dpmcb.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.Offset
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayText
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.onSecondaryClick
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.PartOfConn
import cz.jaro.dpmcb.data.realtions.canGetOn
import cz.jaro.dpmcb.data.realtions.isNullOrEmpty
import cz.jaro.dpmcb.ui.theme.Colors
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.minutes

sealed interface TimetableEvent {
    data class StopClick(val stopName: String, val time: LocalDateTime) : TimetableEvent
    data class TimetableClick(val line: ShortLine, val stop: String, val platform: Platform, val direction: Direction,) : TimetableEvent
}

@Composable
fun Timetable(
    stops: List<BusStop>,
    onEvent: (TimetableEvent) -> Unit,
    onlineConnStops: List<OnlineConnStop>?,
    nextStopIndex: Int?,
    showLine: Boolean = true,
    traveledSegments: Int = 0,
    position: Float = 0F,
    isOnline: Boolean = false,
    part: PartOfConn? = null,
    highlight: IntRange? = null,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
) {
    val filteredStops =
        if (!part.isNullOrEmpty()) stops.withIndex().toList().slice(part) else stops.withIndex().toList()
    val lastIndex = stops.lastIndex
    val rowHeights = remember { mutableStateListOf(*Array(stops.size) { 0F }) }
    val leftPartWidths = remember { mutableStateListOf(*Array(stops.size) { 0F }) }
    val density = LocalDensity.current
    val leftPartMaxWidth = with(density) { leftPartWidths.max().toDp() }
    val segmentLengths = remember(rowHeights.toList()) {
        rowHeights.zipWithNext { a, b -> a / 2 + b / 2 }
    }
    val lineLength = remember(segmentLengths) {
        segmentLengths.sum()
    }
    val distance = remember(segmentLengths, traveledSegments, position) {
        if (traveledSegments == stops.lastIndex) return@remember segmentLengths.sum()
        val posRelativeToThisStop = position - traveledSegments
        val currentSegmentLength = segmentLengths[traveledSegments]
        val distanceInCurrentSegment = currentSegmentLength * posRelativeToThisStop
        val previous = segmentLengths.take(traveledSegments).sum()
        previous + distanceInCurrentSegment
    }
    val animatedDistance = animateFloatAsState(distance, label = "HeightAnimation")

    filteredStops.forEachIndexed { index, (indexOnBus, stop) ->
        val isFirst = indexOnBus == 0
        val isLast = indexOnBus == lastIndex
        val passed = traveledSegments >= indexOnBus
        val onlineStop = onlineConnStops?.find { it.scheduledTime == stop.time.time }
        val previousOnlineStop = onlineConnStops?.getOrNull(onlineConnStops.indexOf(onlineStop) - 1)
        val highlighted = highlight == null || indexOnBus in highlight
        val defaultColor =
            if (indexOnBus == nextStopIndex) MaterialTheme.colorScheme.secondary
            else if (highlighted) MaterialTheme.colorScheme.onSurface
            else Colors.dimmedContent
        val a = if (indexOnBus == nextStopIndex || highlighted) 1F else .5F
        val platform = onlineStop?.platform ?: stop.platform
        val departs = stop.type.canGetOn && index < filteredStops.lastIndex

        var showDropDown by rememberSaveable { mutableStateOf(false) }

        DropdownMenu(
            expanded = showDropDown,
            onDismissRequest = {
                showDropDown = false
            }
        ) {
            DropdownMenuItem({ Text(stop.name) }, onClick = {}, enabled = false)
            DropdownMenuItem(
                text = { Text("Zobrazit odjezdy") },
                onClick = {
                    onEvent(TimetableEvent.StopClick(stop.name, stop.time))
                    showDropDown = false
                },
            )
            if (departs && stop.platform != null) DropdownMenuItem(
                text = { Text("Zobrazit zastávkové JŘ") },
                onClick = {
                    onEvent(TimetableEvent.TimetableClick(stop.line, stop.name, stop.platform, stop.direction)) // TODO: direction on onw-way lines works differently
                    showDropDown = false
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .defaultMinSize(minHeight = 24.dp)
                .onSizeChanged {
                    rowHeights[index] = it.height.toFloat()
                }
                .combinedClickable(
                    onClick = {
                        onEvent(TimetableEvent.StopClick(stop.name, stop.time))
                    },
                    onLongClick = {
                        showDropDown = true
                    },
                )
                .onSecondaryClick(Unit) {
                    showDropDown = true
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .widthIn(min = leftPartMaxWidth)
                    .onSizeChanged {
                        leftPartWidths[index] = it.width.toFloat()
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${stop.arrival?.time ?: stop.time.time}",
                    color = defaultColor,
                )
                if (stop.arrival != null && previousOnlineStop != null) Text(
                    text = "${(stop.arrival + previousOnlineStop.delay).time}",
                    color = colorOfDelayText(previousOnlineStop.delay).copy(alpha = a),
                ) else if (onlineStop != null) Text(
                    text = "${(stop.time + onlineStop.delay).time}",
                    color = colorOfDelayText(onlineStop.delay).copy(alpha = a),
                )
            }

            if (showLine) Line(
                height = position,
                distance = animatedDistance,
                lineLength = lineLength,
                isOnline = isOnline,
                isFirst = isFirst,
                isLast = isLast,
                i = index,
                passed = passed,
                highlight = part?.let { part ->
                    highlight?.let { (it.first - part.start)..(it.last - part.start) } ?: 0..part.count()
                } ?: highlight,
            )

            if (stop.arrival != null) {
                Text(
                    text = "${stop.time.time}",
                    color = defaultColor,
                )
                if (onlineStop != null) Text(
                    text = "${stop.time.coerceAtLeast(stop.arrival + onlineStop.delay).time}",
                    color = colorOfDelayText((stop.arrival + onlineStop.delay - stop.time).coerceAtLeast(0.minutes)).copy(alpha = a),
                )
            }

            Text(stopNameText(stop.name, platform, stop.type), Modifier.weight(1F), color = defaultColor)
        }
    }
}

@Composable
fun Line(
    height: Float,
    distance: State<Float>,
    lineLength: Float,
    isOnline: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    i: Int,
    passed: Boolean,
    modifier: Modifier = Modifier,
    highlight: IntRange? = null,
) {
    val passedColor = if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
    val busColor = MaterialTheme.colorScheme.secondary
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface
    val lineColor = Colors.dimmedContent

    val highlighted = highlight != null && i in highlight

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .requiredWidth(16.dp),
        contentDescription = "Poloha spoje"
    ) {
        val rowHeight = size.height
        val lineWidth = 3.dp.toPx()
        val lineXOffset = size.width / 2
        val circleRadius = 5.5.dp.toPx()
        val circleStrokeWidth = 3.dp.toPx()

        translate(left = lineXOffset, top = rowHeight / 2) {
            if (!isFirst) drawLine(
                color = lineColor,
                start = Offset(y = -rowHeight / 2),
                end = Offset(),
                strokeWidth = lineWidth,
            )
            if (!isLast) drawLine(
                color = lineColor,
                start = Offset(),
                end = Offset(y = rowHeight / 2),
                strokeWidth = lineWidth,
            )
            if (highlight != null && highlight.first < i && i <= highlight.last) drawLine(
                color = highlightColor,
                start = Offset(y = -rowHeight / 2),
                end = Offset(),
                strokeWidth = lineWidth,
            )
            if (highlight != null && highlight.first <= i && i < highlight.last) drawLine(
                color = highlightColor,
                start = Offset(),
                end = Offset(y = rowHeight / 2),
                strokeWidth = lineWidth,
            )

            drawCircle(
                color = if (passed && highlight == null) passedColor else bgColor,
                radius = circleRadius,
                center = Offset(),
                style = Fill
            )
            drawCircle(
                color = if (passed && highlight == null) passedColor else if (highlighted) highlightColor else lineColor,
                radius = circleRadius,
                center = Offset(),
                style = Stroke(
                    width = circleStrokeWidth
                )
            )

            if (isLast && height > 0F) translate(top = -lineLength) {
                if (highlight == null) drawLine(
                    color = passedColor,
                    start = Offset(),
                    end = Offset(y = distance.value),
                    strokeWidth = lineWidth,
                )

                drawCircle(
                    color = busColor,
                    radius = circleRadius - circleStrokeWidth * .5F,
                    center = Offset(y = distance.value)
                )
            }
        }
    }
}