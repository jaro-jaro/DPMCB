package cz.jaro.dpmcb.ui.now_running

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.regN
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.textItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDelay
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes

@Composable
fun NowRunning(
    args: Route.NowRunning,
    navController: NavHostController,
    viewModel: NowRunningViewModel = run {
        val navigate = navController.navigateFunction
        val getNavDestination = { navController.currentBackStackEntry?.destination }
        koinViewModel {
            parametersOf(NowRunningViewModel.Parameters(filters = args.filters.toList(), type = args.type, navigate = navigate, getNavDestination = getNavDestination))
        }
    },
) {
    App.title = R.string.now_running
    App.selected = DrawerAction.NowRunning

    val state by viewModel.state.collectAsStateWithLifecycle()

    NowRunningScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NowRunningScreen(
    state: NowRunningState,
    onEvent: (NowRunningEvent) -> Unit,
) {
    when (state) {
        NowRunningState.IsNotToday -> Text(
            text = "Pro zobrazení právě jedoucích spojů si změňte datum na dnešek",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        NowRunningState.Offline -> Text(
            text = "Jste offline :(",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        NowRunningState.NoLines -> Text(
            text = "Bohužel, zdá se že právě nejede žádná linka. Toto může také nastat pokud má Dopravní podnik výpadek svých informačních serverů. V takovém případě nefungují aktuální informace o spojích ani kdekoliv jinde, včetně zastávkových označníků ve městě.",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        is NowRunningState.HasType -> Column {
            Text(
                "Řadit podle:", modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                NowRunningType.entries.forEach { type ->
                    FilterChip(
                        modifier = Modifier
                            .padding(all = 4.dp),
                        selected = type == state.type,
                        onClick = {
                            onEvent(NowRunningEvent.ChangeType(type))
                        },
                        label = { Text(type.label) }
                    )
                }
            }
            when (state) {
                is NowRunningState.LoadingLines -> Text(text = "Načítání...")

                is NowRunningState.HasFilters -> Column {
                    Text(
                        "Filtr linek:", modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        state.lineNumbers.forEach { line ->
                            Chip(
                                list = state.filters,
                                lineNumber = line,
                                onClick = {
                                    onEvent(NowRunningEvent.ChangeFilter(line))
                                }
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        when (state) {
                            is NowRunningState.NothingRunningNow -> textItem("Od vybraných linek právě nic nejede")
                            is NowRunningState.Loading -> textItem("Načítání...")
                            is NowRunningState.OK -> when (state.result) {
                                is NowRunningResults.Lines -> state.result.list.forEach { line ->
                                    stickyHeader(key = line.lineNumber to line.destination) {
                                        Column(
                                            Modifier.background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            Text(
                                                text = "${line.lineNumber} -> ${line.destination}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) {
                                                Text(text = "Vůz: příští zastávka", modifier = Modifier.weight(1F), style = MaterialTheme.typography.labelMedium)
                                                Text(text = "odjezd", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.onSurface)
                                            )
                                        }
                                    }
                                    items(line.buses, key = { it.busId }) { bus ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onEvent(NowRunningEvent.NavToBus(bus.busId))
                                                }
                                        ) {
                                            Text(text = "${bus.vehicle.regN()}: ${bus.nextStopName}", modifier = Modifier.weight(1F))
                                            Text(text = bus.nextStopTime.toString())
                                            Text(
                                                text = bus.nextStopTime.plus(bus.delay.toInt().minutes).toString(),
                                                color = UtilFunctions.colorOfDelayText(bus.delay),
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }

                                is NowRunningResults.RegN -> items(state.result.list, key = { it.busId }) { bus ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onEvent(NowRunningEvent.NavToBus(bus.busId))
                                            }
                                    ) {
                                        Text(text = "${bus.lineNumber} -> ${bus.destination}", modifier = Modifier.weight(1F))
                                        Text(text = bus.vehicle.regN())
                                    }
                                }

                                is NowRunningResults.Delay -> items(state.result.list, key = { it.busId }) { bus ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onEvent(NowRunningEvent.NavToBus(bus.busId))
                                            }
                                    ) {
                                        Text(text = "${bus.lineNumber} -> ${bus.destination}", modifier = Modifier.weight(1F))
                                        Text(
                                            text = bus.delay.toDouble().minutes.toDelay(),
                                            color = UtilFunctions.colorOfDelayText(bus.delay)
                                        )
                                    }
                                }
                            }
                        }

                        if (state is NowRunningState.HasNotRunning && state.nowNotRunning.isNotEmpty()) {
                            stickyHeader {
                                Surface {
                                    Text("Jedoucí kurzy, které nejsou online:", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            items(state.nowNotRunning) {
                                Text(it.second, Modifier.clickable {
                                    onEvent(NowRunningEvent.NavToSeq(it.first))
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(
    list: List<Int>,
    lineNumber: Int,
    onClick: (Boolean) -> Unit,
) = FilterChip(
    modifier = Modifier
        .padding(all = 4.dp),
    selected = lineNumber in list,
    onClick = {
        onClick(lineNumber !in list)
    },
    label = { Text("$lineNumber") }
)