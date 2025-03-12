package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import kotlinx.datetime.LocalTime

@Entity("", [], false, primaryKeys = ["tab", "connNumber", "stopIndexOnLine"], [], [])
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