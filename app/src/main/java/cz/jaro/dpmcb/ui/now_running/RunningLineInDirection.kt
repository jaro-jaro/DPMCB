package cz.jaro.dpmcb.ui.now_running

data class RunningLineInDirection(
    val lineNumber: Int,
    val destination: String,
    val buses: List<RunningBus>,
)
