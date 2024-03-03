package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.helperclasses.Direction

@Entity(primaryKeys = ["tab", "connNumber"])
data class Conn(
// Primary keys
    val tab: String,
    val connNumber: Int,
// Other
    val line: Int,
    val fixedCodes: String,
    val direction: Direction,
    val sequence: String?,
    val orderInSequence: Int?,
) {
    var id = "S-$line-$connNumber"
        internal set
}