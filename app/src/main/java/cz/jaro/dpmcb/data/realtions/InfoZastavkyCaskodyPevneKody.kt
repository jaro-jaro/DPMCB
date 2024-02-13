package cz.jaro.dpmcb.data.realtions

data class InfoZastavkyCaskodyPevneKody(
    val info: LinkaNizkopodlaznostSpojIdKurz,
    val zastavky: List<CasNazevSpojIdLinkaPristi>,
    val caskody: List<JedeOdDo>,
    val pevneKody: List<String>,
)