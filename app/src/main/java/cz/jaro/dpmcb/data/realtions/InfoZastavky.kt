package cz.jaro.dpmcb.data.realtions

data class InfoZastavky(
    val info: LinkaNizkopodlaznostSpojIdKurz,
    val zastavky: List<CasNazevSpojIdLinkaPristi>,
)