package cz.jaro.dpmcb.data.realtions.sequence

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import java.time.LocalDate
import java.time.LocalTime

data class CoreBusOfSequence(
    val lowFloor: Boolean,
    val line: Int,
    val sequence: String?,
    val fixedCodes: String,
    val stopFixedCodes: String,
    val connStopFixedCodes: String,
    val time: LocalTime,
    val name: String,
    val connName: String,
    val type: TimeCodeType,
    val from: LocalDate,
    val to: LocalDate,
    val tab: String,
)