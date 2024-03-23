package cz.jaro.dpmcb.data.realtions

data class InfoStopsCodesSequence(
    val info: LineLowFloorConnIdSeq,
    val stops: List<LineTimeNameConnIdNextStop>,
    val timeCodes: List<RunsFromTo>,
    val fixedCodes: String,
    val sequence: List<String>?,
)