package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.entities.types.Direction

expect class Conn(
    tab: Table,
    connNumber: BusNumber,
    line: LongLine,
    fixedCodes: String,
    direction: Direction,
) {
    val tab: Table
    val connNumber: BusNumber
    val line: LongLine
    val fixedCodes: String
    val direction: Direction
    var name: BusName
        internal set
}