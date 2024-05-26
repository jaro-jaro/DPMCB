package cz.jaro.dpmcb.ui.bus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.ui.common.SequenceToBusTransitionData
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateWithOptionsFunction
import cz.jaro.dpmcb.ui.common.BusDoesNotExist
import cz.jaro.dpmcb.ui.common.BusDoesNotRun
import cz.jaro.dpmcb.ui.common.CodesAndShare
import cz.jaro.dpmcb.ui.common.Favouritificator
import cz.jaro.dpmcb.ui.common.Restriction
import cz.jaro.dpmcb.ui.common.SequenceRow
import cz.jaro.dpmcb.ui.common.Timetable
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.common.TransitionScope
import cz.jaro.dpmcb.ui.common.sharedElement
import cz.jaro.dpmcb.ui.sequence.DelayBubble
import cz.jaro.dpmcb.ui.sequence.Name
import cz.jaro.dpmcb.ui.sequence.SequenceState
import cz.jaro.dpmcb.ui.sequence.Vehicle
import cz.jaro.dpmcb.ui.sequence.Wheelchair
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

context(TransitionScope)
@Composable
fun Bus(
    args: Route.Bus,
    navController: NavHostController,
    viewModel: BusViewModel = Unit.run {
        val navigate = navController.navigateWithOptionsFunction
        koinViewModel {
            ParametersHolder(mutableListOf("${args.lineNumber}/${args.busNumber}", navigate))
        }
    },
) {
    title = R.string.detail_spoje
    App.selected = DrawerAction.FindBus

    val state by viewModel.state.collectAsStateWithLifecycle()

    BusScreen(
        state = state,
        navigate = navController.navigateFunction,
        onEvent = viewModel::onEvent,
    )
}

context(TransitionScope)
@Composable
fun BusScreen(
    state: BusState,
    navigate: NavigateFunction,
    onEvent: (BusEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        when (state) {
            is BusState.Loading -> Loading()

            is BusState.DoesNotExist -> BusDoesNotExist(state.busName)

            is BusState.DoesNotRun -> {
                BusDoesNotRun(state.runsNextTimeAfterToday, onEvent, state.runsNextTimeAfterDate, state.busName, state.date)

                CodesAndShare(state)
            }

            is BusState.OK -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Name(
                        "${state.lineNumber}", state.busName
                    )
                    Wheelchair(
                        lowFloor = state.lowFloor,
                        confirmedLowFloor = (state as? BusState.OnlineRunning)?.confirmedLowFloor,
                        key = state.busName,
                        Modifier
                            .padding(start = 8.dp),
                        enableCart = true,
                    )

                    if (state is BusState.OnlineRunning) DelayBubble(state.delayMin, state.busName)
                    if (state is BusState.OnlineRunning) Vehicle(state.vehicle)

                    Spacer(Modifier.weight(1F))

                    Favouritificator(
                        onEvent = onEvent,
                        busName = state.busName,
                        favouritePartOfConn = state.favourite,
                        stops = state.stops
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    SequenceRow(onEvent, state.sequenceName, state.nextBus != null, state.previousBus != null)
                    if (state.restriction) Restriction()
                    if (state !is BusState.OnlineRunning && state.error) cz.jaro.dpmcb.ui.common.Error()
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .sharedElement("timetable-${state.busName}")
                    ) {
                        Timetable(
                            navigate = navigate,
                            onlineConnStops = (state as? BusState.Online)?.onlineConnStops,
                            nextStopIndex = (state as? BusState.OnlineRunning)?.nextStopIndex,
                            stops = state.stops,
                            traveledSegments = state.traveledSegments,
                            height = state.lineHeight,
                            isOnline = state is BusState.OnlineRunning
                        )
                    }

                    CodesAndShare(state)
                }
            }
        }
    }
}

context(TransitionScope)
@Composable
private fun Loading() {
    val data = sharedTransitionData as? SequenceToBusTransitionData
    if (data != null) {
        val (bus, state) = data
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Name(
                "${bus.lineNumber}", bus.busName
            )
            Wheelchair(
                lowFloor = bus.lowFloor,
                confirmedLowFloor = (state as? SequenceState.Online)?.confirmedLowFloor?.takeIf { bus.isRunning },
                key = bus.busName,
                Modifier
                    .padding(start = 8.dp),
                enableCart = false,
            )

            if (state is SequenceState.Online && bus.isRunning) DelayBubble(state.delayMin, bus.busName)
            if (state is SequenceState.Online && bus.isRunning) Vehicle(state.vehicle)

            Spacer(Modifier.weight(1F))

            Favouritificator(
                onEvent = {},
                busName = bus.busName,
                favouritePartOfConn = null,
                stops = emptyList(),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SequenceRow(
                onEvent = { },
                sequenceName = state.sequenceName,
                hasNextBus = state.buses.indexOf(bus) != state.buses.lastIndex || state.after.size == 1,
                hasPreviousBus = state.buses.indexOf(bus) != 0 || state.before.size == 1
            )
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Timetable(
                    stops = bus.stops,
                    navigate = {},
                    onlineConnStops = null,
                    nextStopIndex = null,
                    showLine = true,
                    height = state.height,
                    traveledSegments = state.traveledSegments,
                    isOnline = state is SequenceState.Online
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton({}) {
                    Text("Spoj ${bus.busName}")
                }
            }
        }
    } else Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}