package cz.jaro.dpmcb.data.realtions.bus

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import java.time.LocalDate

data class CodesOfBus(
    val fixedCodes: String,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
)