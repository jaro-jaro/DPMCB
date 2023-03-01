package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.helperclasses.Cas

data class CasNazevSpojId(
    val odjezd: Cas?,
    val prijezd: Cas?,
    val nazev: String,
    val spojId: String,
)