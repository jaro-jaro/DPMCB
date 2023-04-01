package cz.jaro.dpmcb.data

import kotlinx.serialization.Serializable

@Serializable
data class Nastaveni(
    val dmPodleSystemu: Boolean = true,
    val dm: Boolean = true,
    val autoOnline: Boolean = true,
    val kontrolaAktualizaci: Boolean = true,
    val zachovavatNizkopodlaznost: Boolean = false,
)