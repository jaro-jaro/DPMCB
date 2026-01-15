package cz.jaro.dpmcb.data.entities

import kotlinx.serialization.Serializable

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