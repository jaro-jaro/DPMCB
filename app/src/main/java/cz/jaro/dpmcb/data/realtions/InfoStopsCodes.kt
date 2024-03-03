package cz.jaro.dpmcb.data.realtions

data class InfoStopsCodes(
    val info: LineLowFloorConnIdSeq,
    val stops: List<LineTimeNameConnIdNextStop>,
    val timeCodes: List<RunsFromTo>,
    val fixedCodes: String,
)