package cz.jaro.dpmcb.ui.vybirator

sealed class VybiratorEvent {
    object KliklEnter: VybiratorEvent()
    data class KliklNaSeznam(val vec: String,): VybiratorEvent()
    data class NapsalNeco(val co: String,): VybiratorEvent()
}
