package cz.jaro.dpmcb.data.realtions.sequence

import java.time.LocalTime

data class TimeOfSequence(
    val sequence: String,
    val start: LocalTime,
    val end: LocalTime,
)
