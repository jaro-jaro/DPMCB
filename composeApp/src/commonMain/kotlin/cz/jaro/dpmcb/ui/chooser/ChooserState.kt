package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.text.input.TextFieldState
import kotlinx.datetime.LocalDate

data class ChooserState(
    val type: ChooserType,
    val search: TextFieldState,
    val info: String,
    val list: List<String>,
    val date: LocalDate,
)