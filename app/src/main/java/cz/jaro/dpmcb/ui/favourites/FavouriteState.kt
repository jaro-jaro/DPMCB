package cz.jaro.dpmcb.ui.favourites

import java.time.LocalDate
import java.time.LocalTime

sealed interface FavouriteState {
    val busName: String
    val line: Int
    val originStopName: String
    val originStopTime: LocalTime
    val destinationStopName: String
    val destinationStopTime: LocalTime
    val nextWillRun: LocalDate?

    data class Offline(
        override val busName: String,
        override val line: Int,
        override val originStopName: String,
        override val originStopTime: LocalTime,
        override val destinationStopName: String,
        override val destinationStopTime: LocalTime,
        override val nextWillRun: LocalDate?,
    ) : FavouriteState

    data class Online(
        override val busName: String,
        override val line: Int,
        val delay: Float,
        val vehicle: Int?,
        override val originStopName: String,
        override val originStopTime: LocalTime,
        val currentStopName: String,
        val currentStopTime: LocalTime,
        override val destinationStopName: String,
        override val destinationStopTime: LocalTime,
        val positionOfCurrentStop: Int,
    ) : FavouriteState {
        override val nextWillRun: LocalDate? get() = LocalDate.now()
    }
}
