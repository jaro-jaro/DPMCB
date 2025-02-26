package cz.jaro.dpmcb.data.realtions.timetable

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.TimeCodeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class CoreBusInTimetable(
    val departure: LocalTime,
    val lowFloor: Boolean,
    val connName: BusName,
    val destination: String,
    val fixedCodes: String,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
)