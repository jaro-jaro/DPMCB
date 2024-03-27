package cz.jaro.dpmcb.ui.now_running

data class RunningVehicle(
    val busId: String,
    val vehicle: Int,
    val sequence: String?,
    val lineNumber: Int,
    val destination: String,
)