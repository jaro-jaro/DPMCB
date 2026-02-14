package cz.jaro.dpmcb.data.realtions.now_running

import cz.jaro.dpmcb.data.entities.FareZone
import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.datetime.LocalTime

data class StopOfNowRunning(
    val name: StopName,
    val fareZone: FareZone?,
    val time: LocalTime,
)
