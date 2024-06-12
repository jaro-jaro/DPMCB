package cz.jaro.dpmcb.ui.now_running

import java.time.LocalTime

data class RunningBus(
    val busName: String,
    val nextStopName: String,
    val nextStopTime: LocalTime,
    val delay: Float,
    val vehicle: Int,
    val sequence: String?,
)