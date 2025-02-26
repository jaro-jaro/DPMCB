package cz.jaro.dpmcb.data.realtions.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Direction

data class NowRunning(
    val busName: BusName,
    val lineNumber: ShortLine,
    val direction: Direction,
    val sequence: SequenceCode?,
    val stops: List<StopOfNowRunning>,
)
