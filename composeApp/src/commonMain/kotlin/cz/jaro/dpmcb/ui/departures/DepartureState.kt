package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.realtions.StopType
import kotlin.time.Duration
import kotlinx.datetime.LocalTime

data class DepartureState(
    val destination: String,
    val currentNextStop: Pair<String, LocalTime>?,
    val nextStop: String?,
    val stopType: StopType,
    val lineNumber: ShortLine,
    val time: LocalTime,
    val busName: BusName,
    val lowFloor: Boolean,
    val confirmedLowFloor: Boolean?,
    val delay: Float?,
    val runsVia: List<String>,
    val runsIn: Duration,
)