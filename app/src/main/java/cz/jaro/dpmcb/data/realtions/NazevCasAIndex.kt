package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class NazevCasAIndex(
    val nazev: String,
    val cas: LocalTime?,
    val indexZastavkyNaLince: Int,
)
