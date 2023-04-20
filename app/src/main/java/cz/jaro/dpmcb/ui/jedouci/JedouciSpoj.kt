package cz.jaro.dpmcb.ui.jedouci

import cz.jaro.dpmcb.data.helperclasses.Smer
import java.time.LocalTime

data class JedouciSpoj(
    val cisloLinky: Int,
    val spojId: String,
    val cilovaZastavka: Pair<String, LocalTime>,
    val pristiZastavka: Pair<String, LocalTime>,
    val zpozdeni: Int,
    val indexNaLince: Int,
    val smer: Smer,
)