package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class CasNazevSpojId(
    val cas: LocalTime,
    val nazev: String,
    val spojId: String,
)