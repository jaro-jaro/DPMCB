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

//@kotlinx.serialization.Serializable
//data class StarejSpoj(
//    val id: Long,
//
//    val cisloLinky: Int,
//    val nazevKurzu: String,
//
//    val smer: Smer,
//    val nizkopodlaznost: Boolean,
//    val vyjmecnosti: Int,
//
//    val idZastavekSpoje: List<Long>,
//    val nazvyZastavek: List<String>,
//) {
//
//    val jedeVDen: VDP
//    get() = when (nazevKurzu[0]) {
//        'V' -> VDP.VIKENDY
//        'D' -> VDP.DNY
//        'P' -> VDP.PRAZDNINY
//        else -> throw IllegalArgumentException(nazevKurzu[0].toString() + " není V, D ani P!")
//    }
//}
