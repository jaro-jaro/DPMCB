package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class Validity(
    val validFrom: LocalDate,
    val validTo: LocalDate,
)
