package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.FareZone
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

data class RunningBus(
    val busName: BusName,
    val nextStopName: StopName,
    val nextStopTime: LocalDateTime,
    val nextStopZone: FareZone?,
    val delay: Duration?,
    val vehicle: RegistrationNumber?,
    val sequence: SequenceCode?,
)