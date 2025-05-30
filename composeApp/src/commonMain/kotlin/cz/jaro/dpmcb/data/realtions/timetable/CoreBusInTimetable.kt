package cz.jaro.dpmcb.data.realtions.timetable

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoreBusInTimetable(
    val departure: LocalTime,
    val lowFloor: Boolean,
    val connName: BusName,
    val destination: String,
    val fixedCodes: String,
    val type: TimeCodeType,
    @SerialName("validfrom")
    val from: LocalDate,
    @SerialName("validto")
    val to: LocalDate,
)