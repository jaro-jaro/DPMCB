package cz.jaro.dpmcb.data.jikord

data class OnlineTimetable(
    val stops: List<OnlineConnStop>,
    val nextStopIndex: Int?
)