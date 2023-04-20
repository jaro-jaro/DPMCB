package cz.jaro.dpmcb.ui.spoj

import cz.jaro.dpmcb.data.naJihu.ZastavkaSpojeNaJihu

data class DetailSpojeStateZJihu(
    val zpozdeni: Int? = null,
    val zastavkyNaJihu: List<ZastavkaSpojeNaJihu>? = null,
)