package cz.jaro.dpmcb.data.realtions.timetable

import java.time.LocalTime

data class BusInTimetable(
    val departure: LocalTime,
    val lowFloor: Boolean,
    val busName: String,
    val destination: String,
    val delay: Float? = null,
)