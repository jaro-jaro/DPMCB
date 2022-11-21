package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.VDP
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.emptyGraphZastavek

@kotlinx.serialization.Serializable
data class VsechnoOstatni(
    val verze: Int = -1,
    val typDne: VDP = VDP.DNY,

    val linkyAJejichZastavky: Map<Int, List<String>> = emptyMap(),
    val zastavky: List<String> = emptyList(),

    val graphZastavek: GraphZastavek = emptyGraphZastavek(),
)

//@kotlinx.serialization.Serializable
//data class StareUplneVsechno(
//    val verze: Int = -1,
//    val datum: Datum = Datum.dnes,
//
//    val linky: List<Linka> = emptyList(),
//    val zastavky: List<String> = emptyList(),
//
//    val graphZastavek: GraphZastavek = emptyMap()
//) {
//    val spoje get() = linky.flatMap { it.spoje }
//    val zastavkySpoju get() = linky.flatMap { it.spoje }.flatMap { it.zastavkySpoje }
//}
