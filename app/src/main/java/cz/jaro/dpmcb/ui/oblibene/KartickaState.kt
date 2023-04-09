package cz.jaro.dpmcb.ui.oblibene

import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Datum

data class KartickaState(
    val spojId: String,
    val linka: Int,
    val zpozdeni: Int?,
    val vychoziZastavka: String,
    val vychoziZastavkaCas: Cas,
    val aktualniZastavka: String?,
    val aktualniZastavkaCas: Cas?,
    val cilovaZastavka: String,
    val cilovaZastavkaCas: Cas,
    val dalsiPojede: Datum?,
)