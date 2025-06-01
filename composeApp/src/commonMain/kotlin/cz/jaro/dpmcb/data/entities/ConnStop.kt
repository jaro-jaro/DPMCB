package cz.jaro.dpmcb.data.entities

import kotlinx.datetime.LocalTime

expect class ConnStop(
    tab: Table,
    connNumber: BusNumber,
    stopIndexOnLine: Int,
    line: LongLine,
    stopNumber: StopNumber,
    kmFromStart: Int,
    fixedCodes: String,
    arrival: LocalTime?,
    departure: LocalTime?,
) {
    val tab: Table
    val connNumber: BusNumber
    val stopIndexOnLine: Int
    val line: LongLine
    val stopNumber: StopNumber
    val kmFromStart: Int
    val fixedCodes: String
    val arrival: LocalTime?
    val departure: LocalTime?
    val time: LocalTime?
}