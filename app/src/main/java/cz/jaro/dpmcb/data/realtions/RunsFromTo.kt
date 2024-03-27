package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class RunsFromTo(
    val runs: Boolean,
    val `in`: ClosedRange<LocalDate>,
)