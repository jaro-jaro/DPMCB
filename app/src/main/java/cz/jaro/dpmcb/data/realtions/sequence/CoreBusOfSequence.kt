package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class CoreBusOfSequence(
    val lowFloor: Boolean,
    val line: LongLine,
    val sequence: SequenceCode?,
    val fixedCodes: String,
    val stopFixedCodes: String,
    val connStopFixedCodes: String,
    val time: LocalTime,
    val name: String,
    val connName: BusName,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
    val tab: Table,
)