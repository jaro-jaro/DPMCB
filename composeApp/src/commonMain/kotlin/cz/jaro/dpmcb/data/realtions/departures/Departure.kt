package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.realtions.StopType
import kotlinx.datetime.LocalTime

data class Departure(
    val name: String,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val busName: BusName,
    val line: ShortLine,
    val lowFloor: Boolean,
    val busStops: List<StopOfDeparture>,
    val stopType: StopType,
)
