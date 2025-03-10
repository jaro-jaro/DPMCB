package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.entities.Validity
import cz.jaro.dpmcb.data.realtions.BusInfo
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.RunsFromTo

data class InterBusOfSequence(
    val info: BusInfo,
    val stops: List<BusStop>,
    val timeCodes: List<RunsFromTo>,
    val fixedCodes: String,
    val validity: Validity,
)