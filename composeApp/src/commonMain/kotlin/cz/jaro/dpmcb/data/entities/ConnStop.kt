package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Entity(primaryKeys = ["tab", "connNumber", "stopIndexOnLine"])
@Serializable
data class ConnStop(
// Primary keys
    val tab: Table,
    val connNumber: BusNumber,
    val stopIndexOnLine: LineStopNumber,
// Other
    val line: LongLine,
    val stopNumber: StopNumber,
    val kmFromStart: Int,
    val fixedCodes: String,
    val arrival: LocalTime?,
    val departure: LocalTime?,
    val platform: Platform?,
) {
    val time get() = departure ?: arrival
}