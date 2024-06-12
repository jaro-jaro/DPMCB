package cz.jaro.dpmcb.data.realtions.now_running

import cz.jaro.dpmcb.data.entities.types.Direction

data class NowRunning(
    val busName: String,
    val lineNumber: Int,
    val direction: Direction,
    val sequence: String?,
    val stops: List<StopOfNowRunning>,
)
