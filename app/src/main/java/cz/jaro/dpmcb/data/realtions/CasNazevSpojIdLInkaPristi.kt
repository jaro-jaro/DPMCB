package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class CasNazevSpojIdLInkaPristi(
    val cas: LocalTime,
    val nazev: String,
    val linka: Int,
    val pristiZastavka: String?,
    val spojId: String,
)