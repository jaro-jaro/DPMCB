package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import java.time.LocalDate

@Entity(primaryKeys = ["tab", "connNumber", "code", "termIndex"])
data class TimeCode(
// Primary keys
    val tab: String,
    val connNumber: Int,
    val code: Int,
    val termIndex: Int,
// Other
    val line: Int,
    val type: TimeCodeType,
    val validFrom: LocalDate,
    val validTo: LocalDate,

    val runs2: Boolean = type != TimeCodeType.DoesNotRun,
)
