package cz.jaro.dpmcb.data.realtions.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.types.Direction

data class NowRunning(
    val busName: BusName,
    val lineNumber: LongLine,
    val direction: Direction,
    val sequence: SequenceCode?,
    val stops: List<StopOfNowRunning>,
)
