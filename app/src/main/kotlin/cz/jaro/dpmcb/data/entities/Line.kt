package cz.jaro.dpmcb.data.entities

import androidx.room.Embedded
import androidx.room.Entity
import cz.jaro.dpmcb.data.entities.types.LineType
import cz.jaro.dpmcb.data.entities.types.VehicleType

@Entity(primaryKeys = ["tab"])
data class Line(
// Primary keys
    val tab: Table,
// Other
    val number: LongLine,
    val route: String,
    val vehicleType: VehicleType,
    val lineType: LineType,
    val hasRestriction: Boolean,
    @Embedded
    val validity: Validity,
) {
    @get:JvmName("getShortNumber")
    var shortNumber = number.toShortLine()
        internal set
}