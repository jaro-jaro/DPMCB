package cz.jaro.dpmcb.data.realtions

data class LinkaNizkopodlaznostSpojIdKurz(
    val nizkopodlaznost: Boolean,
    val linka: Int,
    val spojId: String,
    val kurz: String?,
)