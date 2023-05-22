package cz.jaro.dpmcb.ui.spoj

import cz.jaro.dpmcb.data.naJihu.ZastavkaSpojeNaJihu
import kotlin.time.Duration

data class SpojStateZJihu(
    val zpozdeni: Duration? = null,
    val zastavkyNaJihu: List<ZastavkaSpojeNaJihu>? = null,
)