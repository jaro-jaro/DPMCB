package cz.jaro.dpmcb.ui.find_bus

import androidx.compose.foundation.text.input.TextFieldState
import cz.jaro.dpmcb.data.entities.SequenceCode
import kotlinx.datetime.LocalDate

data class FindBusState(
    val date: LocalDate,
    val line: TextFieldState,
    val number: TextFieldState,
    val vehicle: TextFieldState,
    val name: TextFieldState,
    val sequence: TextFieldState,
    val result: FindBusResult,
)

sealed interface FindBusResult {
    data object None : FindBusResult
    data object Offline : FindBusResult
    data class InvalidBusName(val name: String) : FindBusResult
    data class LineNotFound(val line: String) : FindBusResult
    data class VehicleNotFound(val regN: String) : FindBusResult
    data class SequenceNotFound(val seq: String) : FindBusResult
    data class MoreSequencesFound(val seq: String, val sequences: List<Pair<SequenceCode, String>>) : FindBusResult
}
