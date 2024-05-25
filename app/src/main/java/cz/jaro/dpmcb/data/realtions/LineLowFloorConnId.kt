package cz.jaro.dpmcb.data.realtions

data class LineLowFloorConnId(
    val lowFloor: Boolean,
    val line: Int,
    val connName: String,
)