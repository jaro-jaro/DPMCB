package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.Companion.runs
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Entity("", [], false, primaryKeys = ["tab", "connNumber", "code", "termIndex"], [], [])
@Serializable
data class TimeCode(
// Primary keys
    val tab: Table,
    val connNumber: BusNumber,
    val code: Int,
    val termIndex: Int,
// Other
    val line: LongLine,
    val type: TimeCodeType,
    val validFrom: LocalDate,
    val validTo: LocalDate,

    val runs2: Boolean = type.runs,
)
