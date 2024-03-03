package cz.jaro.dpmcb.ui.bus

import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import java.time.LocalTime

data class OnlineBusState(
    val onlineConnStops: List<OnlineConnStop>? = null,
    val delay: kotlin.time.Duration? = null,
    val vehicle: Int? = null,
    val confirmedLowFloor: Boolean? = null,
    val nextStopTime: LocalTime? = null,
)