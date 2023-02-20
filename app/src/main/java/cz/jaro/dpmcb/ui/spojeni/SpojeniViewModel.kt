package cz.jaro.dpmcb.ui.spojeni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import cz.jaro.dpmcb.ui.destinations.VysledkySpojeniScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class SpojeniViewModel : ViewModel() {

    private val _historie = MutableStateFlow(repo.historieVyhledavani.toList())
    val historie = _historie.asStateFlow()

    private val _nastaveni =
        MutableStateFlow(repo.historieVyhledavani.firstOrNull()?.let { NastaveniVyhledavani(it.first, it.second) } ?: NastaveniVyhledavani("", ""))
    val nastaveni = _nastaveni.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun vybratZastavku(start: Boolean) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.Navigovat(
                    VybiratorScreenDestination(
                        typ = if (start) TypAdapteru.ZASTAVKY_ZPET_1 else TypAdapteru.ZASTAVKA_ZPET_2,
                    )
                )
            )
        }
    }

    fun prohazujeZastavky() {
        _nastaveni.update {
            it.copy(start = it.cil, cil = it.start)
        }
    }

    fun vyhledat() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.pridatDoHistorieVyhledavani(
                start = nastaveni.value.start,
                cil = nastaveni.value.cil
            )
            _historie.value = repo.historieVyhledavani.toList()

            _uiEvent.send(UiEvent.Navigovat(VysledkySpojeniScreenDestination(nastaveni.value)))
        }
    }

    fun vybralZastavku(start: Boolean, zastavka: String) {
        _nastaveni.update {
            if (start)
                it.copy(start = zastavka)
            else
                it.copy(cil = zastavka)
        }
    }

    fun upravitNastaveni(function: (NastaveniVyhledavani) -> NastaveniVyhledavani) {
        _nastaveni.update(function)
    }

    fun vyhledatZHistorie(i: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val start = historie.value[i].first
            val cil = historie.value[i].second

            repo.pridatDoHistorieVyhledavani(start = start, cil = cil)
            _historie.value = repo.historieVyhledavani.toList()

            _uiEvent.send(UiEvent.Navigovat(VysledkySpojeniScreenDestination(nastaveni.value.copy(start = start, cil = cil))))

            _nastaveni.update {
                it.copy(start = start, cil = cil)
            }
        }
    }
}