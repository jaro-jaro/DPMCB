package cz.jaro.dpmcb.ui.spoj

import androidx.compose.ui.graphics.vector.ImageVector
import cz.jaro.dpmcb.data.helperclasses.CastSpoje
import cz.jaro.dpmcb.data.jikord.ZastavkaOnlineSpoje
import cz.jaro.dpmcb.data.realtions.CasNazevSpojIdLInkaPristi
import java.time.LocalDate
import java.time.LocalTime

sealed interface SpojState {
    sealed interface OK : SpojState {

        val spojId: String
        val zastavky: List<CasNazevSpojIdLInkaPristi>
        val cisloLinky: Int
        val nizkopodlaznost: Pair<ImageVector, String>
        val nizkopodlaznostPotvrzena: Boolean
        val caskody: List<String>
        val pevneKody: List<String>
        val linkaKod: String
        val nazevSpoje: String
        val deeplink: String
        val vyluka: Boolean
        val projetychUseku: Int
        val vyska: Float
        val oblibeny: CastSpoje?
        val chyba: Boolean

        data class Offline(
            override val spojId: String,
            override val zastavky: List<CasNazevSpojIdLInkaPristi>,
            override val cisloLinky: Int,
            override val nizkopodlaznost: Pair<ImageVector, String>,
            override val caskody: List<String>,
            override val pevneKody: List<String>,
            override val linkaKod: String,
            override val nazevSpoje: String,
            override val deeplink: String,
            override val vyluka: Boolean,
            override val projetychUseku: Int,
            override val vyska: Float,
            override val oblibeny: CastSpoje?,
            override val chyba: Boolean,
        ) : OK {
            override val nizkopodlaznostPotvrzena = false
        }

        data class Online(
            override val spojId: String,
            override val zastavky: List<CasNazevSpojIdLInkaPristi>,
            override val cisloLinky: Int,
            override val nizkopodlaznost: Pair<ImageVector, String>,
            override val caskody: List<String>,
            override val pevneKody: List<String>,
            override val linkaKod: String,
            override val nazevSpoje: String,
            override val deeplink: String,
            override val vyluka: Boolean,
            override val projetychUseku: Int,
            override val vyska: Float,
            override val oblibeny: CastSpoje?,
            override val chyba: Boolean,
            val zastavkyNaJihu: List<ZastavkaOnlineSpoje>,
            val zpozdeniMin: Float,
            val vuz: Int?,
            override val nizkopodlaznostPotvrzena: Boolean,
            val pristiZastavka: LocalTime,
        ) : OK {
            companion object {
                operator fun invoke(
                    state: Offline,
                    zastavkyNaJihu: List<ZastavkaOnlineSpoje>,
                    zpozdeniMin: Float,
                    vuz: Int?,
                    potvrzenaNizkopodlaznost: Pair<ImageVector, String>?,
                    pristiZastavka: LocalTime,
                ) = with(state) {
                    Online(
                        spojId, zastavky, cisloLinky, potvrzenaNizkopodlaznost ?: nizkopodlaznost, caskody, pevneKody, linkaKod, nazevSpoje,
                        deeplink, vyluka, projetychUseku, vyska, oblibeny, chyba, zastavkyNaJihu, zpozdeniMin, vuz, potvrzenaNizkopodlaznost != null, pristiZastavka
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
        val spojId: String,
        val datum: LocalDate,
        val pristeJedePoDnesku: LocalDate?,
        val pristeJedePoDatu: LocalDate?,
    ) : SpojState
}