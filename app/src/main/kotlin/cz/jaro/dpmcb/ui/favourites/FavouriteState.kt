package cz.jaro.dpmcb.ui.favourites

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.todayHere
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface FavouriteState {
    val busName: BusName
    val line: ShortLine
    val originStopName: String
    val originStopTime: LocalTime
    val destinationStopName: String
    val destinationStopTime: LocalTime
    val nextWillRun: LocalDate?

    data class Offline(
        override val busName: BusName,
        override val line: ShortLine,
        override val originStopName: String,
        override val originStopTime: LocalTime,
        override val destinationStopName: String,
        override val destinationStopTime: LocalTime,
        override val nextWillRun: LocalDate?,
    ) : FavouriteState

    data class Online(
        override val busName: BusName,
        override val line: ShortLine,
        val delay: Float,
        val vehicle: RegistrationNumber?,
        override val originStopName: String,
        override val originStopTime: LocalTime,
        val currentStopName: String,
        val currentStopTime: LocalTime,
        override val destinationStopName: String,
        override val destinationStopTime: LocalTime,
        val positionOfCurrentStop: Int,
    ) : FavouriteState {
        override val nextWillRun: LocalDate get() = SystemClock.todayHere()
    }

}

@OptIn(ExperimentalContracts::class)
fun FavouriteState.isOnline(): Boolean {
    contract {
        returns(true) implies (this@isOnline is FavouriteState.Online)
    }
    return this is FavouriteState.Online
}
