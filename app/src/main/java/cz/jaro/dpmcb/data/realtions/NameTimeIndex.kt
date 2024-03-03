package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class NameTimeIndex(
    val name: String,
    val time: LocalTime?,
    val index: Int,
)
