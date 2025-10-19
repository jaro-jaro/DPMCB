package cz.jaro.dpmcb.data.realtions.now_running

import kotlinx.datetime.LocalTime

data class BusStartEnd(
    val start: LocalTime,
    val end: LocalTime,
)
