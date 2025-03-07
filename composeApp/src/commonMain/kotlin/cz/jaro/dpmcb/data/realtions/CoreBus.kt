package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.TimeCodeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class CoreBus(
    val lowFloor: Boolean,
    val line: LongLine,
    val sequence: SequenceCode,
    val fixedCodes: String,
    val stopFixedCodes: String,
    val connStopFixedCodes: String,
    val time: LocalTime,
    val name: String,
    val connName: BusName,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
    val group: SequenceGroup,
)