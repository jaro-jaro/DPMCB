package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

sealed interface DeparturesState {
    data object Loading : DeparturesState

    sealed interface NothingRuns : DeparturesState

    data object NothingRunsAtAll : NothingRuns
    data object NothingRunsHere : NothingRuns
    data object LineDoesNotRun : NothingRuns
    data object LineDoesNotRunHere : NothingRuns

    data class Runs(
        val line: List<DepartureState>,
    ) : DeparturesState
}

fun List<DepartureState>.home(info: DeparturesInfo) = (withIndex().firstOrNull { (_, bus) ->
    bus.time + (if (bus.runsIn > Duration.ZERO && bus.delay != null) bus.delay.toDouble().minutes else 0.seconds) >= info.time
}?.index ?: lastIndex) + 1