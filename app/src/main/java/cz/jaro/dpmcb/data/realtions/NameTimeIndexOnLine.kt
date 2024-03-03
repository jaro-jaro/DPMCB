package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class NameTimeIndexOnLine(
    val name: String,
    val time: LocalTime?,
    val stopIndexOnLine: Int,
)