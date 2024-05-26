package cz.jaro.dpmcb.data.realtions.departures

import java.time.LocalDate
import java.time.LocalTime

data class CoreDeparture(
    val tab: String,
    val name: String,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val connNumber: Int,
    val line: Int,
    val lowFloor: Boolean,
    val fixedCodes: String,
    val runs: Boolean,
    val from: LocalDate,
    val to: LocalDate,
)
