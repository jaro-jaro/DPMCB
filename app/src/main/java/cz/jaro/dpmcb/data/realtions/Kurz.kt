package cz.jaro.dpmcb.data.realtions

data class Kurz(
    val nazev: String,
    val navaznostiPredtim: List<String>,
    val spoje: List<InfoZastavky>,
    val navaznostiPotom: List<String>,
    val spolecneCaskody: List<JedeOdDo>,
    val spolecnePevneKody: List<String>,
)
