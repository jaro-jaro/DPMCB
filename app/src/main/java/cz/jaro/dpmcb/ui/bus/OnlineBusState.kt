package cz.jaro.dpmcb.ui.bus

import cz.jaro.dpmcb.data.jikord.OnlineConnDetail
import java.time.LocalTime
import kotlin.time.Duration

data class OnlineBusState(
    val onlineConnDetail: OnlineConnDetail? = null,
    val delay: Duration? = null,
    val vehicle: Int? = null,
    val confirmedLowFloor: Boolean? = null,
    val nextStopTime: LocalTime? = null,
)