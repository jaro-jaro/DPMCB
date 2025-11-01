package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(primaryKeys = ["tab", "stopNumber"])
@Serializable
data class Stop(
// Primary keys
    val tab: Table,
    val stopNumber: StopNumber,
// Other
    val line: LongLine,
    val stopName: String,
    val fixedCodes: String,
)