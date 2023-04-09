package cz.jaro.dpmcb.ui.odjezdy

import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Trvani

data class KartickaState(
    val konecna: String,
    val aktualniNasledujiciZastavka: Pair<String, Cas>?,
    val cisloLinky: Int,
    val cas: Cas,
    val idSpoje: String,
    val nizkopodlaznost: Boolean,
    val zpozdeni: Int?,
    val jedePres: List<String>,
    val jedeZa: Trvani?,
)