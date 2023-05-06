package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import java.time.LocalDate

@Entity(primaryKeys = ["tab", "cisloSpoje", "kod", "indexTerminu"])
data class CasKod(
// Primary keys
    val tab: String,
    val cisloSpoje: Int,
    val kod: Int,
    val indexTerminu: Int,
// Other
    val linka: Int,
    val jede: Boolean,
    val platiOd: LocalDate,
    val platiDo: LocalDate,
)
