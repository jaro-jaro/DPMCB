package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class TimeLowFloorConnDestinationFixedCodesDelay(
    val departure: LocalTime,
    val lowFloor: Boolean,
    val connName: String,
    val destination: String,
    val fixedCodes: String,
    val delay: Float? = null,
)