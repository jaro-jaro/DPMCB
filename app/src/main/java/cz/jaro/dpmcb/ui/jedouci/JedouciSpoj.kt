package cz.jaro.dpmcb.ui.jedouci

import cz.jaro.datum_cas.Cas
import cz.jaro.dpmcb.data.helperclasses.Smer

data class JedouciSpoj(
    val cisloLinky: Int,
    val spojId: String,
    val cilovaZastavka: Pair<String, Cas>,
    val pristiZastavka: Pair<String, Cas>,
    val zpozdeni: Int,
    val indexNaLince: Int,
    val smer: Smer,
)