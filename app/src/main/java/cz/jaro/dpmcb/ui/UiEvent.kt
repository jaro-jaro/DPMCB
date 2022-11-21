package cz.jaro.dpmcb.ui

import com.ramcosta.composedestinations.spec.Direction

sealed class UiEvent {
    data class Zpet(val kam: Direction? = null, val inclusive: Boolean = false): UiEvent()
    data class Navigovat(val kam: Direction): UiEvent()
    data class Zkopirovat(val text: String) : UiEvent()
}
