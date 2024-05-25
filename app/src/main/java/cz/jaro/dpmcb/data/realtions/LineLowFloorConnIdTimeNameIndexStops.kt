package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class LineLowFloorConnIdTimeNameIndexStops(
    val name: String,
    val time: LocalTime,
    val stopIndexOnLine: Int,
    val busName: String,
    val line: Int,
    val lowFloor: Boolean,
    val busStops: List<NameTimeIndexOnLine>,
)
