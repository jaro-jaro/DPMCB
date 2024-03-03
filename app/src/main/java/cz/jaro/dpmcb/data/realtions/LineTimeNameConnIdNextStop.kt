package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class LineTimeNameConnIdNextStop(
    val time: LocalTime,
    val name: String,
    val line: Int,
    val nextStop: String?,
    val connId: String,
)