package cz.jaro.dpmcb.data.entities

expect class Stop(
    tab: Table,
    stopNumber: StopNumber,
    line: LongLine,
    stopName: String,
    fixedCodes: String,
) {
    val tab: Table
    val stopNumber: StopNumber
    val line: LongLine
    val stopName: String
    val fixedCodes: String
}