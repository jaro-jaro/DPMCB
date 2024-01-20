package cz.jaro.dpmcb.ui.kurz

import cz.jaro.dpmcb.data.realtions.CasNazevSpojId

sealed interface SpojKurzuState {

    val spojId: String
    val zastavky: List<CasNazevSpojId>
    val cisloLinky: Int
    val nizkopodlaznost: Boolean

    data class Offline(
        override val spojId: String,
        override val zastavky: List<CasNazevSpojId>,
        override val cisloLinky: Int,
        override val nizkopodlaznost: Boolean,
    ) : SpojKurzuState

    data class Online(
        override val spojId: String,
        override val zastavky: List<CasNazevSpojId>,
        override val cisloLinky: Int,
        override val nizkopodlaznost: Boolean,
        val zpozdeniMin: Float,
        val vuz: Int?,
        val potvrzenaNizkopodlaznost: Boolean?,
    ) : SpojKurzuState {
        companion object {
            operator fun invoke(
                state: Offline,
                zpozdeniMin: Float,
                vuz: Int?,
                potvrzenaNizkopodlaznost: Boolean?,
            ) = with(state) {
                Online(
                    spojId, zastavky, cisloLinky, nizkopodlaznost, zpozdeniMin, vuz, potvrzenaNizkopodlaznost
                )
            }
        }
    }
}