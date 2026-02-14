package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.FareZone
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.VehicleType
import cz.jaro.dpmcb.data.realtions.StopType
import kotlinx.datetime.LocalTime

data class Departure(
    val name: StopName,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val busName: BusName,
    val line: LongLine,
    val direction: Direction,
    val vehicleType: VehicleType,
    val busStops: List<StopOfDeparture>,
    val stopType: StopType,
    val sequence: SequenceCode,
    val platform: Platform?,
    val fareZone: FareZone?,
)
