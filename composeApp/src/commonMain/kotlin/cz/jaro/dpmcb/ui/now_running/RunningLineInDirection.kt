package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.ShortLine

data class RunningLineInDirection(
    val lineNumber: ShortLine,
    val destination: String,
    val buses: List<RunningBus>,
)
