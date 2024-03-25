package cz.jaro.dpmcb.ui.sequence

import android.annotation.SuppressLint
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.colorOfDelayBubbleContainer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.colorOfDelayBubbleText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.regN
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDelay
import cz.jaro.dpmcb.ui.bus.Timetable
import cz.jaro.dpmcb.ui.destinations.BusDestination
import cz.jaro.dpmcb.ui.destinations.SequenceDestination
import cz.jaro.dpmcb.ui.main.DrawerAction
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import java.time.LocalTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@Destination
@Composable
fun Sequence(
    sequence: String,
    viewModel: SequenceViewModel = koinViewModel {
        ParametersHolder(mutableListOf(sequence))
    },
    navigator: DestinationsNavigator,
) {
    title = R.string.detail_kurzu
    App.selected = DrawerAction.FindBus

    val state by viewModel.state.collectAsStateWithLifecycle()

    SequenceScreen(
        state = state,
        navigate = navigator.navigateFunction,
        lazyListState = rememberLazyListState(),
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SequenceScreen(
    state: SequenceState,
    navigate: NavigateFunction,
    lazyListState: LazyListState,
) = Scaffold(
    floatingActionButton = {
        if (state is SequenceState.OK) FABs(state, lazyListState)
    }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        state = lazyListState
    ) {
        when (state) {
            is SequenceState.Loading -> rowItem(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            is SequenceState.DoesNotExist -> rowItem(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Tento kurz (${state.sequenceName}) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
            }

            is SequenceState.OK -> {
                item {
                    FlowRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Name(state.sequenceName)

                        if (state is SequenceState.OK.Online && state.confirmedLowFloor != null) Wheelchair(
                            lowFloor = state.confirmedLowFloor,
                            confirmedLowFloor = state.confirmedLowFloor,
                            modifier = Modifier.padding(start = 8.dp),
                        )

                        if (state is SequenceState.OK.Online) DelayBubble(state.delayMin)
                        if (state is SequenceState.OK.Online) Vehicle(state.vehicle)
                    }
                }

                item {
                    state.fixedCodes.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.timeCodes.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    HorizontalDivider(Modifier.fillMaxWidth())
                }

                state.before.forEach {
                    item {
                        Connection(navigate, it)
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                }

                state.buses.forEach { bus ->
                    stickyHeader {
                        Surface {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Name("${bus.lineNumber}")
                                Wheelchair(
                                    lowFloor = bus.lowFloor,
                                    confirmedLowFloor = (state as? SequenceState.OK.Online)?.confirmedLowFloor?.takeIf { bus.isRunning },
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                                Spacer(Modifier.weight(1F))

                                BusButton(navigate, bus)
                            }
                        }
                        Surface {
                            if (bus.isRunning && state is SequenceState.OK.Online) Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DelayBubble(state.delayMin)
                                Vehicle(state.vehicle)
                            }
                        }

                    }
                    item {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Timetable(
                                stops = bus.stops,
                                navigate = navigate,
                                onlineConnStops = null,
                                nextStopTime = null,
                                showLine = bus.isRunning || (state.buses.none { it.isRunning } && bus.shouldBeRunning),
                                height = state.height,
                                traveledSegments = state.traveledSegments,
                                isOnline = state is SequenceState.OK.Online
                            )
                        }
                    }
                    item {
                        HorizontalDivider(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }
                }

                stickyHeader { }

                state.after.forEach {
                    item {
                        Connection(navigate, it)
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun FABs(state: SequenceState.OK, lazyListState: LazyListState) {
    fun Int.busIndexToListIndex() = 3 + state.before.count() * 2 + this * 3

    val now = remember(state.buses) {
        state.buses.indexOfFirst {
            it.isRunning
        }.takeUnless {
            it == -1
        } ?: state.buses.indexOfFirst {
            LocalTime.now() < it.stops.first().time
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
private fun BusButton(
    navigate: NavigateFunction,
    bus: BusInSequence,
) = TextButton(
    onClick = {
        navigate(BusDestination(busId = bus.busId))
    }
) {
    Text("Detail spoje")
}

@Composable
private fun Connection(
    navigate: NavigateFunction,
    sequence: Pair<String, String>,
) = TextButton(
    onClick = {
        navigate(SequenceDestination(sequence.first))
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
            Modifier.padding(horizontal = 8.dp)
        )
        val context = LocalContext.current
        IconWithTooltip(
            Icons.Default.Info,
            "Zobrazit informace o voze",
            Modifier.clickable {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(context, Uri.parse("https://seznam-autobusu.cz/seznam?operatorName=DP+města+České+Budějovice&prov=1&evc=$vehicle"))
            },
        )
    }
}

@Composable
fun DelayBubble(delayMin: Float) {
    Badge(
        containerColor = colorOfDelayBubbleContainer(delayMin),
        contentColor = colorOfDelayBubbleText(delayMin),
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
fun Name(name: String) {
    Text(name, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
}