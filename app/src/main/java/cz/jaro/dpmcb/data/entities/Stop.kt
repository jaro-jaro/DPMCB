package cz.jaro.dpmcb.data.entities

import androidx.room.Entity

@Entity(primaryKeys = ["tab", "stopNumber"])
data class Stop(
// Primary keys
    val tab: String,
    val stopNumber: Int,
// Other
    val line: Int,
    val stopName: String,
    val fixedCodes: String,
)