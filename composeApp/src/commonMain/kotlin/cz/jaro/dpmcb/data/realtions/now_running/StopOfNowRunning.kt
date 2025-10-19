package cz.jaro.dpmcb.data.realtions.now_running

import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StopOfNowRunning(
    val name: String,
    @SerialName("time_")
    val time: LocalTime,
)
