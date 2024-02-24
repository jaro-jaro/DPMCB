package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class NazevCasIndexNaLince(
    val nazev: String,
    val cas: LocalTime?,
    val indexZastavkyNaLince: Int,
)
