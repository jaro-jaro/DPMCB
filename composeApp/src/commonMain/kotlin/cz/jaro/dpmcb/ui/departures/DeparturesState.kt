package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.helperclasses.indexOfFirstOrNull
import cz.jaro.dpmcb.data.helperclasses.plus
import kotlinx.datetime.LocalTime

sealed interface DeparturesState {

    val info: DeparturesInfo
    val isOnline: Boolean

    data class Loading(
        override val info: DeparturesInfo,
        override val isOnline: Boolean,
    ) : DeparturesState

    sealed interface Loaded : DeparturesState

    enum class NothingRunsReason {
        NothingRunsAtAll,
        NothingRunsHere,
        LineDoesNotRun,
        LineDoesNotRunHere,
    }

    data class NothingRuns(
        override val info: DeparturesInfo,
        override val isOnline: Boolean,
        val reason: NothingRunsReason,
    ) : Loaded

    data class Runs(
        override val info: DeparturesInfo,
        override val isOnline: Boolean,
        val departures: List<DepartureState>,
    ) : Loaded
}

fun List<DepartureState>.indexOfNext(time: LocalTime, now: LocalTime) = (indexOfFirstOrNull { bus ->
    val departure = if (bus.delay != null && (bus.time + bus.delay) > now) bus.time + bus.delay else bus.time
    departure >= time
} ?: lastIndex) + 1 // <-- an item with the previous day button