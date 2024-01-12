package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.ui.theme.Theme
import kotlinx.serialization.Serializable

@Serializable
data class Nastaveni(
    val dmPodleSystemu: Boolean = true,
    val dm: Boolean = true,
    val dynamickeBarvy: Boolean = true,
    val tema: Theme = Theme.Yellow,
    val autoOnline: Boolean = true,
    val kontrolaAktualizaci: Boolean = true,
)