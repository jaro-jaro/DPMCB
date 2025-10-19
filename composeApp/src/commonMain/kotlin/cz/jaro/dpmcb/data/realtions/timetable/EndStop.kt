package cz.jaro.dpmcb.data.realtions.timetable

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EndStop(
    val direction: Direction,
    val stopIndexOnLine: Int,
    val connName: BusName,
    val stopName: String,
    val fixedCodes: String,
    val stopFixedCodes: String,
    val type: TimeCodeType,
    @SerialName("validfrom")
    val from: LocalDate,
    @SerialName("validto")
    val to: LocalDate,
)