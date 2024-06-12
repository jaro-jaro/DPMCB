package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.realtions.RunsFromTo

data class Sequence(
    val name: String,
    val before: List<String>,
    val buses: List<BusOfSequence>,
    val after: List<String>,
    val commonTimeCodes: List<RunsFromTo>,
    val commonFixedCodes: String,
)
