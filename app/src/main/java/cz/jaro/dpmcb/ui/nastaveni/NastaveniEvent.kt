package cz.jaro.dpmcb.ui.nastaveni

import cz.jaro.dpmcb.data.Nastaveni
import cz.jaro.dpmcb.data.helperclasses.MutateLambda

sealed interface NastaveniEvent {
    object NavigateBack : NastaveniEvent
    object AktualizovatAplikaci : NastaveniEvent
    object AktualizovatData : NastaveniEvent
    data class UpravitNastaveni(val upravit: MutateLambda<Nastaveni>) : NastaveniEvent
}
