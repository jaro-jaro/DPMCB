package cz.jaro.dpmcb.data.realtions

import cz.jaro.datum_cas.Cas

data class OdjezdNizkopodlaznostSpojId(
    val odjezd: Cas,
    val nizkopodlaznost: Boolean,
    val spojId: String,
    val pevneKody: String,
)