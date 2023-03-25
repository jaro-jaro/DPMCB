package cz.jaro.dpmcb.data.realtions

import cz.jaro.datum_cas.Cas

data class CasNazevSpojId(
    val cas: Cas,
    val nazev: String,
    val spojId: String,
)