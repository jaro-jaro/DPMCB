package cz.jaro.dpmcb.ui.spojeni

import cz.jaro.dpmcb.data.helperclasses.Cas
import kotlinx.serialization.Serializable

@Serializable
data class NastaveniVyhledavani(
    val start: String,
    val cil: String,
    val jenNizkopodlazni: Boolean = false,
    val jenPrima: Boolean = false,
    val cas: Cas = Cas.ted,
)
