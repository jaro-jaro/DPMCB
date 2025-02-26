package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.realtions.RunsFromTo

data class Sequence(
    val name: SequenceCode,
    val before: List<SequenceCode>,
    val buses: List<BusOfSequence>,
    val after: List<SequenceCode>,
    val commonTimeCodes: List<RunsFromTo>,
    val commonFixedCodes: String,
    val commonValidity: cz.jaro.dpmcb.data.database.entities.Validity?,
)
