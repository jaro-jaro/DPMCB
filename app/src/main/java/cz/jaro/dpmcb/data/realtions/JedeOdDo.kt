package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class JedeOdDo(
    val jede: Boolean,
    val v: ClosedRange<LocalDate>,
)