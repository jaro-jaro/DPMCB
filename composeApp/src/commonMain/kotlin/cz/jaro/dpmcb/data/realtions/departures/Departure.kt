package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.realtions.StopType
import kotlinx.datetime.LocalTime

data class Departure(
    val name: String,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val busName: BusName,
    val line: LongLine,
    val direction: Direction,
    val lowFloor: Boolean,
    val busStops: List<StopOfDeparture>,
    val stopType: StopType,
)
