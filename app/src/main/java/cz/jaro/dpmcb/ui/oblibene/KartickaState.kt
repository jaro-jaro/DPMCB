package cz.jaro.dpmcb.ui.oblibene

import java.time.LocalDate
import java.time.LocalTime

sealed interface KartickaState {
    val spojId: String
    val linka: Int
    val vychoziZastavka: String
    val vychoziZastavkaCas: LocalTime
    val cilovaZastavka: String
    val cilovaZastavkaCas: LocalTime
    val dalsiPojede: LocalDate?

    data class Offline(
        override val spojId: String,
        override val linka: Int,
        override val vychoziZastavka: String,
        override val vychoziZastavkaCas: LocalTime,
        override val cilovaZastavka: String,
        override val cilovaZastavkaCas: LocalTime,
        override val dalsiPojede: LocalDate?,
    ) : KartickaState

    data class Online(
        override val spojId: String,
        override val linka: Int,
        val zpozdeni: Float,
        override val vychoziZastavka: String,
        override val vychoziZastavkaCas: LocalTime,
        val aktualniZastavka: String,
        val aktualniZastavkaCas: LocalTime,
        override val cilovaZastavka: String,
        override val cilovaZastavkaCas: LocalTime,
        val mistoAktualniZastavky: Int,
    ) : KartickaState {
        override val dalsiPojede: LocalDate? get() = LocalDate.now()
    }
}
