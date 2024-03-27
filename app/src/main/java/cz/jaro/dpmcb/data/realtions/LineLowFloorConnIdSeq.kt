package cz.jaro.dpmcb.data.realtions

data class LineLowFloorConnIdSeq(
    val lowFloor: Boolean,
    val line: Int,
    val connId: String,
    val sequence: String?,
)