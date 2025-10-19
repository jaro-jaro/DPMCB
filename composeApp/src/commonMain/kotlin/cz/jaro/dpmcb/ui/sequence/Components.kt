package cz.jaro.dpmcb.ui.sequence

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun FABs(state: SequenceState.OK, lazyListState: LazyListState) {
    fun Int.busIndexToListIndex() = 3 + state.before.count() + this * 3

    val now = remember(state.buses, state.date) {
        if (
            state.date != SystemClock.todayHere() || !state.runsToday ||
            SystemClock.timeHere() < state.buses.first().stops.first().time ||
            state.buses.last().stops.last().time < SystemClock.timeHere()
        ) null else
            state.buses.indexOfFirst {
                it.isRunning
            }.takeUnless {
                it == -1
            } ?: state.buses.indexOfFirst {
                SystemClock.timeHere() < it.stops.last().time
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
                contentDescription = "Skočit nahoru",
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
                contentDescription = "Skočit na právě jedoucí",
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
                contentDescription = "Skočit dolů",
            )
        }
    }
}

@Composable
fun BusButton(
    onEvent: (SequenceEvent) -> Unit,
    bus: BusInSequence,
) = TextButton(
    onClick = {
        onEvent(SequenceEvent.BusClick(bus.busName))
    }
) {
    Text("Detail spoje")
}

@Composable
fun Connection(
    onEvent: (SequenceEvent) -> Unit,
    sequence: Pair<SequenceCode, String>,
) = TextButton(
    onClick = {
        onEvent(SequenceEvent.SequenceClick(sequence.first))
    }
) {
    Text(sequence.second)
}

@Composable
fun VehicleSearcher(
    onEvent: (SequenceEvent) -> Unit,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var lost by rememberSaveable { mutableStateOf(false) }

    if (searching) CircularProgressIndicator(Modifier.padding(horizontal = 8.dp))
    else {
        TextButton(
            onClick = {
                lost = false
                searching = true
                onEvent(
                    SequenceEvent.FindBus(
                        onLost = {
                            searching = false
                            lost = true
                        },
                        onFound = {
                            searching = false
                        },
                    )
                )
            },
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        ) {
            Text("Stáhnout vůz")
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                Modifier.size(ButtonDefaults.IconSize)
            )
        }
    }
    if (lost) Text("Nepodařilo se vůz najít :(")
}