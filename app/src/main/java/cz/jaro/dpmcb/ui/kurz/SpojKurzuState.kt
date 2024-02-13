package cz.jaro.dpmcb.ui.kurz

import cz.jaro.dpmcb.data.realtions.CasNazevSpojIdLinkaPristi

data class SpojKurzuState(
    val spojId: String,
    val zastavky: List<CasNazevSpojIdLinkaPristi>,
    val cisloLinky: Int,
    val nizkopodlaznost: Boolean,
    val jede: Boolean,
)