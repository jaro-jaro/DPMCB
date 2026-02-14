package cz.jaro.dpmcb.data.realtions.connection

import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class StopNameTime(
    val name: StopName,
    val departure: LocalTime?,
    val arrival: LocalTime?,
    val time: LocalTime,
    val fixedCodes: String,
    val platform: Platform?,
)
