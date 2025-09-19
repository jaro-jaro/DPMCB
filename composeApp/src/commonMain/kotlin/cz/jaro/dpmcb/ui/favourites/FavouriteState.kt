package cz.jaro.dpmcb.ui.favourites

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.Traction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

data class FavouriteState(
    val busName: BusName,
    val line: ShortLine,
    val lineTraction: Traction,
    val originStopName: String,
    val originStopTime: LocalTime,
    val destinationStopName: String,
    val destinationStopTime: LocalTime,
    val nextWillRun: LocalDate?,
    val type: FavouriteType,
    val online: OnlineFavouriteState?,
)

data class OnlineFavouriteState(
    val delay: Duration,
    val vehicleNumber: RegistrationNumber?,
    val vehicleName: String?,
    val vehicleTraction: Traction?,
    val currentStopName: String,
    val currentStopTime: LocalTime,
    val positionOfCurrentStop: Int,
)

enum class FavouriteType {
    Favourite, Recent;
}