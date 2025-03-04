package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine

sealed interface NowRunningEvent {
    data class ChangeType(val type: NowRunningType) : NowRunningEvent
    data class ChangeFilter(val lineNumber: ShortLine) : NowRunningEvent
    data class NavToBus(val busName: BusName) : NowRunningEvent
    data class NavToSeq(val seq: SequenceCode) : NowRunningEvent
}