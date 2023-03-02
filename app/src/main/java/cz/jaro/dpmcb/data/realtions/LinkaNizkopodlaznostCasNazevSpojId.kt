package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Datum

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