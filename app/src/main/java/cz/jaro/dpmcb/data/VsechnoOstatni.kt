package cz.jaro.dpmcb.data

@kotlinx.serialization.Serializable
data class VsechnoOstatni(
    val verze: Int = -1,

    val oblibene: List<String> = emptyList(),

    val nastaveni: Nastaveni = Nastaveni(),
)