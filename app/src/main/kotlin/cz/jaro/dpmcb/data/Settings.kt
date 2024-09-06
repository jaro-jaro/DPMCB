package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.ui.theme.Theme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Nastaveni")
data class Settings(
    @SerialName("dmPodleSystemu") val dmAsSystem: Boolean = true,
    val dm: Boolean = true,
    @SerialName("dynamickeBarvy") val dynamicColors: Boolean = true,
    @SerialName("tema") val theme: Theme = Theme.Default,
    val autoOnline: Boolean = true,
    @SerialName("kontrolaAktualizaci") val checkForUpdates: Boolean = true,
//    val special: Boolean = false,
)