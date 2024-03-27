package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
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
    val runs: Boolean,
    val validFrom: LocalDate,
    val validTo: LocalDate,
)
