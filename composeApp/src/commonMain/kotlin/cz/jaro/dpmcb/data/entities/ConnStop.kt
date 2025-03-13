package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Entity("", [], false, primaryKeys = ["tab", "connNumber", "stopIndexOnLine"], [], [])
@Serializable
data class ConnStop(
// Primary keys
    val tab: Table,
    val connNumber: BusNumber,
    val stopIndexOnLine: Int,
// Other
    val line: LongLine,
    val stopNumber: StopNumber,
    val kmFromStart: Int,
    val fixedCodes: String,
    val arrival: LocalTime?,
    val departure: LocalTime?,
) {
    val time get() = departure ?: arrival
}