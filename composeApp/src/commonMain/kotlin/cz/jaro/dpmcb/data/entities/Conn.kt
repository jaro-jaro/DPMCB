package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.entities.types.Direction
import kotlin.jvm.JvmName

@Entity(primaryKeys = ["tab", "connNumber"])
data class Conn(
// Primary keys
    val tab: Table,
    val connNumber: BusNumber,
// Other
    val line: LongLine,
    val fixedCodes: String,
    val direction: Direction,
) {
    @get:JvmName("getName")
    var name = BusName(line, connNumber)
        internal set
}