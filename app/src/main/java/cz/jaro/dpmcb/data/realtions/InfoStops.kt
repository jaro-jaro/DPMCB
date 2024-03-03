package cz.jaro.dpmcb.data.realtions

data class InfoStops(
    val info: LineLowFloorConnIdSeq,
    val stops: List<LineTimeNameConnIdNextStop>,
)