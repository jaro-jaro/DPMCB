package cz.jaro.dpmcb.ui.find_bus

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.isUnknown
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.entities.toRegNum
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.toLastDigits
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.seqName
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

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

    val state = result.mapState { result ->
        FindBusState(
            name = name,
            line = line,
            number = number,
            vehicle = vehicle,
            sequence = sequence,
            result = result,
            date = date,
        )
    }

    private fun confirm(busName: BusName) = navigator.navigate(
        Route.Bus(
            date = date,
            busName = busName,
        )
    )

    @OptIn(ExperimentalTime::class)
    private fun findSequenceByRegN(rn: RegistrationNumber) {
        launch {
            with(repo) {
                val found = repo.vehicleNumbersOnSequences.value[SystemClock.todayHere()]?.entries
                    .orEmpty()
                    .filter { it.value == rn }
                    .map { (seq) ->
                        seq to seq.seqName()
                    }

                onFoundSequences(found, rn.toString(), SequenceSource.Vehicle)
            }
        }
    }

    private fun findLine(sl: ShortLine, callback: (LongLine?) -> Unit) {
        launch {
            with(repo) {
                callback(sl.findLongLine())
            }
        }
    }

    private fun findSequences(seq: String) {
        launch {
            val found = repo.findSequences(seq)

            onFoundSequences(found, seq, SequenceSource.Search)
        }
    }

    private fun onFoundSequences(
        found: List<Pair<SequenceCode, String>>,
        input: String,
        source: SequenceSource,
    ) {
        if (found.isEmpty()) result.value = FindBusResult.SequenceNotFound(input, source)
        else if (found.size == 1) onEvent(FindBusEvent.SelectSequence(found.single().first))
        else result.value = FindBusResult.MoreSequencesFound(input, source, found)
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
            findSequenceByRegN(vehicle.text.toRegNum())
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