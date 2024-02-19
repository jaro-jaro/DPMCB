package cz.jaro.dpmcb.ui.kurz

sealed interface KurzState {

    data object Loading : KurzState

    data class Neexistuje(
        val kurz: String,
    ) : KurzState

    sealed interface OK : KurzState {
        val kurz: String
        val navaznostiPredtim: List<String>
        val navaznostiPotom: List<String>
        val spoje: List<SpojKurzuState>
        val caskody: List<String>
        val pevneKody: List<String>
        val jedeDnes: Boolean

        data class Offline(
            override val kurz: String,
            override val navaznostiPredtim: List<String>,
            override val navaznostiPotom: List<String>,
            override val spoje: List<SpojKurzuState>,
            override val caskody: List<String>,
            override val pevneKody: List<String>,
            override val jedeDnes: Boolean,
        ) : OK

        data class Online(
            override val kurz: String,
            override val navaznostiPredtim: List<String>,
            override val navaznostiPotom: List<String>,
            override val spoje: List<SpojKurzuState>,
            override val caskody: List<String>,
            override val pevneKody: List<String>,
            override val jedeDnes: Boolean,
            val zpozdeniMin: Float,
            val vuz: Int?,
            val potvrzenaNizkopodlaznost: Boolean?,
        ) : OK {
            companion object {
                operator fun invoke(
                    state: OK,
                    zpozdeniMin: Float,
                    vuz: Int?,
                    potvrzenaNizkopodlaznost: Boolean?,
                ) = with(state) {
                    Online(
                        kurz, navaznostiPredtim, navaznostiPotom, spoje, caskody, pevneKody, jedeDnes, zpozdeniMin, vuz, potvrzenaNizkopodlaznost
                    )
                }
            }
        }
    }
}