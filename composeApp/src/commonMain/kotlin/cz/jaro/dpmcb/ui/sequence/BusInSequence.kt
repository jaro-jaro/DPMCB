package cz.jaro.dpmcb.ui.sequence

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.realtions.BusStop

data class BusInSequence(
    val busName: BusName,
    val stops: List<BusStop>,
    val isOneWay: Boolean,
    val direction: Direction,
    val lineNumber: ShortLine,
    val lowFloor: Boolean,
    val isRunning: Boolean,
    val shouldBeRunning: Boolean,
    val timeCodes: List<String>,
    val fixedCodes: List<String>,
    val lineCode: String,
)