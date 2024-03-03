package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import java.time.LocalTime

@Entity(primaryKeys = ["tab", "connNumber", "stopIndexOnLine"])
data class ConnStop(
// Primary keys
    val tab: String,
    val connNumber: Int,
    val stopIndexOnLine: Int,
// Other
    val line: Int,
    val stopNumber: Int,
    val kmFromStart: Int,
    val arrival: LocalTime?,
    val departure: LocalTime?,
) {
    val time get() = departure ?: arrival
}