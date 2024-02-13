package cz.jaro.dpmcb.ui.spoj

import cz.jaro.dpmcb.data.helperclasses.CastSpoje
import cz.jaro.dpmcb.data.jikord.ZastavkaOnlineSpoje
import cz.jaro.dpmcb.data.realtions.CasNazevSpojIdLinkaPristi
import java.time.LocalDate
import java.time.LocalTime

sealed interface SpojState {

    sealed interface Existuje : SpojState {
        val spojId: String
        val caskody: List<String>
        val pevneKody: List<String>
        val linkaKod: String
        val nazevSpoje: String
        val deeplink: String
    }

    sealed interface OK : Existuje {

        val zastavky: List<CasNazevSpojIdLinkaPristi>
        val cisloLinky: Int
        val nizkopodlaznost: Boolean
        val kurz: String?
        val vyluka: Boolean
        val projetychUseku: Int
        val vyska: Float
        val oblibeny: CastSpoje?
        val chyba: Boolean

        data class Offline(
            override val spojId: String,
            override val zastavky: List<CasNazevSpojIdLinkaPristi>,
            override val cisloLinky: Int,
            override val nizkopodlaznost: Boolean,
            override val caskody: List<String>,
            override val pevneKody: List<String>,
            override val linkaKod: String,
            override val nazevSpoje: String,
            override val kurz: String?,
            override val deeplink: String,
            override val vyluka: Boolean,
            override val projetychUseku: Int,
            override val vyska: Float,
            override val oblibeny: CastSpoje?,
            override val chyba: Boolean,
        ) : OK

        data class Online(
            override val spojId: String,
            override val zastavky: List<CasNazevSpojIdLinkaPristi>,
            override val cisloLinky: Int,
            override val nizkopodlaznost: Boolean,
            override val caskody: List<String>,
            override val pevneKody: List<String>,
            override val linkaKod: String,
            override val nazevSpoje: String,
            override val kurz: String?,
            override val deeplink: String,
            override val vyluka: Boolean,
            override val projetychUseku: Int,
            override val vyska: Float,
            override val oblibeny: CastSpoje?,
            override val chyba: Boolean,
            val zastavkyNaJihu: List<ZastavkaOnlineSpoje>,
            val zpozdeniMin: Float,
            val vuz: Int?,
            val potvrzenaNizkopodlaznost: Boolean?,
            val pristiZastavka: LocalTime,
        ) : OK {
            companion object {
                operator fun invoke(
                    state: Offline,
                    zastavkyNaJihu: List<ZastavkaOnlineSpoje>,
                    zpozdeniMin: Float,
                    vuz: Int?,
                    potvrzenaNizkopodlaznost: Boolean?,
                    pristiZastavka: LocalTime,
                ) = with(state) {
                    Online(
                        spojId, zastavky, cisloLinky, nizkopodlaznost, caskody, pevneKody, linkaKod, nazevSpoje, kurz,
                        deeplink, vyluka, projetychUseku, vyska, oblibeny, chyba, zastavkyNaJihu, zpozdeniMin, vuz, potvrzenaNizkopodlaznost, pristiZastavka
                    )
                }
            }
        }
    }

    data object Loading : SpojState

    data class Neexistuje(
        val spojId: String,
    ) : SpojState

    data class Nejede(
        override val spojId: String,
        val datum: LocalDate,
        override val caskody: List<String>,
        override val pevneKody: List<String>,
        override val linkaKod: String,
        override val nazevSpoje: String,
        override val deeplink: String,
        val pristeJedePoDnesku: LocalDate?,
        val pristeJedePoDatu: LocalDate?,
    ) : Existuje
}