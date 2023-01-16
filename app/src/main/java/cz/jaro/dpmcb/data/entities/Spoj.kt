package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.jaro.dpmcb.data.helperclasses.Smer

@kotlinx.serialization.Serializable
@Entity
data class Spoj(
    @PrimaryKey val id: Long,

    val cisloLinky: Int,
    val nazevKurzu: String,

    val smer: Smer,
    val nizkopodlaznost: Boolean,
    val vyjmecnosti: Int,
)