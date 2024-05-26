package cz.jaro.dpmcb.data.realtions.now_running

import java.time.LocalTime

data class StopOfNowRunning(
    val name: String,
    val time: LocalTime?,
)
