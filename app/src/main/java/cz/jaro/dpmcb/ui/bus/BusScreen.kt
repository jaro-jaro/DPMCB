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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedCard
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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateWithOptionsFunction
import cz.jaro.dpmcb.ui.common.BusDoesNotExist
import cz.jaro.dpmcb.ui.common.BusDoesNotRun
import cz.jaro.dpmcb.ui.common.CodesAndShare
import cz.jaro.dpmcb.ui.common.DelayBubble
import cz.jaro.dpmcb.ui.common.Error
import cz.jaro.dpmcb.ui.common.Favouritificator
import cz.jaro.dpmcb.ui.common.Name
import cz.jaro.dpmcb.ui.common.Restriction
import cz.jaro.dpmcb.ui.common.SequenceRow
import cz.jaro.dpmcb.ui.common.Timetable
import cz.jaro.dpmcb.ui.common.Vehicle
import cz.jaro.dpmcb.ui.common.Wheelchair
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@Composable
fun Bus(
    args: Route.Bus,
    navController: NavHostController,
    viewModel: BusViewModel = run {
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
                    Name("${state.lineNumber}", subName = "/${state.busName.split('/')[1]}")
                    Wheelchair(
                        lowFloor = state.lowFloor,
                        confirmedLowFloor = (state as? BusState.OnlineRunning)?.confirmedLowFloor,
                        Modifier.padding(start = 8.dp),
                        enableCart = true,
                    )

                    if (state is BusState.OnlineRunning) DelayBubble(state.delayMin)
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
                    if (state !is BusState.OnlineRunning && state.error) Error()
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
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

@Composable
private fun Loading() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}