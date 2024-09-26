package cz.jaro.dpmcb.data.realtions.departures

import kotlinx.datetime.LocalTime

data class StopOfDeparture(
    val name: String,
    val time: LocalTime?,
    val stopIndexOnLine: Int,
)