package cz.jaro.dpmcb.ui.now_running

data class RunningDelayedBus(
    val busName: String,
    val sequence: String?,
    val delay: Float,
    val lineNumber: Int,
    val destination: String,
)