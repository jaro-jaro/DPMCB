package cz.jaro.dpmcb.data.entities

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Validity(
    val validFrom: LocalDate,
    val validTo: LocalDate,
)
