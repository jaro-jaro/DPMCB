package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.indexOfFirstOrNull
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import kotlin.time.Duration
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

fun List<DepartureState>.home(info: DeparturesInfo) = (indexOfFirstOrNull { bus ->
    bus.time.plus(if (bus.runsIn > Duration.ZERO && bus.delay != null) bus.delay.toDouble().seconds else 0.seconds) >= info.time
} ?: lastIndex) + 1