package cz.jaro.dpmcb.ui.odjezdy

import cz.jaro.datum_cas.Cas

data class OdjezdyState(
    val cas: Cas,
    val indexScrollovani: Int = 0,
    val filtrLinky: Int? = null,
    val filtrZastavky: String? = null,
    val kompaktniRezim: Boolean = false,
)