package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.LongLine

data class RunningLineInDirection(
    val lineNumber: LongLine,
    val destination: String,
    val buses: List<RunningBus>,
)
