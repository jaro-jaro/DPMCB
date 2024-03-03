package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class TimeLowFloorConnIdDestinationFixedCodesDelay(
    val departure: LocalTime,
    val lowFloor: Boolean,
    val connId: String,
    val destination: String,
    val fixedCodes: String,
    val delay: Float? = null,
)