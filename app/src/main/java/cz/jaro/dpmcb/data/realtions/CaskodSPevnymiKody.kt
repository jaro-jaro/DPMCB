package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class CaskodSPevnymiKody(
    val jede: Boolean,
    val od: LocalDate,
    val `do`: LocalDate,
    val pevneKody: String,
)
