package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import java.time.LocalDate

data class RunsFromTo(
    val type: TimeCodeType,
    val `in`: ClosedRange<LocalDate>,
)