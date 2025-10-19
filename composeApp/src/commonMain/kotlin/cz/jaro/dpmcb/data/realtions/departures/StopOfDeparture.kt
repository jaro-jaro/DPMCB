package cz.jaro.dpmcb.data.realtions.departures

import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StopOfDeparture(
    val name: String,
    @SerialName("time_")
    val time: LocalTime,
    @SerialName("stopindexonline")
    val stopIndexOnLine: Int,
)