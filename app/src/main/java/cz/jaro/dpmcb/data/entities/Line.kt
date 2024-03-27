package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.helperclasses.LineType
import cz.jaro.dpmcb.data.helperclasses.VehicleType
import java.time.LocalDate

@Entity(primaryKeys = ["tab"])
data class Line(
// Primary keys
    val tab: String,
// Other
    val number: Int,
    val route: String,
    val vehicleType: VehicleType,
    val lineType: LineType,
    val hasRestriction: Boolean,
    val validFrom: LocalDate,
    val validTo: LocalDate,

    val shortNumber: Int = number - 325_000,
)
