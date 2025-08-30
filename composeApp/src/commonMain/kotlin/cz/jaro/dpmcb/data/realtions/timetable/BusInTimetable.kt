package cz.jaro.dpmcb.data.realtions.timetable

import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.datetime.LocalTime

data class BusInTimetable(
    val departure: LocalTime,
    val busName: BusName,
    val destination: String,
)