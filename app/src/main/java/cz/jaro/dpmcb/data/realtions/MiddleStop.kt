package cz.jaro.dpmcb.data.realtions

import kotlinx.datetime.LocalTime

data class MiddleStop(
    val name: String,
    val time: LocalTime?,
    val index: Int,
)
