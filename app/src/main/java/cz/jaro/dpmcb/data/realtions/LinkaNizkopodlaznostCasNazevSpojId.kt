package cz.jaro.dpmcb.data.realtions

import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Datum

data class LinkaNizkopodlaznostCasNazevSpojId(
    val nizkopodlaznost: Boolean,
    val linka: Int,
    val pevneKody: String,
    val cas: Cas,
    val nazev: String,
    val spojId: String,
    val jede: Boolean,
    val od: Datum,
    @Suppress("PropertyName") val do_: Datum,
)