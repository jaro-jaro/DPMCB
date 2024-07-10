package cz.jaro.dpmcb.ui.bus

import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.jikord.OnlineConnDetail
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

data class OnlineBusState(
    val onlineConnDetail: OnlineConnDetail? = null,
    val delay: Duration? = null,
    val vehicle: RegistrationNumber? = null,
    val confirmedLowFloor: Boolean? = null,
    val nextStopTime: LocalTime? = null,
)