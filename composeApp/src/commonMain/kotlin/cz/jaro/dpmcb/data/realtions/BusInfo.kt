package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine

data class BusInfo(
    val lowFloor: Boolean,
    val line: ShortLine,
    val connName: BusName,
    val sequence: SequenceCode?,
)