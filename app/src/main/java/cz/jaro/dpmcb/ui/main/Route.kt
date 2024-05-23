package cz.jaro.dpmcb.ui.main

import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Route")
sealed interface Route {
    @Serializable
    @SerialName("bus")
    data class Bus(
        val busId: String,
    ) : Route

    @Serializable
    @SerialName("card")
    data object Card : Route

    @Serializable
    @SerialName("chooser")
    data class Chooser(
        val type: ChooserType,
        val lineNumber: Int = -1,
        val stop: String? = null,
    ) : Route

    @Serializable
    @SerialName("departures")
    data class Departures(
        val stop: String,
        val time: SimpleTime? = null,
        val line: Int = 0,
        val via: String? = null,
    ) : Route

    @Serializable
    @SerialName("favourites")
    data object Favourites : Route

    @Serializable
    @SerialName("map")
    data object Map : Route

    @Serializable
    @SerialName("now_running")
    data class NowRunning(
        val filters: List<Int> = listOf(),
        val type: NowRunningType = NowRunningType.Line,
    ) : Route

    @Serializable
    @SerialName("sequence")
    data class Sequence(
        val sequence: String,
    ) : Route

    @Serializable
    @SerialName("timetable")
    data class Timetable(
        val lineNumber: Int,
        val stop: String,
        val nextStop: String,
    ) : Route
}