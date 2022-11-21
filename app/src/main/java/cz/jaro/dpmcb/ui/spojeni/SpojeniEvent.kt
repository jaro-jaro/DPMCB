package cz.jaro.dpmcb.ui.spojeni

sealed class SpojeniEvent {
    data class ChceVybratZastavku(val start: Boolean) : SpojeniEvent()
    data class VybralZastavku(val zastavka: String, val start: Boolean) : SpojeniEvent()
    object Vyhledat : SpojeniEvent()
    object ProhazujeZastavky : SpojeniEvent()
}
