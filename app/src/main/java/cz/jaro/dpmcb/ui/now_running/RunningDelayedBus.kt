package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine

data class RunningDelayedBus(
    val busName: BusName,
    val sequence: SequenceCode?,
    val delay: Float,
    val lineNumber: ShortLine,
    val destination: String,
)