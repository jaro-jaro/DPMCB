package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.VehicleType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoreBus(
    val lowFloor: Boolean,
    val line: LongLine,
    val sequence: SequenceCode,
    val fixedCodes: String,
    val stopFixedCodes: String,
    val connStopFixedCodes: String,
    @SerialName("time_")
    val time: LocalTime,
    val arrival: LocalTime?,
    val name: String,
    val connName: BusName,
    val type: TimeCodeType,
    @SerialName("validfrom")
    val from: LocalDate,
    @SerialName("validto")
    val to: LocalDate,
    @SerialName("seqGroup")
    val group: SequenceGroup,
    val vehicleType: VehicleType,
)