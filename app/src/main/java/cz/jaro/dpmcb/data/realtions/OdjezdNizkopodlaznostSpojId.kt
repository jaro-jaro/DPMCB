package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class OdjezdNizkopodlaznostSpojId(
    val odjezd: LocalTime,
    val nizkopodlaznost: Boolean,
    val spojId: String,
    val cil: String,
    val pevneKody: String,
    val zpozdeni: Float? = null,
)