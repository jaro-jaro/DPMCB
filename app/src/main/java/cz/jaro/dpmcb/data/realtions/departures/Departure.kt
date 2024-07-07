package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.realtions.StopType
import java.time.LocalTime

data class Departure(
    val name: String,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val busName: String,
    val line: Int,
    val lowFloor: Boolean,
    val busStops: List<StopOfDeparture>,
    val stopType: StopType,
)
