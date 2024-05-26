package cz.jaro.dpmcb.ui.sequence

import cz.jaro.dpmcb.data.realtions.BusStop

data class BusInSequence(
    val busName: String,
    val stops: List<BusStop>,
    val lineNumber: Int,
    val lowFloor: Boolean,
    val isRunning: Boolean,
    val shouldBeRunning: Boolean,
)