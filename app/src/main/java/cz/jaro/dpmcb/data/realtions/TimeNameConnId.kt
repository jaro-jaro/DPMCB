package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class TimeNameConnId(
    val time: LocalTime,
    val name: String,
    val connName: String,
)