package cz.jaro.dpmcb.ui.sequence

import cz.jaro.dpmcb.data.realtions.LineTimeNameConnIdNextStop

data class BusInSequence(
    val busId: String,
    val stops: List<LineTimeNameConnIdNextStop>,
    val lineNumber: Int,
    val lowFloor: Boolean,
    val isRunning: Boolean,
)