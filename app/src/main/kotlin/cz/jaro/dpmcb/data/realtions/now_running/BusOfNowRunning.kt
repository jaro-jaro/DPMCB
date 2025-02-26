package cz.jaro.dpmcb.data.realtions.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.Direction

data class BusOfNowRunning(
    val connName: BusName,
    val line: LongLine,
    val direction: Direction,
    val sequence: SequenceCode?,
    val tab: Table,
)
