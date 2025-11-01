package cz.jaro.dpmcb.data.realtions.connection

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class StopNameTime(
    val name: String,
    val departure: LocalTime?,
    val arrival: LocalTime?,
    val time: LocalTime,
    val fixedCodes: String,
)
