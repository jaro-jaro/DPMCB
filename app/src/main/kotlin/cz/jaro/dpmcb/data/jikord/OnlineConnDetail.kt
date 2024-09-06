package cz.jaro.dpmcb.data.jikord

data class OnlineConnDetail(
    val stops: List<OnlineConnStop>,
    val nextStopIndex: Int?
)