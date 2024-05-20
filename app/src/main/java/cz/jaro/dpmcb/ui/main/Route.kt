package cz.jaro.dpmcb.ui.main

import cz.jaro.dpmcb.data.helperclasses.NullableLocalTimeSerializer
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
sealed interface Route {
    @Serializable
    data class Bus(
        val busId: String,
    ) : Route

    @Serializable
    data object Card : Route

    @Serializable
    data class Chooser(
        val type: ChooserType,
        val lineNumber: Int = -1,
        val stop: String? = null,
    ) : Route

    @Serializable
    data class Departures(
        val stop: String,
        @Serializable(with = NullableLocalTimeSerializer::class)
        val time: LocalTime? = null,
        val line: Int? = null,
        val via: String? = null,
    ) : Route

    @Serializable
    data object Favourites : Route

    @Serializable
    data object Map : Route

    @Serializable
    data class NowRunning(
        val filters: List<Int> = emptyList(),
        val type: NowRunningType = NowRunningType.Line,
    ) : Route

    @Serializable
    data class Sequence(
        val sequence: String,
    ) : Route

    @Serializable
    data class Timetable(
        val lineNumber: Int,
        val stop: String,
        val nextStop: String,
    ) : Route
}