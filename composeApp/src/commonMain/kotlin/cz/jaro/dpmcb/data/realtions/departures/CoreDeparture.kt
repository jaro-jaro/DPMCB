package cz.jaro.dpmcb.data.realtions.departures

import cz.jaro.dpmcb.data.entities.BusNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoreDeparture(
    val tab: Table,
    val name: String,
    val direction: Direction,
    @SerialName("time_")
    val time: LocalTime,
    @SerialName("stopindexonline")
    val stopIndexOnLine: Int,
    @SerialName("connstopfixedcodes")
    val connStopFixedCodes: String,
    @SerialName("connnumber")
    val connNumber: BusNumber,
    val line: LongLine,
    @SerialName("lowfloor")
    val lowFloor: Boolean,
    @SerialName("fixedcodes")
    val fixedCodes: String,
    val type: TimeCodeType,
    @SerialName("validfrom")
    val from: LocalDate,
    @SerialName("validto")
    val to: LocalDate,
)
