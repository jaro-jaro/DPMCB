package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.helperclasses.Cas

data class LinkaNizkopodlaznostCasNazevSpojId(
    val nizkopodlaznost: Boolean,
    val linka: Int,
    val pevneKody: String,
    val odjezd: Cas?,
    val prijezd: Cas?,
    val nazev: String,
    val spojId: String,
)