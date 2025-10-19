package cz.jaro.dpmcb.ui.find_bus

import cz.jaro.dpmcb.data.entities.SequenceCode
import kotlinx.datetime.LocalDate

sealed interface FindBusEvent {
    data class DownloadVehicles(val onSuccess: () -> Unit, val onFail: () -> Unit) : FindBusEvent
    data object Confirm : FindBusEvent
    data object ConfirmLine : FindBusEvent
    data object ConfirmVehicle : FindBusEvent
    data object ConfirmSequence : FindBusEvent
    data class SelectSequence(val seq: SequenceCode) : FindBusEvent
    data class ChangeDate(val date: LocalDate) : FindBusEvent
}
