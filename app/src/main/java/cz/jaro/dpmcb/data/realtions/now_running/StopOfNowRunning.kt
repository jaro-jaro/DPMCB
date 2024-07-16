package cz.jaro.dpmcb.data.realtions.now_running

import kotlinx.datetime.LocalTime

data class StopOfNowRunning(
    val name: String,
    val time: LocalTime,
)
