package cz.jaro.dpmcb.ui.spoj

import androidx.compose.ui.graphics.vector.ImageVector
import cz.jaro.dpmcb.data.naJihu.ZastavkaSpojeNaJihu
import cz.jaro.dpmcb.data.realtions.CasNazevSpojIdLInkaPristi
import java.time.LocalDate

sealed interface SpojState {
    sealed interface OK : SpojState {

        val spojId: String
        val zastavky: List<CasNazevSpojIdLInkaPristi>
        val cisloLinky: Int
        val nizkopodlaznost: Pair<ImageVector, String>
        val caskody: List<String>
        val pevneKody: List<String>
        val linkaKod: String
        val nazevSpoje: String
        val deeplink: String
        val vyluka: Boolean
        val projetychUseku: Int
        val vyska: Float
        val jeOblibeny: Boolean

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
            override val jeOblibeny: Boolean,
        ) : OK

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
            override val jeOblibeny: Boolean,
            val zpozdeni: Int,
            val zastavkyNaJihu: List<ZastavkaSpojeNaJihu>,
        ) : OK {
            companion object {
                operator fun invoke(
                    state: Offline,
                    zpozdeni: Int,
                    zastavkyNaJihu: List<ZastavkaSpojeNaJihu>,
                ) = with(state) {
                    Online(
                        spojId, zastavky, cisloLinky, nizkopodlaznost, caskody, pevneKody, linkaKod, nazevSpoje,
                        deeplink, vyluka, projetychUseku, vyska, jeOblibeny, zpozdeni, zastavkyNaJihu
                    )
                }
            }
        }
    }

    object Loading : SpojState

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