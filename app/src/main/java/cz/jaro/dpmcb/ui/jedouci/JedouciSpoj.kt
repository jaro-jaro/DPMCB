package cz.jaro.dpmcb.ui.jedouci

import java.time.LocalTime

data class JedouciSpoj(
    val spojId: String,
    val pristiZastavkaNazev: String,
    val pristiZastavkaCas: LocalTime,
    val zpozdeni: Float,
    val vuz: Int,
)