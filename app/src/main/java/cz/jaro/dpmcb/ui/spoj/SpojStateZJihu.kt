package cz.jaro.dpmcb.ui.spoj

import cz.jaro.dpmcb.data.jikord.ZastavkaOnlineSpoje
import java.time.LocalTime

data class SpojStateZJihu(
    val zastavkyNaJihu: List<ZastavkaOnlineSpoje>? = null,
    val zpozdeni: kotlin.time.Duration? = null,
    val vuz: Int? = null,
    val potvrzenaNizkopodlaznost: Boolean? = null,
    val pristiZastavka: LocalTime? = null,
)