package cz.jaro.dpmcb.ui.odjezdy

import java.time.LocalTime

data class OdjezdyInfo(
    val cas: LocalTime,
    val indexScrollovani: Int = 0,
    val filtrLinky: Int? = null,
    val filtrZastavky: String? = null,
    val kompaktniRezim: Boolean = false,
)