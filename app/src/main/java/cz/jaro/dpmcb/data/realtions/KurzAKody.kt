package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class SpojKurzAKody(
    val kurz: String?,
    val jede: Boolean,
    val od: LocalDate,
    val `do`: LocalDate,
    val pevneKody: String,
    val tab: String,
    val spojId: String,
)
