package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.entities.types.LineType
import cz.jaro.dpmcb.data.entities.types.VehicleType
import kotlinx.datetime.LocalDate

expect class Line(
    tab: Table,
    number: LongLine,
    route: String,
    vehicleType: VehicleType,
    lineType: LineType,
    hasRestriction: Boolean,
    validFrom: LocalDate,
    validTo: LocalDate,
) {
    val tab: Table
    val number: LongLine
    val route: String
    val vehicleType: VehicleType
    val lineType: LineType
    val hasRestriction: Boolean
    val validFrom: LocalDate
    val validTo: LocalDate

    var shortNumber: ShortLine
        internal set
}