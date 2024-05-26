package cz.jaro.dpmcb.data.realtions.bus

import java.time.LocalDate

data class CodesOfBus(
    val fixedCodes: String,
    val runs: Boolean,
    val from: LocalDate,
    val to: LocalDate,
)