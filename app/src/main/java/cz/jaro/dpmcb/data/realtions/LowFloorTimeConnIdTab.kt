package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class LowFloorTimeConnIdTab(
    val lowFloor: Boolean,
    val time: LocalTime,
    val connId: String,
    val tab: String,
)