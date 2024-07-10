package cz.jaro.dpmcb.data.realtions.bus

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.realtions.BusInfo
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.RunsFromTo

data class BusDetail(
    val info: BusInfo,
    val stops: List<BusStop>,
    val timeCodes: List<RunsFromTo>,
    val fixedCodes: String,
    val sequence: List<BusName>?,
    val before: List<SequenceCode>?,
    val after: List<SequenceCode>?,
)