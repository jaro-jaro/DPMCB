package cz.jaro.dpmcb.ui.main

import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Route")
sealed interface Route {
    @Serializable
    @SerialName("Bus")
    data class Bus(
        val busId: String,
    ) : Route

    @Serializable
    @SerialName("Card")
    data object Card : Route

    @Serializable
    @SerialName("Chooser")
    data class Chooser(
        val type: ChooserType,
        val lineNumber: Int = -1,
        val stop: String? = null,
    ) : Route

    @Serializable
    @SerialName("Departures")
    data class Departures(
        val stop: String,
        val time: SimpleTime? = null,
        val line: Int = 0,
        val via: String? = null,
    ) : Route

    @Serializable
    @SerialName("Favourites")
    data object Favourites : Route

    @Serializable
    @SerialName("Map")
    data object Map : Route

    @Serializable
    @SerialName("NowRunning")
    data class NowRunning(
        val filters: List<Int> = emptyList(),
        val type: NowRunningType = NowRunningType.Line,
    ) : Route

    @Serializable
    @SerialName("Sequence")
    data class Sequence(
        val sequence: String,
    ) : Route

    @Serializable
    @SerialName("Timetable")
    data class Timetable(
        val lineNumber: Int,
        val stop: String,
        val nextStop: String,
    ) : Route
}