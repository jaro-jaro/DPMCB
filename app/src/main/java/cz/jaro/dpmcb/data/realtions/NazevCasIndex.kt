package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class NazevCasIndex(
    val nazev: String,
    val cas: LocalTime?,
    val index: Int,
)
