package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import java.time.LocalDate

@Entity(primaryKeys = ["linka", "cisloSpoje", "kod", "indexTerminu"])
data class CasKod(
    val linka: Int,
    val cisloSpoje: Int,
    val kod: Int,
    val indexTerminu: Int,
    val jede: Boolean,
    val platiOd: LocalDate,
    val platiDo: LocalDate,
)
