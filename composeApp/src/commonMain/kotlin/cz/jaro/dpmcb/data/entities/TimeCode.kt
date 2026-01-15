package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.Companion.runs
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TimeCode(
// Primary keys
    val tab: Table,
    val connNumber: BusNumber,
    val code: Short,
    val termIndex: Short,
// Other
    val line: LongLine,
    val type: TimeCodeType,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val runs2: Boolean = type.runs,
)