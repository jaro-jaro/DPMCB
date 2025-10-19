package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.LineType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.VehicleType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
actual data class Conn actual constructor(
    actual val tab: Table,
    actual val connNumber: BusNumber,
    actual val line: LongLine,
    actual val fixedCodes: String,
    actual val direction: Direction,
) {
    actual var name = BusName(line, connNumber)
}

@Serializable
actual data class ConnStop actual constructor(
    actual val tab: Table,
    actual val connNumber: BusNumber,
    actual val stopIndexOnLine: Int,
    actual val line: LongLine,
    actual val stopNumber: StopNumber,
    actual val kmFromStart: Int,
    actual val fixedCodes: String,
    actual val arrival: LocalTime?,
    actual val departure: LocalTime?,
) {
    actual val time get() = departure ?: arrival
}

@Serializable
actual data class Line actual constructor(
    actual val tab: Table,
    actual val number: LongLine,
    actual val route: String,
    actual val vehicleType: VehicleType,
    actual val lineType: LineType,
    actual val hasRestriction: Boolean,
    actual val validFrom: LocalDate,
    actual val validTo: LocalDate,
) {
    actual var shortNumber = number.toShortLine()
        internal set
}

@Serializable
actual data class SeqGroup actual constructor(
    @SerialName("groupNumber")
    actual val group: SequenceGroup,
    actual val validFrom: LocalDate,
    actual val validTo: LocalDate,
)

@Serializable
actual data class SeqOfConn actual constructor(
    actual val line: LongLine,
    actual val connNumber: BusNumber,
    actual val sequence: SequenceCode,
    @SerialName("seqGroup")
    actual val group: SequenceGroup,
    actual val orderInSequence: Int?,
)

@Serializable
actual data class Stop actual constructor(
    actual val tab: Table,
    actual val stopNumber: StopNumber,
    actual val line: LongLine,
    actual val stopName: String,
    actual val fixedCodes: String,
)

@Serializable
actual data class TimeCode actual constructor(
    actual val tab: Table,
    actual val connNumber: BusNumber,
    actual val code: Int,
    actual val termIndex: Int,
    actual val line: LongLine,
    actual val type: TimeCodeType,
    actual val validFrom: LocalDate,
    actual val validTo: LocalDate,
    actual val runs2: Boolean,
)