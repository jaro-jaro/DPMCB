package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.realtions.BusInfo
import cz.jaro.dpmcb.data.realtions.BusStop

data class BusOfSequence(
    val info: BusInfo,
    val stops: List<BusStop>,
)