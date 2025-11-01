package cz.jaro.dpmcb.ui.card

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlin.time.Duration.Companion.seconds

class CardViewModel(
    private val cardManager: CardManager,
) : ViewModel() {

    val hasCard = cardManager.card
        .stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), null)

    fun addCard() = cardManager.loadCard()

    val card = cardManager.card
}
