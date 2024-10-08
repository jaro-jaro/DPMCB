package cz.jaro.dpmcb.ui.sequence

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.ui.common.DelayBubble
import cz.jaro.dpmcb.ui.common.Name
import cz.jaro.dpmcb.ui.common.Timetable
import cz.jaro.dpmcb.ui.common.Vehicle
import cz.jaro.dpmcb.ui.common.Wheelchair
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Sequence(
    args: Route.Sequence,
    navController: NavHostController,
    viewModel: SequenceViewModel = koinViewModel {
        parametersOf(
            SequenceViewModel.Parameters(
                sequence = SequenceCode("${args.sequenceNumber}/${args.lineAndModifiers}"),
            )
        )
    },
) {
    title = R.string.detail_kurzu
    App.selected = DrawerAction.FindBus

    LaunchedEffect(Unit) {
        viewModel.navigate = navController.navigateFunction
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    SequenceScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SequenceScreen(
    state: SequenceState,
    onEvent: (SequenceEvent) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
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
                Text("Tento kurz (${state.sequenceName}) bohužel ${state.date.asString()} neexistuje :(\nBuď jste zadali špatně jeho název, nebo tento kurz existuje v jiném jízdním řádu, který dnes neplatí.")
            }

            is SequenceState.OK -> {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Name(state.sequenceName)

                        if (state is SequenceState.Online && state.confirmedLowFloor != null) Wheelchair(
                            lowFloor = state.confirmedLowFloor,
                            confirmedLowFloor = state.confirmedLowFloor,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state is SequenceState.Online) DelayBubble(state.delayMin)
                        if (state.vehicle != null) Vehicle(state.vehicle)
                        else VehicleSearcher(onEvent)
                    }
                }

                item {
                    state.fixedCodes.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.timeCodes.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(state.lineCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                item {
                    HorizontalDivider(Modifier.fillMaxWidth())
                }

                state.before.forEach {
                    item {
                        Connection(onEvent, it)
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
                                Name("${bus.lineNumber}", subName = "/${bus.busName.bus()}")
                                Wheelchair(
                                    lowFloor = bus.lowFloor,
                                    confirmedLowFloor = (state as? SequenceState.Online)?.confirmedLowFloor?.takeIf { bus.isRunning },
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                                Spacer(Modifier.weight(1F))

                                BusButton(onEvent, bus)
                            }
                        }
                        Surface {
                            if (bus.isRunning && state is SequenceState.Online) Row(
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
                                onEvent = onEvent.fromTimetable,
                                onlineConnStops = null,
                                nextStopIndex = null,
                                showLine = bus.isRunning || (state.buses.none { it.isRunning } && bus.shouldBeRunning),
                                traveledSegments = state.traveledSegments,
                                height = state.height,
                                isOnline = state is SequenceState.Online,
                            )
                        }
                    }
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth(1F)
                                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        ) {
                            bus.fixedCodes.forEach {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            bus.timeCodes.forEach {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(bus.lineCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Connection(onEvent, it)
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}