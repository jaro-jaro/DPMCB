package cz.jaro.dpmcb.data.entities

//@kotlinx.serialization.Serializable
//data class NeexistujiciKurz(
//    val jmeno: String,
//
//    val cisloLinky: Int,
//
//    val nizkopodlaznost: Boolean,
//    val vyjmecnosti: Int,
//
//    val spoje: List<Spoj>,
//)
//
//@kotlinx.serialization.Serializable
//data class StarejKurz(
//    val jmeno: String,
//
//    val cisloLinky: Int,
//
//    val nizkopodlaznost: Boolean,
//    val vyjmecnosti: Int,
//
//    val idSpoju: List<Long>,
//) {
//
//    val jedeVDen: VDP
//        get() = when (jmeno[0]) {
//            'V' -> VDP.VIKENDY
//            'D' -> VDP.DNY
//            'P' -> VDP.PRAZDNINY
//            else -> throw IllegalArgumentException(jmeno[0] + " není V, D ani P!")
//        }
//}
