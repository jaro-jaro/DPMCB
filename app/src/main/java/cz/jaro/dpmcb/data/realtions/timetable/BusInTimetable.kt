package cz.jaro.dpmcb.data.realtions.timetable

import java.time.LocalTime

data class BusInTimetable(
    val departure: LocalTime,
    val lowFloor: Boolean,
    val connName: String,
    val destination: String,
    val fixedCodes: String,
    val delay: Float? = null,
)