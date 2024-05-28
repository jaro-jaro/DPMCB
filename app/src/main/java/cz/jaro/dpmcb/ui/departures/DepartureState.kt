package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.realtions.StopType
import java.time.Duration
import java.time.LocalTime

data class DepartureState(
    val destination: String,
    val currentNextStop: Pair<String, LocalTime>?,
    val nextStop: String?,
    val stopType: StopType,
    val lineNumber: Int,
    val time: LocalTime,
    val busName: String,
    val lowFloor: Boolean,
    val confirmedLowFloor: Boolean?,
    val delay: Float?,
    val runsVia: List<String>,
    val runsIn: Duration,
)