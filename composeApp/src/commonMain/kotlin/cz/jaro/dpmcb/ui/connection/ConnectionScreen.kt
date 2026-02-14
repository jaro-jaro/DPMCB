package cz.jaro.dpmcb.ui.connection

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TransferWithinAStation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.Logger
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.Offset
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.asStringDM
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.nowFlow
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.Name
import cz.jaro.dpmcb.ui.common.invertedIconColor
import cz.jaro.dpmcb.ui.common.invertedVehicleIconGraphicsLayer
import cz.jaro.dpmcb.ui.common.stopNameText
import cz.jaro.dpmcb.ui.connection_results.drawFilledCircle
import cz.jaro.dpmcb.ui.connection_results.drawOutlinedCircle
import cz.jaro.dpmcb.ui.departures.asString
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

context(logger: Logger)
@Suppress("unused")
@Composable
fun Connection(
    args: Route.Connection,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: ConnectionViewModel = viewModel(args),
) {
    AppState.title = "Spojení"
    AppState.selected = DrawerAction.Connection

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ConnectionScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

context(logger: Logger)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ConnectionScreen(
    state: ConnectionState?,
    onEvent: (ConnectionEvent) -> Unit,
) = Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
) {
    if (state != null) OutlinedCard(
        Modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
    ) {
        Row(
            Modifier.padding(all = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val dateText = if (state.start.date == SystemClock.todayHere()) "" else
                state.start.date.asStringDM() + " "
            val now by nowFlow.collectAsStateWithLifecycle()
            val runsIn = state.start - now
            if (runsIn <= 0.hours || runsIn >= 5.hours)
                Text(dateText + state.start.time.toString())
            else
                Text("Za " + runsIn.asString())
            Text(state.length.asString())
        }
        HorizontalDivider(Modifier.fillMaxWidth())
        Column(Modifier.animateContentSize().fillMaxWidth().padding(horizontal = 8.dp)) {
            val textMeasurer = rememberTextMeasurer(64)
            val maxWidth = textMeasurer.measureDurationsWidth(state.buses, state.coordinates)
            RecursiveDetails(onEvent, state.buses, state.start.date, state.coordinates, maxWidth, textMeasurer)
        }
    }
}

@Composable
private fun TextMeasurer.measureDurationsWidth(
    alternatives: Alternatives,
    coordinates: Coordinates,
) = alternatives.currentDurations(coordinates).filterNotNull().distinct().maxOfOrNull {
    measure(it.asString(), style = LocalTextStyle.current).size.width
} ?: 0

fun Alternatives.currentDurations(coordinates: Coordinates): List<Duration?> {
    if (coordinates.isEmpty()) return emptyList()
    val first = coordinates.first()
    val rest = coordinates.drop(1)
    val tree = this[first]
    return tree.next.currentDurations(rest) + tree.part?.transferTime + tree.part?.length
}

context(logger: Logger)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecursiveDetails(
    onEvent: (ConnectionEvent) -> Unit,
    alternatives: Alternatives,
    startDate: LocalDate,
    setCoordinates: Coordinates,
    maxWidth: Int,
    textMeasurer: TextMeasurer,
    level: Int = 0,
    phase: Int = 0,
    isInSetConnection: Boolean = true,
) {
    val setPage = setCoordinates.first()
    val isFirst = level == 0
    val pagerState = rememberPagerState(initialPage = 0) { alternatives.size + 1 }
    LaunchedEffect(Unit) {
        pagerState.scrollToPage(0)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (!isInSetConnection) return@LaunchedEffect
        val newPage = pagerState.currentPage
        if (newPage == setPage + 1 || newPage == setPage - 1)
            onEvent(ConnectionEvent.OnSwipe(level, newPage))
    }
    HorizontalPager(
        state = pagerState,
        Modifier,
        beyondViewportPageCount = 0,
        verticalAlignment = Alignment.Top,
    ) { page ->
        val tree = alternatives.getOrNull(page)
        val bus = tree?.part
        if (page == alternatives.size || tree == null || bus == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            }
        } else Column(
            Modifier.animateContentSize(),
        ) {
            val totalHeight = remember { mutableStateOf(0) }
            val next = tree.next
            val isLast = next.isEmpty()
            val localCoordinates: Coordinates = listOf(page) + next.getCoordinatesOfFirstConnection()
            val localMaxWidth = textMeasurer.measureDurationsWidth(alternatives, localCoordinates)
            val maxWidth = if (page == setPage) maxWidth else localMaxWidth
            BusDetails(
                onEvent, startDate, bus, maxWidth, level, isFirst, isLast, phase,
                Modifier.onSizeChanged {
                    totalHeight.value = it.height
                }
            )
            if (!isLast) RecursiveDetails(
                onEvent = onEvent,
                alternatives = next,
                startDate = startDate,
                setCoordinates = if (page == setPage) setCoordinates.drop(1) else localCoordinates.drop(1),
                maxWidth = maxWidth,
                textMeasurer = textMeasurer,
                level = level + 1,
                phase = phase + totalHeight.value,
                isInSetConnection = page == setPage,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BusDetails(
    onEvent: (ConnectionEvent) -> Unit,
    startDate: LocalDate,
    bus: ConnectionBus,
    maxWidth: Int,
    level: Int,
    isFirst: Boolean,
    isLast: Boolean,
    phase: Int,
    modifier: Modifier = Modifier,
) = Column {
    val transferRowHeight = remember { mutableStateOf(null as Int?) }
    val density = LocalDensity.current

    Row(
        modifier
            .height(IntrinsicSize.Max)
    ) {
        Column(
            Modifier.animateContentSize(alignment = Alignment.CenterEnd).fillMaxHeight().width(with(density) { maxWidth.toDp() }),
            horizontalAlignment = Alignment.End,
        ) {
            val transferRowHeightDp = with(density) { transferRowHeight.value?.toDp() }
            if (bus.transferTime != null) Box(
                Modifier.height(transferRowHeightDp ?: 0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = bus.transferTime.asString(),
                    color = if (bus.transferTight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Visible,
                )
            }
        }

        Spacer(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxHeight()
                .width(24.dp)
        )

        if (bus.transferTime != null) Row(
            Modifier.weight(1F).onSizeChanged {
                transferRowHeight.value = it.height
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconWithTooltip(
                imageVector = Icons.Default.TransferWithinAStation,
                contentDescription = "Přestup",
                Modifier.padding(end = 8.dp).size(16.dp),
                tint = if (bus.transferTight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "přestup",
                Modifier.weight(1F),
                color = if (bus.transferTight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(
        modifier
            .clickable {
                onEvent(ConnectionEvent.SelectBus(level))
            }
            .height(IntrinsicSize.Max)
    ) {
        val busRowHeight = remember { mutableStateOf(0) }
        val directionRowHeight = remember { mutableStateOf(0) }
        val departureRowHeight = remember { mutableStateOf(0) }
        val arrivalRowHeight = remember { mutableStateOf(0) }

        val departureRowHeightDp = with(density) { departureRowHeight.value.toDp() }
        val busRowHeightDp = with(density) { busRowHeight.value.toDp() }
        val directionRowHeightDp = with(density) { directionRowHeight.value.toDp() }
        val arrivalRowHeightDp = with(density) { arrivalRowHeight.value.toDp() }

        Column(
            Modifier.animateContentSize(alignment = Alignment.CenterEnd).fillMaxHeight().width(with(density) { maxWidth.toDp() }),
            horizontalAlignment = Alignment.End,
        ) {
            val dateText = if (bus.date == startDate) "" else bus.date.asStringDM() + " "
            Box(Modifier.height(departureRowHeightDp), contentAlignment = Alignment.CenterEnd) {
                Text("${dateText}${bus.departure}")
            }
            Box(Modifier.height(busRowHeightDp), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = bus.length.asString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Visible,
                )
            }
            Spacer(Modifier.height(directionRowHeightDp))
            Box(Modifier.height(arrivalRowHeightDp), contentAlignment = Alignment.CenterEnd) {
                Text("${dateText}${bus.arrival}")
            }
        }

        Line(bus, isFirst, isLast, busRowHeight, directionRowHeight, transferRowHeight, departureRowHeight, arrivalRowHeight, phase)

        Column(Modifier.weight(1F)) {
            Row(Modifier.onSizeChanged {
                departureRowHeight.value = it.height
            }) {
                Text(stopNameText(bus.startStop, bus.startStopPlatform))
            }
            Row(Modifier.onSizeChanged {
                busRowHeight.value = it.height
            }) {
                Name(
                    name = "${bus.line.toShortLine()}",
                    Modifier.padding(end = 8.dp),
                    suffix = "/" + bus.bus.bus(),
                    color = invertedIconColor(bus.isTrolleybus),
                )
            }
            Row(Modifier.onSizeChanged {
                directionRowHeight.value = it.height
            }) {
                val stops = when (bus.stopCount) {
                    1 -> "zastávka"
                    2, 3, 4 -> "zastávky"
                    else -> "zastávek"
                }
                Text("-> ${bus.direction}, ${bus.stopCount} $stops", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.onSizeChanged {
                arrivalRowHeight.value = it.height
            }) {
                Text(stopNameText(bus.endStop, bus.endStopPlatform))
            }
        }
    }
}

@Composable
private fun Line(
    bus: ConnectionBus,
    isFirst: Boolean,
    isLast: Boolean,
    busRowHeightState: State<Int>,
    directionRowHeightState: State<Int>,
    transferRowHeightState: State<Int?>,
    departureRowHeightState: State<Int>,
    arrivalRowHeightState: State<Int>,
    phase: Int,
) {
    val transferColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor = MaterialTheme.colorScheme.surface
    val isTrolleybus = bus.isTrolleybus
    val lineColor = invertedIconColor(isTrolleybus)
    val layer = invertedVehicleIconGraphicsLayer(isTrolleybus, size = 24.dp)

    Canvas(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxHeight()
            .width(24.dp),
        contentDescription = "Trasa",
    ) {
        val canvasWidth = size.width
        val lineWidth = 3.dp.toPx()
        val lineDashLength = 5.dp.toPx()
        val lineXOffset = canvasWidth / 2
        val circleRadius = 7.dp.toPx()
        val circleStrokeWidth = 3.dp.toPx()

        translate(left = lineXOffset) {
            val departureRowHeight = departureRowHeightState.value * 1F
            val busRowHeight = busRowHeightState.value * 1F
            val directionRowHeight = directionRowHeightState.value * 1F
            val arrivalRowHeight = arrivalRowHeightState.value * 1F
            val transferRowHeight = (transferRowHeightState.value ?: 0) * 1F
            val busIconOffset = (busRowHeight - canvasWidth) / 2

            val lineEnd = departureRowHeight + busRowHeight + directionRowHeight + arrivalRowHeight / 2
            val lineStart = departureRowHeight / 2
            if (!isFirst) drawLine(
                color = transferColor,
                start = Offset(y = -transferRowHeight),
                end = Offset(y = lineStart),
                strokeWidth = lineWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(lineDashLength, lineDashLength), phase * 1F),
            )
            drawLine(
                color = lineColor,
                start = Offset(y = lineStart),
                end = Offset(y = lineEnd),
                strokeWidth = lineWidth,
            )
            if (!isLast) drawLine(
                color = transferColor,
                start = Offset(y = lineEnd),
                end = Offset(y = lineEnd + arrivalRowHeight / 2),
                strokeWidth = lineWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(lineDashLength, lineDashLength), phase * 1F + lineEnd),
            )
            translate(left = -lineXOffset, top = departureRowHeight + busIconOffset) {
                drawLayer(layer)
//                drawRoundRect(lineColor, Offset(), Size(canvasWidth, canvasWidth), cornerRadius = CornerRadius(canvasWidth / 4, canvasWidth / 4))
            }
            if (isFirst) drawOutlinedCircle(lineColor, bgColor, Offset(y = lineStart), circleRadius, circleStrokeWidth)
            else drawFilledCircle(lineColor, Offset(y = lineStart), circleRadius)
            if (isLast) drawOutlinedCircle(lineColor, bgColor, Offset(y = lineEnd), circleRadius, circleStrokeWidth)
            else drawFilledCircle(lineColor, Offset(y = lineEnd), circleRadius)
        }
    }
}
