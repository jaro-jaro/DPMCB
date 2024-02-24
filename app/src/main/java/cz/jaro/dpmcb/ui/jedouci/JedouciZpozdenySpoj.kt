package cz.jaro.dpmcb.ui.jedouci

data class JedouciZpozdenySpoj(
    val spojId: String,
    val kurz: String?,
    val zpozdeni: Float,
    val cisloLinky: Int,
    val cilovaZastavka: String,
)