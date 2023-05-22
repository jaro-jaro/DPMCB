package cz.jaro.dpmcb.ui.odjezdy

import java.time.Duration
import java.time.LocalTime

data class KartickaState(
    val konecna: String,
    val aktualniNasledujiciZastavka: Pair<String, LocalTime>?,
    val pristiZastavka: String?,
    val cisloLinky: Int,
    val cas: LocalTime,
    val idSpoje: String,
    val nizkopodlaznost: Boolean,
    val zpozdeni: Int?,
    val pojedePres: List<String>,
    val jedeZa: Duration?,
)