package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.entities.FareZone
import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.datetime.LocalTime

data class StopOfDeparture(
    val name: StopName,
    val time: LocalTime,
    val fareZone: FareZone?,
    val stopIndexOnLine: Int,
)