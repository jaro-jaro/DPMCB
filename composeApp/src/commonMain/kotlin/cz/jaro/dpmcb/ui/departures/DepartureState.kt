package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.realtions.StopType
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

data class DepartureState(
    val destination: String,
    val currentNextStop: Pair<String, LocalTime>?,
    val platform: Platform?,
    val isLastStop: Boolean,
    val stopType: StopType,
    val lineNumber: ShortLine,
    val time: LocalTime,
    val busName: BusName,
    val lineTraction: Traction,
    val vehicleTraction: Traction?,
    val delay: Duration?,
    val runsVia: List<String>,
)