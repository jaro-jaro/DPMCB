package cz.jaro.dpmcb.data.realtions.departures

import java.time.LocalTime

data class StopOfDeparture(
    val name: String,
    val time: LocalTime?,
    val stopIndexOnLine: Int,
)