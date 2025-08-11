package cz.jaro.dpmcb.ui.chooser

import kotlinx.datetime.LocalDate

sealed interface ChooserEvent {
    data class ChangeDate(val date: LocalDate) : ChooserEvent
    data object Confirm : ChooserEvent
    data class ClickedOnListItem(val item: String) : ChooserEvent
}