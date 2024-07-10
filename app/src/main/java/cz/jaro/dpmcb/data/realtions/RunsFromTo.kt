package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import kotlinx.datetime.LocalDate

data class RunsFromTo(
    val type: TimeCodeType,
    val `in`: ClosedRange<LocalDate>,
)