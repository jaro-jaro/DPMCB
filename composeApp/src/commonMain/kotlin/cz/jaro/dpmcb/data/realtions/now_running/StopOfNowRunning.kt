package cz.jaro.dpmcb.data.realtions.now_running

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class StopOfNowRunning(
    val name: String,
    val time: LocalTime,
)
