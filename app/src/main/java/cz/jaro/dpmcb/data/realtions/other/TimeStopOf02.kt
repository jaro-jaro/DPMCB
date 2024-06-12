package cz.jaro.dpmcb.data.realtions.other

import java.time.LocalTime

data class TimeStopOf02(
    val lowFloor: Boolean,
    val time: LocalTime,
    val connName: String,
    val tab: String,
)