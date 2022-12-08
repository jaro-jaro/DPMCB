package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.emptyGraphZastavek

@kotlinx.serialization.Serializable
data class VsechnoOstatni(
    val verze: Int = -1,

    val linkyAJejichZastavky: Map<Int, List<String>> = emptyMap(),
    val zastavky: List<String> = emptyList(),

    val graphZastavek: GraphZastavek = emptyGraphZastavek(),

    val historieVyhledavani: List<Pair<String, String>> = listOf(),

    val idSpoju: Map<Long, String> = mapOf(),
)