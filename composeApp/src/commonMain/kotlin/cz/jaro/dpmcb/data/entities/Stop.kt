package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import kotlinx.serialization.Serializable

@Entity("", [], false, primaryKeys = ["tab", "stopNumber"], [], [])
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