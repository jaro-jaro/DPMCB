package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.helperclasses.Smer

@Entity(primaryKeys = ["tab", "cisloSpoje"])
data class Spoj(
// Primary keys
    val tab: String,
    val cisloSpoje: Int,
// Other
    val linka: Int,
    val pevneKody: String,
    val smer: Smer,
    val kurz: String?,
    val poradiNaKurzu: Int?,
) {
    var id = "S-$linka-$cisloSpoje"
        internal set
}