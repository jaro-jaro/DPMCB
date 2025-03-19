package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.entities.SequenceCode
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class TimeOfSequence(
    val sequence: SequenceCode,
    val start: LocalTime,
    val end: LocalTime,
)
