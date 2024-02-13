package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class Kody(
    val pevneKody: String,
    val jede: Boolean,
    val od: LocalDate,
    val `do`: LocalDate,
)