package cz.jaro.dpmcb.data.entities

import androidx.room.Entity

@Entity(primaryKeys = ["linka", "cisloZastavky"])
data class Zastavka(
    val linka: Int,
    val cisloZastavky: Int,
    val nazevZastavky: String,
    val pevneKody: String,
)