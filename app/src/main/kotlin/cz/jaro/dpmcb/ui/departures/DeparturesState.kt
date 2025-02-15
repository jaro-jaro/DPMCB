package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.helperclasses.indexOfFirstOrNull
import cz.jaro.dpmcb.data.helperclasses.plus
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface DeparturesState {

    val info: DeparturesInfo
    val stop: String
    val isOnline: Boolean

    data class Loading(
        override val info: DeparturesInfo,
        override val stop: String,
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
        override val stop: String,
        override val isOnline: Boolean,
        val reason: NothingRunsReason,
    ) : Loaded

    data class Runs(
        override val info: DeparturesInfo,
        override val stop: String,
        override val isOnline: Boolean,
        val departures: List<DepartureState>,
    ) : Loaded
}

fun List<DepartureState>.home(time: LocalTime) = (indexOfFirstOrNull { bus ->
    val departure = bus.time.plus(if (bus.runsIn > Duration.ZERO && bus.delay != null) bus.delay.toDouble().seconds else 0.seconds)
    departure >= time
} ?: lastIndex) + 1