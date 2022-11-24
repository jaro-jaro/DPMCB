package cz.jaro.dpmcb.ui.spojeni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class SpojeniViewModel : ViewModel() {

    private val _state = MutableStateFlow(SpojeniState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    fun poslatEvent(event: SpojeniEvent) {
        when (event) {
            is SpojeniEvent.ChceVybratZastavku -> {
                event.start
                viewModelScope.launch {
                    _uiEvent.send(UiEvent.Navigovat(
                        VybiratorScreenDestination(
                            typ = if (event.start) TypAdapteru.PRVNI_ZASTAVKA else TypAdapteru.DRUHA_ZASTAVKA,
                        )
                    ))
                }
            }
            is SpojeniEvent.ProhazujeZastavky -> {
                _state.update {
                    it.copy(start = it.cil, cil = it.start)
                }
            }
            is SpojeniEvent.Vyhledat -> {
                viewModelScope.launch(Dispatchers.IO) {
                    VyhledavacSpojeni.vyhledatSpojeni(
                        start = state.value.start,
                        cil = state.value.cil
                    ).also {
                        _uiEvent.send(UiEvent.Zkopirovat(it.toString()))
                    }
                }
            }

            is SpojeniEvent.VybralZastavku -> {
                _state.update {
                    if (event.start)
                        it.copy(start = event.zastavka)
                    else
                        it.copy(cil = event.zastavka)
                }
            }

            is SpojeniEvent.ZmenitNizkopodlaznost -> {
                _state.update {
                    it.copy(nizkopodlaznost = !it.nizkopodlaznost)
                }
            }
        }
    }

    data class SpojeniState(
        val start: String = "",
        val cil: String = "",
        val cas: Cas = Cas.ted,
        val nizkopodlaznost: Boolean = false,
    )
}
