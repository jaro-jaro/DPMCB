package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import cz.jaro.dpmcb.data.entities.types.LineType
import cz.jaro.dpmcb.data.entities.types.VehicleType
import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmName

@Entity("", [], false, primaryKeys = ["tab"], [], [])
data class Line(
// Primary keys
    val tab: Table,
// Other
    val number: LongLine,
    val route: String,
    val vehicleType: VehicleType,
    val lineType: LineType,
    val hasRestriction: Boolean,
    val validFrom: LocalDate,
    val validTo: LocalDate,
) {
    @get:JvmName("getShortNumber")
    var shortNumber = number.toShortLine()
        internal set
}