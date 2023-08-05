package cz.jaro.dpmcb.ui.nastaveni

import cz.jaro.dpmcb.data.Nastaveni

data class NastaveniState(
    val nastaveni: Nastaveni,
    val verze: String,
    val verzeDat: Int,
    val metaVerzeDat: Int,
)
