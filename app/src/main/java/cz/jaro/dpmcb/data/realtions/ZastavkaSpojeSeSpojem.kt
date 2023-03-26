package cz.jaro.dpmcb.data.realtions

import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Datum

data class ZastavkaSpojeSeSpojem(
    val nazev: String,
    val cas: Cas,
    val indexZastavkyNaLince: Int,
    val cisloSpoje: Int,
    val linka: Int,
    val nizkopodlaznost: Boolean,
    val pevneKody: String,
    val jede: Boolean,
    val od: Datum,
    val `do`: Datum,
)
