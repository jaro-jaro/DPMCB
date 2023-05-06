package cz.jaro.dpmcb.data.entities

import androidx.room.Entity

@Entity(primaryKeys = ["tab", "cisloZastavky"])
data class Zastavka(
// Primary keys
    val tab: String,
    val cisloZastavky: Int,
// Other
    val linka: Int,
    val nazevZastavky: String,
    val pevneKody: String,
)