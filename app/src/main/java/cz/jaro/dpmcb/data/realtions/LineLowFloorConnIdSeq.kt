package cz.jaro.dpmcb.data.realtions

data class LineLowFloorConnIdSeq(
    val lowFloor: Boolean,
    val line: Int,
    val connName: String,
    val sequence: String?,
)