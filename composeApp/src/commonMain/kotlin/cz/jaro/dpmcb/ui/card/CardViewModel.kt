package cz.jaro.dpmcb.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

class CardViewModel(
    private val cardManager: CardManager,
) : ViewModel() {

    val hasCard = cardManager.card
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    fun addCard() = cardManager.loadCard()

    val card = cardManager.card
}
