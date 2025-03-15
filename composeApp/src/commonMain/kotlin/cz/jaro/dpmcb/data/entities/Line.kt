package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import cz.jaro.dpmcb.data.entities.types.LineType
import cz.jaro.dpmcb.data.entities.types.VehicleType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlin.jvm.JvmName

@Entity("", [], false, primaryKeys = ["tab"], [], [])
@Serializable
data class Line(
// Primary keys
    val tab: Table,
// Other
    val number: LongLine,
    val route: String,
    @SerialName("vehicletype")
    val vehicleType: VehicleType,
    @SerialName("linetype")
    val lineType: LineType,
    @SerialName("hasrestriction")
    val hasRestriction: Boolean,
    @SerialName("validfrom")
    val validFrom: LocalDate,
    @SerialName("validto")
    val validTo: LocalDate,
) {
    @get:JvmName("getShortNumber")
    @SerialName("shortnumber")
    var shortNumber = number.toShortLine()
        internal set
}