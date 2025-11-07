package cz.jaro.dpmcb.ui.connection_results

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TransferWithinAStation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.IndicatorBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScopeMarker
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.helperclasses.Offset
import cz.jaro.dpmcb.data.helperclasses.asString
import cz.jaro.dpmcb.data.helperclasses.asStringDM
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.nowFlow
import cz.jaro.dpmcb.data.helperclasses.rowItem
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.common.InvertedVehicleIcon
import cz.jaro.dpmcb.ui.common.Name
import cz.jaro.dpmcb.ui.common.invertedIconColor
import cz.jaro.dpmcb.ui.departures.asString
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@Suppress("unused")
@Composable
fun ConnectionResults(
    args: Route.ConnectionResults,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: ConnectionResultsViewModel = viewModel(args),
) {
    AppState.title = "Výsledky vyhledávání"
    AppState.selected = DrawerAction.Connection

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ConnectionResultsScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ConnectionResultsScreen(
    state: ConnectionResultState,
    onEvent: (ConnectionResultsEvent) -> Unit,
) = Column(
    Modifier
        .padding(horizontal = 8.dp)
        .fillMaxWidth()
) {
    Text(text = "${state.settings.start} -> ${state.settings.destination}")
    Text(text = "${state.settings.datetime.date.asString()} ${state.settings.datetime.time}")
    if (state.settings.directOnly) Text(text = "Zobrazují se pouze přímá spojení")

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.loadingPast,
        onRefresh = {
            onEvent(ConnectionResultsEvent.LoadPast)
        },
        Modifier.fillMaxWidth()
            .weight(1F),
        state = pullToRefreshState,
        indicator = {
            Indicator(pullToRefreshState, state)
        },
    ) {
        LazyColumn(
            contentPadding = WindowInsets.safeContent.only(WindowInsetsSides.Bottom).asPaddingValues(),
        ) {
            items(state.results, key = { it.def }) { connection ->
                OutlinedCard(
                    Modifier
                        .clickable {
                            onEvent(ConnectionResultsEvent.SelectConnection(connection.def, connection.departure.date))
                        }
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        Modifier.padding(all = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dateText = if (connection.departure.date == state.settings.datetime.date) "" else
                            connection.departure.date.asStringDM() + ". "
                        val now by nowFlow.collectAsStateWithLifecycle()
                        val runsIn = connection.departure - now
                        if (runsIn <= 0.hours || runsIn >= 5.hours)
                            Text(dateText + connection.departure.time.toString())
                        else
                            Text("Za " + runsIn.asString())
                        Text(connection.length.asString())
                    }
                    connection.ResultDetail()
                }
            }

            rowItem(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                LaunchedEffect(state.loading) {
                    if (!state.loading) onEvent(ConnectionResultsEvent.LoadMore)
                }
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BoxScope.Indicator(
    pullToRefreshState: PullToRefreshState,
    state: ConnectionResultState,
) {
    IndicatorBox(
        modifier = Modifier.align(Alignment.TopCenter),
        state = pullToRefreshState,
        isRefreshing = state.loadingPast,
        containerColor = PullToRefreshDefaults.indicatorContainerColor,
        maxDistance = PullToRefreshDefaults.IndicatorMaxDistance,
    ) {
        Crossfade(
            targetState = state.loadingPast,
            animationSpec = tween(durationMillis = 100)
        ) { refreshing ->
            if (refreshing) CircularProgressIndicator(
                Modifier.size(16.dp),
                strokeWidth = 2.5.dp,
                color = PullToRefreshDefaults.indicatorColor,
            ) else {
                val targetAlpha by remember {
                    derivedStateOf {
                        if (pullToRefreshState.distanceFraction >= 1F) 1F else .3F
                    }
                }
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(easing = LinearEasing)
                )
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    Modifier.size(24.dp),
                    tint = PullToRefreshDefaults.indicatorColor.copy(alpha = alpha),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
context(_: ColumnScope)
private fun ConnectionResult.ResultDetail() {
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        parts.forEachIndexed { i, bus ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InvertedVehicleIcon(bus.isTrolleybus)
                Name(
                    name = "${bus.line}",
                    color = invertedIconColor(bus.isTrolleybus),
                )
                if (i != parts.lastIndex) Icon(
                    imageVector = if (bus.transferTight || bus.transferLong) Icons.Default.TransferWithinAStation
                    else Icons.AutoMirrored.Default.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (bus.transferTight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 8.dp, end = 8.dp).height(IntrinsicSize.Max),
    ) {
        val transferRowHeight = mutableStateOf(0)
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${departure.time}")
            Text("${arrival.time}")
        }
        Line(transferRowHeight)
        Column(Modifier.weight(1F)) {
            Text(startStop, Modifier.fillMaxWidth())
            if (transfers.isNotEmpty()) {
                val transferCount = transfers.size
                val transfers = when (transferCount) {
                    1 -> "přestup"
                    2, 3, 4 -> "přestupy"
                    else -> "přestupů"
                }
                Text(
                    text = "$transferCount $transfers: ${this@ResultDetail.transfers.joinToString()}",
                    Modifier.onSizeChanged {
                        transferRowHeight.value = it.height
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(endStop, Modifier.fillMaxWidth())
        }
    }
}

@Composable
context(_: RowScope)
private fun ConnectionResult.Line(
    transferRowHeightState: State<Int>,
) {
    val bgColor = MaterialTheme.colorScheme.surface
    val trolleybuses = parts.map { bus ->
        bus.isTrolleybus
    }
    val colors = trolleybuses.map { isTrolleybus ->
        invertedIconColor(isTrolleybus)
    }
    val busesLength = parts.fold(Duration.ZERO) { acc, bus -> acc + bus.length }
    val lengths = parts.map { bus ->
        (bus.length / busesLength).toFloat()
    }
    val busCount = parts.size
    Canvas(
        Modifier
            .padding(horizontal = 8.dp)
            .fillMaxHeight()
            .width(24.dp),
        contentDescription = "Trasa",
    ) {
        val transferRowHeight = transferRowHeightState.value * 1F
        val canvasHeight = size.height
        val canvasWidth = size.width
        val lineWidth = 3.dp.toPx()
        val lineXOffset = canvasWidth / 2
        val circleRadius = 7.dp.toPx()
        val circleStrokeWidth = 3.dp.toPx()
        val rowHeight = (canvasHeight - transferRowHeight) / 2
        val transferLength = 4.dp.toPx()
        val transfersLength = transferLength * (busCount - 1)
        val lineLength = canvasHeight - rowHeight
        val busLineLength = lineLength - transfersLength - circleRadius * 2

        translate(left = lineXOffset, top = rowHeight * .5F) {
            drawLine(
                color = colors.first(),
                start = Offset(),
                end = Offset(y = circleRadius),
                strokeWidth = lineWidth,
            )
            (colors zip lengths).foldIndexed(circleRadius) { i, previous, (color, length) ->
                val transfer = if (i != 0) transferLength else 0F
                translate(top = previous) {
                    if (i != 0) drawLine(
                        color = bgColor,
                        start = Offset(),
                        end = Offset(y = transferLength),
                        strokeWidth = lineWidth,
                    )
                    translate(top = transfer) {
                        drawLine(
                            color = color,
                            start = Offset(),
                            end = Offset(y = busLineLength * length),
                            strokeWidth = lineWidth,
                        )
                    }
                }
                previous + transfer + busLineLength * length
            }
            drawLine(
                color = colors.last(),
                start = Offset(y = lineLength - circleRadius),
                end = Offset(y = lineLength),
                strokeWidth = lineWidth,
            )
            drawOutlinedCircle(colors.first(), bgColor, Offset(), circleRadius, circleStrokeWidth)
            drawOutlinedCircle(colors.last(), bgColor, Offset(y = lineLength), circleRadius, circleStrokeWidth)
        }
    }
}

@DrawScopeMarker
fun DrawScope.drawFilledCircle(
    color: Color,
    center: Offset,
    radius: Float,
) = drawCircle(
    color = color,
    radius = radius,
    center = center,
    style = Fill
)

@DrawScopeMarker
fun DrawScope.drawOutlinedCircle(
    color: Color,
    bgColor: Color,
    center: Offset,
    radius: Float,
    strokeWidth: Float,
) {
    drawCircle(
        color = bgColor,
        radius = radius,
        center = center,
        style = Fill
    )
    drawCircle(
        color = color,
        radius = radius - strokeWidth / 2,
        center = center,
        style = Stroke(
            width = strokeWidth,
        )
    )
}
