package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class BusStop(
    val time: LocalTime,
    val name: String,
    val line: Int,
    val nextStop: String?,
    val connName: String,
    val type: StopType,
)