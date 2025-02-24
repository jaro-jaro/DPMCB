package cz.jaro.dpmcb.ui.card

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

interface CardManager {
    fun loadCard()

    fun removeCard()

    val card: StateFlow<ImageBitmap?>
}