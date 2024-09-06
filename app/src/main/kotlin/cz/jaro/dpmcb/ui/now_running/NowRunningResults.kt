package cz.jaro.dpmcb.ui.now_running

sealed interface NowRunningResults <T>{

    val list: List<T>

    data class Lines(
        override val list: List<RunningLineInDirection>
    ) : NowRunningResults<RunningLineInDirection>
    data class Delay(
        override val list: List<RunningDelayedBus>
    ) : NowRunningResults<RunningDelayedBus>
    data class RegN(
        override val list: List<RunningVehicle>
    ) : NowRunningResults<RunningVehicle>
}
fun List<RunningLineInDirection>.toResult() = NowRunningResults.Lines(this)
fun List<RunningDelayedBus>.toResult() = NowRunningResults.Delay(this)
fun List<RunningVehicle>.toResult() = NowRunningResults.RegN(this)