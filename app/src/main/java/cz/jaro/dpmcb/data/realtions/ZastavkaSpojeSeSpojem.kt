package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate
import java.time.LocalTime

data class ZastavkaSpojeSeSpojem(
    val nazev: String,
    val cas: LocalTime,
    val indexZastavkyNaLince: Int,
    val cisloSpoje: Int,
    val linka: Int,
    val nizkopodlaznost: Boolean,
    val pevneKody: String,
    val jede: Boolean,
    val od: LocalDate,
    val `do`: LocalDate,
)
