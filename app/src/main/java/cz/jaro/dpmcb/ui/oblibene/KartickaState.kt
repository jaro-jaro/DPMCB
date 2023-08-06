package cz.jaro.dpmcb.ui.oblibene

import java.time.LocalDate
import java.time.LocalTime

data class KartickaState(
    val spojId: String,
    val linka: Int,
    val zpozdeni: Int?,
    val vychoziZastavka: String,
    val vychoziZastavkaCas: LocalTime,
    val aktualniZastavka: String?,
    val aktualniZastavkaCas: LocalTime?,
    val cilovaZastavka: String,
    val cilovaZastavkaCas: LocalTime,
    val mistoAktualniZastavky: Int,
    val dalsiPojede: LocalDate?,
)