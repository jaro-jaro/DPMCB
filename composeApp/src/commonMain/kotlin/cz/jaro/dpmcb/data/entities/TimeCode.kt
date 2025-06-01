package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.Companion.runs
import kotlinx.datetime.LocalDate

expect class TimeCode(
    tab: Table,
    connNumber: BusNumber,
    code: Int,
    termIndex: Int,
    line: LongLine,
    type: TimeCodeType,
    validFrom: LocalDate,
    validTo: LocalDate,
    runs2: Boolean = type.runs,
) {
    val tab: Table
    val connNumber: BusNumber
    val code: Int
    val termIndex: Int
    val line: LongLine
    val type: TimeCodeType
    val validFrom: LocalDate
    val validTo: LocalDate
    val runs2: Boolean
}