package cz.jaro.dpmcb.data.realtions

import kotlinx.datetime.LocalDate

data class Validity(
    val validFrom: LocalDate,
    val validTo: LocalDate,
)
