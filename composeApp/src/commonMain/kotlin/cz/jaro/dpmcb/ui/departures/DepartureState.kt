package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.FareZone
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.realtions.StopType
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

data class DepartureState(
    val destination: StopName,
    val destinationZone: FareZone?,
    val currentNextStop: Triple<StopName, LocalDateTime, FareZone?>?,
    val platform: Platform?,
    val direction: Direction,
    val isLastStop: Boolean,
    val stopType: StopType,
    val lineNumber: LongLine,
    val time: LocalDateTime,
    val busName: BusName,
    val lineTraction: Traction,
    val vehicleTraction: Traction?,
    val delay: Duration?,
    val runsVia: List<StopName>,
)