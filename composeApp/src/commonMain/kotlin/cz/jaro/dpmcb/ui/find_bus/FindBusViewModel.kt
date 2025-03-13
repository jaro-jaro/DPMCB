package cz.jaro.dpmcb.ui.find_bus

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.isUnknown
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.entities.toRegNum
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.toLastDigits
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.seconds

class FindBusViewModel(
    private val repo: SpojeRepository,
    private val onlineRepository: OnlineRepository,
    private val date: LocalDate,
) : ViewModel() {
    lateinit var navigator: Navigator

    private val result = MutableStateFlow<FindBusResult>(FindBusResult.None)

    private val name = TextFieldState()
    private val line = TextFieldState()
    private val number = TextFieldState()
    private val vehicle = TextFieldState()
    private val sequence = TextFieldState()

    private val busName get() = BusName(name.text.toString()).takeIf(BusName::isValid)

    val state = result.map { result ->
        FindBusState(
            name = name,
            line = line,
            number = number,
            vehicle = vehicle,
            sequence = sequence,
            result = result,
            date = date,
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5.seconds), FindBusState(
            name = name,
            line = line,
            number = number,
            vehicle = vehicle,
            sequence = sequence,
            result = FindBusResult.None,
            date = date,
        )
    )

    private fun confirm(busName: BusName) = navigator.navigate(
        Route.Bus(
            date = date,
            busName = busName,
        )
    )

    private fun findBusByRegN(rn: RegistrationNumber, callback: (BusName?) -> Unit) {
        viewModelScope.launch {
            callback(onlineRepository.nowRunningBuses().first().find {
                it.vehicle == rn
            }?.name)
        }
    }

    private fun findLine(sl: ShortLine, callback: (LongLine?) -> Unit) {
        viewModelScope.launch {
            with(repo) {
                callback(sl.findLongLine())
            }
        }
    }

    private fun findSequences(seq: String) {
        viewModelScope.launch {
            val found = repo.findSequences(seq)

            if (found.isEmpty()) result.value = FindBusResult.SequenceNotFound(seq)
            else if (found.size == 1) onEvent(FindBusEvent.SelectSequence(found.single().first))
            else result.value = FindBusResult.MoreSequencesFound(seq, found)
        }
    }

    fun onEvent(e: FindBusEvent): Unit = when (e) {
        FindBusEvent.ConfirmLine -> {
            if (line.text.length == 6)
                confirm(line.text / number.text)
            else if (line.text.length > 3)
                result.value = FindBusResult.LineNotFound(line.text.toString())
            else
                findLine(line.text.toLastDigits(3).toShortLine()) {
                    if (it == null)
                        result.value = FindBusResult.LineNotFound(line.text.toString())
                    else
                        confirm(it / number.text)
                }
        }

        FindBusEvent.ConfirmVehicle -> {
            if (!repo.isOnline())
                result.value = FindBusResult.Offline
            else
                findBusByRegN(vehicle.text.toRegNum()) {
                    if (it == null)
                        result.value = FindBusResult.VehicleNotFound(vehicle.text.toString())
                    else
                        confirm(it)
                }
        }

        FindBusEvent.ConfirmName -> when {
            busName == null -> result.value = FindBusResult.InvalidBusName(name.text.toString())
            busName!!.isUnknown() -> findLine(busName!!.shortLine()) {
                if (it != null)
                    confirm(it / busName!!.bus())
                else
                    result.value = FindBusResult.LineNotFound(busName!!.line().toString())
            }

            else -> confirm(busName!!)
        }

        FindBusEvent.ConfirmSequence -> findSequences(sequence.text.toString())
        FindBusEvent.Confirm ->
            when {
                line.text.isNotEmpty() && number.text.isNotEmpty() -> onEvent(FindBusEvent.ConfirmLine)
                vehicle.text.isNotEmpty() -> onEvent(FindBusEvent.ConfirmVehicle)
                sequence.text.isNotEmpty() -> onEvent(FindBusEvent.ConfirmSequence)
                else -> onEvent(FindBusEvent.ConfirmName)
            }

        is FindBusEvent.SelectSequence -> navigator.navigate(
            Route.Sequence(
                date = date,
                sequence = e.seq,
            )
        )

        is FindBusEvent.ChangeDate -> navigator.navigate(
            Route.FindBus(
                date = e.date,
            )
        )
    }
}

private fun BusName.isValid() = value.contains("/")
        && (value.substringBefore("/").length == 6
        || value.substringBefore("/").length <= 3)
        && value.substringBefore("/").isDigitsOnly()
        && value.substringAfter("/").isNotEmpty()
        && value.substringAfter("/").isDigitsOnly()

fun CharSequence.isDigitsOnly() = Regex("^[0-9]*$").matches(this)