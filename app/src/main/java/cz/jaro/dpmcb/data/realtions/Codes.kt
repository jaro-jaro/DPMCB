package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class Codes(
    val fixedCodes: String,
    val runs: Boolean,
    val from: LocalDate,
    val to: LocalDate,
)