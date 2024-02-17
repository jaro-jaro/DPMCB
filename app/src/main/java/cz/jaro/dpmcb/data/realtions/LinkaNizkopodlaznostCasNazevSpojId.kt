package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate
import java.time.LocalTime

data class LinkaNizkopodlaznostCasNazevSpojId(
    val nizkopodlaznost: Boolean,
    val linka: Int,
    val kurz: String?,
    val pevneKody: String,
    val cas: LocalTime,
    val nazev: String,
    val spojId: String,
    val jede: Boolean,
    val od: LocalDate,
    val `do`: LocalDate,
)