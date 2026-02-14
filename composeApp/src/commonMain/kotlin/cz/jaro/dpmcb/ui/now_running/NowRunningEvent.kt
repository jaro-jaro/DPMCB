package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode

sealed interface NowRunningEvent {
    data class ChangeType(val type: NowRunningType) : NowRunningEvent
    data class ChangeFilter(val lineNumber: LongLine) : NowRunningEvent
    data class NavToBus(val busName: BusName) : NowRunningEvent
    data class NavToSeq(val seq: SequenceCode) : NowRunningEvent
}