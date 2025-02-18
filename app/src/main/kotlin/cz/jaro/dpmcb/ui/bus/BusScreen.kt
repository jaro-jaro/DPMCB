package cz.jaro.dpmcb.ui.bus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.helperclasses.navigateWithOptionsFunction
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.ui.common.DateSelector
import cz.jaro.dpmcb.ui.common.DelayBubble
import cz.jaro.dpmcb.ui.common.Name
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
    superNavController: NavHostController,
    viewModel: BusViewModel = run {
        val navigate = navController.navigateWithOptionsFunction
        koinViewModel {
            ParametersHolder(mutableListOf(args.lineNumber / args.busNumber, navigate, args.date))
        }
    },
) {
    title = R.string.detail_spoje
    App.selected = DrawerAction.FindBus

    LaunchedEffect(Unit) {
        viewModel.navigate = navController.navigateWithOptionsFunction
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    BusScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun BusScreen(
    state: BusState,
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

            is BusState.OK -> Box {
                val graphicsLayerWhole = rememberGraphicsLayer()
                val graphicsLayerPart = rememberGraphicsLayer()
                var part by remember { mutableStateOf(PartOfConn.Empty(state.busName)) }

                ShareLayout(graphicsLayerWhole, state, null)
                ShareLayout(graphicsLayerPart, state, part)

                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Name("${state.lineNumber}", subName = "/${state.busName.bus()}")
                        Wheelchair(
                            lowFloor = state.lowFloor,
                            confirmedLowFloor = state.online?.running?.confirmedLowFloor,
                            Modifier.padding(start = 8.dp),
                            enableCart = true,
                        )

                        Spacer(Modifier.weight(1F))

                        DateSelector(
                            date = state.date,
                            onDateChange = {
                                onEvent(BusEvent.ChangeDate(it))
                            }
                        )

                        Favouritificator(
                            onEvent = onEvent,
                            busName = state.busName,
                            favouritePartOfConn = state.favourite,
                            stops = state.stops
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.online?.running?.delayMin != null) DelayBubble(state.online.running.delayMin)
                        if (state.online?.running != null) Vehicle(state.online.running.vehicle)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        SequenceRow(onEvent, state.sequenceName, state.nextBus != null, state.previousBus != null)
                        if (state.restriction) Restriction()
                        if ((state.online == null || state.online.running == null) && state.error) Error()
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Timetable(
                                stops = state.stops,
                                onEvent = onEvent.fromTimetable,
                                onlineConnStops = state.online?.onlineConnStops,
                                nextStopIndex = state.online?.running?.nextStopIndex,
                                traveledSegments = state.traveledSegments,
                                height = state.lineHeight,
                                isOnline = state.online?.running != null,
                            )
                        }

                        CodesAndShare(
                            state = state,
                            graphicsLayerWhole = graphicsLayerWhole,
                            graphicsLayerPart = graphicsLayerPart,
                            part = part,
                            editPart = { part = it(part) }
                        )
                    }
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