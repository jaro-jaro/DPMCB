package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.entities.SequenceCode
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimeOfSequence(
    val sequence: SequenceCode,
    @SerialName("starttime")
    val start: LocalTime,
    @SerialName("endtime")
    val end: LocalTime,
)
