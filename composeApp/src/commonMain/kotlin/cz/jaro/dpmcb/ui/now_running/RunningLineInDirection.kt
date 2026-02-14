package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.FareZone
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.StopName

data class RunningLineInDirection(
    val lineNumber: LongLine,
    val destination: StopName,
    val destinationZone: FareZone?,
    val buses: List<RunningBus>,
)
