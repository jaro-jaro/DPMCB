package cz.jaro.dpmcb.ui.departures

import java.time.Duration
import java.time.LocalTime

data class DepartureState(
    val destination: String,
    val currentNextStop: Pair<String, LocalTime>?,
    val nextStop: String?,
    val lineNumber: Int,
    val time: LocalTime,
    val busId: String,
    val lowFloor: Boolean,
    val confirmedLowFloor: Boolean?,
    val delay: Float?,
    val runsVia: List<String>,
    val runsIn: Duration,
)