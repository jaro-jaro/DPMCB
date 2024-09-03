package cz.jaro.dpmcb.ui.sequence

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import kotlinx.coroutines.launch

@Composable
fun FABs(state: SequenceState.OK, lazyListState: LazyListState) {
    fun Int.busIndexToListIndex() = 3 + state.before.count() * 2 + this * 4

    val now = remember(state.buses, state.date) {
        if (state.date != SystemClock.todayHere()) null
        else state.buses.indexOfFirst {
            it.isRunning
        }.takeUnless {
            it == -1
        } ?: state.buses.indexOfFirst {
            SystemClock.timeHere() < it.stops.last().time
        }.takeIf {
            state.runsToday && state.buses.first().stops.first().time < SystemClock.timeHere() && SystemClock.timeHere() < state.buses.last().stops.last().time
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