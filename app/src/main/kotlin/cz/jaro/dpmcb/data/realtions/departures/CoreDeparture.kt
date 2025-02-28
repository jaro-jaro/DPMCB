package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.entities.BusNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCodeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class CoreDeparture(
    val tab: Table,
    val name: String,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val connStopFixedCodes: String,
    val connNumber: BusNumber,
    val line: LongLine,
    val lowFloor: Boolean,
    val fixedCodes: String,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
)
