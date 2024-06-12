package cz.jaro.dpmcb.data.realtions.timetable

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import java.time.LocalDate
import java.time.LocalTime

data class CoreBusInTimetable(
    val departure: LocalTime,
    val lowFloor: Boolean,
    val connName: String,
    val destination: String,
    val fixedCodes: String,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
)