package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.LineType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.VehicleType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Entity(primaryKeys = ["tab", "connNumber"])
actual data class Conn actual constructor(
// Primary keys
    actual val tab: Table,
    actual val connNumber: BusNumber,
// Other
    actual val line: LongLine,
    actual val fixedCodes: String,
    actual val direction: Direction,
) {
    @get:JvmName("getName")
    actual var name = BusName(line, connNumber)
}

@Entity(primaryKeys = ["tab", "connNumber", "stopIndexOnLine"])
actual data class ConnStop actual constructor(
// Primary keys
    actual val tab: Table,
    actual val connNumber: BusNumber,
    actual val stopIndexOnLine: Int,
// Other
    actual val line: LongLine,
    actual val stopNumber: StopNumber,
    actual val kmFromStart: Int,
    actual val fixedCodes: String,
    actual val arrival: LocalTime?,
    actual val departure: LocalTime?,
) {
    actual val time get() = departure ?: arrival
}

@Entity(primaryKeys = ["tab"])
actual data class Line actual constructor(
// Primary keys
    actual val tab: Table,
// Other
    actual val number: LongLine,
    actual val route: String,
    actual val vehicleType: VehicleType,
    actual val lineType: LineType,
    actual val hasRestriction: Boolean,
    actual val validFrom: LocalDate,
    actual val validTo: LocalDate,
) {
    @get:JvmName("getShortNumber")
    actual var shortNumber = number.toShortLine()
}

@Entity(primaryKeys = ["group"])
actual data class SeqGroup actual constructor(
// Primary keys
    actual val group: SequenceGroup,
// Other
    actual val validFrom: LocalDate,
    actual val validTo: LocalDate,
)

@Entity(primaryKeys = ["line", "connNumber", "sequence", "group"])
actual data class SeqOfConn actual constructor(
// Primary keys
    actual val line: LongLine,
    actual val connNumber: BusNumber,
    actual val sequence: SequenceCode,
    actual val group: SequenceGroup,
// Other
    actual val orderInSequence: Int?,
)

@Entity(primaryKeys = ["tab", "stopNumber"])
actual data class Stop actual constructor(
// Primary keys
    actual val tab: Table,
    actual val stopNumber: StopNumber,
// Other
    actual val line: LongLine,
    actual val stopName: String,
    actual val fixedCodes: String,
)

@Entity(primaryKeys = ["tab", "connNumber", "code", "termIndex"])
actual data class TimeCode actual constructor(
// Primary keys
    actual val tab: Table,
    actual val connNumber: BusNumber,
    actual val code: Int,
    actual val termIndex: Int,
// Other
    actual val line: LongLine,
    actual val type: TimeCodeType,
    actual val validFrom: LocalDate,
    actual val validTo: LocalDate,
    actual val runs2: Boolean,
)