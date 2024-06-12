package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import java.time.LocalDate
import java.time.LocalTime

data class CoreDeparture(
    val tab: String,
    val name: String,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val connStopFixedCodes: String,
    val connNumber: Int,
    val line: Int,
    val lowFloor: Boolean,
    val fixedCodes: String,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
)
