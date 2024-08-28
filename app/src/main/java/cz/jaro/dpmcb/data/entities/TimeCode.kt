package cz.jaro.dpmcb.data.entities

import androidx.room.Embedded
import androidx.room.Entity
import cz.jaro.dpmcb.data.entities.types.TimeCodeType

@Entity(primaryKeys = ["tab", "connNumber", "code", "termIndex"])
data class TimeCode(
// Primary keys
    val tab: Table,
    val connNumber: BusNumber,
    val code: Int,
    val termIndex: Int,
// Other
    val line: LongLine,
    val type: TimeCodeType,
    @Embedded
    val validity: Validity,

    val runs2: Boolean = type != TimeCodeType.DoesNotRun,
)
