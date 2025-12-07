package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.generated.Validity
import cz.jaro.dpmcb.data.realtions.BusInfo
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.RunsFromTo

data class BusOfSequence(
    val info: BusInfo,
    val stops: List<BusStop>,
    val uniqueTimeCodes: List<RunsFromTo>,
    val uniqueFixedCodes: String,
    val uniqueValidity: Validity?,
)