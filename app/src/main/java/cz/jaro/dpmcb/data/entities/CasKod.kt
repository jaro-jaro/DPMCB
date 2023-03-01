package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.helperclasses.Datum

@Entity(primaryKeys = ["linka", "cisloSpoje", "kod", "indexTerminu"])
data class CasKod(
    val linka: Int,
    val cisloSpoje: Int,
    val kod: Int,
    val indexTerminu: Int,
    val jede: Boolean,
    val platiOd: Datum,
    val platiDo: Datum,
)
