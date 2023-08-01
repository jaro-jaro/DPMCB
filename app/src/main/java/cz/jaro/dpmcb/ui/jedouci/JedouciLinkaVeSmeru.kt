package cz.jaro.dpmcb.ui.jedouci

data class JedouciLinkaVeSmeru(
    val cisloLinky: Int,
    val cilovaZastavka: String,
    val spoje: List<JedouciSpoj>,
)
