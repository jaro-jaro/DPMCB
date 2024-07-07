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
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun FABs(state: SequenceState.OK, lazyListState: LazyListState, date: LocalDate) {
    fun Int.busIndexToListIndex() = 3 + state.before.count() * 2 + this * 4

    val now = remember(state.buses) {
        if (date != LocalDate.now()) null
        else state.buses.indexOfFirst {
            it.isRunning
        }.takeUnless {
            it == -1
        } ?: state.buses.indexOfFirst {
            LocalTime.now() < it.stops.last().time
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
fun BusButton(
    navigate: NavigateFunction,
    bus: BusInSequence,
) = TextButton(
    onClick = {
        navigate(Route.Bus(busName = bus.busName))
    }
) {
    Text("Detail spoje")
}

@Composable
fun Connection(
    navigate: NavigateFunction,
    sequence: Pair<String, String>,
) = TextButton(
    onClick = {
        navigate(Route.Sequence(sequence.first))
    }
) {
    Text(sequence.second)
}