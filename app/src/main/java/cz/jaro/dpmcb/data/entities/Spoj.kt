package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.helperclasses.Smer

@Entity(primaryKeys = ["linka", "cisloSpoje"])
data class Spoj(
    val linka: Int,
    val cisloSpoje: Int,
    val pevneKody: String,
    val smer: Smer,
) {
    var id = "S-$linka-$cisloSpoje"
        internal set
}