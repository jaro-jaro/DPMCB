package cz.jaro.dpmcb.ui.now_running

sealed interface NowRunningEvent {
    data class ChangeType(val typ: NowRunningType) : NowRunningEvent
    data class ChangeFilter(val lineNumber: Int) : NowRunningEvent
    data class NavToBus(val busName: String) : NowRunningEvent
    data class NavToSeq(val seq: String) : NowRunningEvent
}