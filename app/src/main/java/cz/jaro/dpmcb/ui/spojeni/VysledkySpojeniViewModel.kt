package cz.jaro.dpmcb.ui.spojeni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.Spojeni
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VysledkySpojeniViewModel(
    nastaveniVyhledavani: NastaveniVyhledavani,
) : ViewModel() {

    private val _vysledky = MutableStateFlow(emptyList<Spojeni>())
    val vysledky = _vysledky.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            VyhledavacSpojeni().vyhledatSpojeni(nastaveniVyhledavani).collect {
                _vysledky.value += listOf(it)
            }
        }
    }
}
