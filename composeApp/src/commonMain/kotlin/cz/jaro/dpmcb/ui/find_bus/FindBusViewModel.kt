package cz.jaro.dpmcb.ui.find_bus

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.toRegNum
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.entities.withPart
import cz.jaro.dpmcb.data.entities.withoutPart
import cz.jaro.dpmcb.data.entities.withoutType
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.mapState
import cz.jaro.dpmcb.data.helperclasses.toLastDigits
import cz.jaro.dpmcb.data.pushVehicles
import cz.jaro.dpmcb.data.recordException
import cz.jaro.dpmcb.data.seqName
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val sequenceNumber = TextFieldState()
    private val sequenceLine = TextFieldState()

    val state = result.mapState { result ->
        FindBusState(
            name = name,
            line = line,
            number = number,
            vehicle = vehicle,
            sequenceNumber = sequenceNumber,
            sequenceLine = sequenceLine,
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
                val found = vehicleNumbersOnSequences.value[date]?.entries
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

    private fun findSequences(number: String, line: String, allowImmediateSubmit: Boolean = true) {
        launch {
            require(line.isNotEmpty() && line.length <= 2 && number.length <= 2)
            val found = repo.findSequences(line, number)

            onFoundSequences(found, "$number/$line", SequenceSource.Search, allowImmediateSubmit)
        }
    }

    private fun onFoundSequences(
        found: List<Pair<SequenceCode, String>>,
        input: String,
        source: SequenceSource,
        allowImmediateSubmit: Boolean = true,
    ) {
        if (found.isEmpty()) result.value = FindBusResult.SequenceNotFound(input, source)
        else {
            result.value = FindBusResult.SequencesFound(input, source, found)
            if (allowImmediateSubmit) onEvent(FindBusEvent.SelectSequence(found.first().first))
        }
    }

    init {
        combine(
            snapshotFlow { sequenceNumber.text }, snapshotFlow { sequenceLine.text }
        ) { number, line ->
            if (line.isNotEmpty())
                findSequences(number.toString(), line.toString(), allowImmediateSubmit = false)
        }.launch()
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

        FindBusEvent.ConfirmSequence -> findSequences(sequenceNumber.text.toString(), sequenceLine.text.toString())
        FindBusEvent.Confirm ->
            when {
                line.text.isNotEmpty() && number.text.isNotEmpty() -> onEvent(FindBusEvent.ConfirmLine)
                sequenceLine.text.isNotEmpty() -> onEvent(FindBusEvent.ConfirmSequence)
                vehicle.text.isNotEmpty() -> onEvent(FindBusEvent.ConfirmVehicle)
                else -> Unit
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

        is FindBusEvent.DownloadVehicles -> {
            launch {
                val doc = getDoc("https://seznam-autobusu.cz/vypravenost/mhd-cb/vypis?datum=${date}") {
                    e.onFail()
                    return@launch
                }
                val otherPages = doc
                    .body()
                    .select("#snippet--table > div > div.visual-paginator-control > span.description")
                    .first()
                    ?.text()
                    ?.substringAfterLast(' ')
                    ?.toInt()
                    ?.div(50)
                    ?: return@launch

                val otherDocs = List(otherPages) { i ->
                    getDoc("https://seznam-autobusu.cz/vypravenost/mhd-cb/vypis?datum=${date}&strana=${i + 2}") {
                        e.onFail()
                        return@launch
                    }
                }
                val data = (listOf(doc) + otherDocs).flatMap {
                    it
                        .body()
                        .select("#snippet--table > div > table > tbody")
                        .single()
                        .children()
                }
                    .filter { !it.hasClass("table-header") }
                    .map {
                        Triple(
                            it.getElementsByClass("car").single().text().toRegNum(),
                            SequenceCode(
                                "${
                                    it.getElementsByClass("order-on-line").single().text()
                                }/${
                                    it.getElementsByClass("route").single().text()
                                }"
                            ),
                            it.getElementsByClass("note").single().text(),
                        )
                    }

                val todayRunning = repo.todayRunningSequences(date).await().keys

                val downloaded = data.flatMap { (vehicle, sequence, note) ->
                    val withPart = when {
                        note.contains("noc") -> sequence.withPart(1)
                        note.contains("ran") -> sequence.withPart(1)
                        note.contains("odpo") -> sequence.withPart(2)
                        else -> sequence
                    }
                    val foundSequences = todayRunning.find {
                        it.withoutType() == withPart
                    }?.let(::listOf)
                        ?: if (data.count { it.second == sequence } == 1) todayRunning.filter {
                            it.withoutType().withoutPart() == sequence
                        } // Stejný bus na ranní i odpolední části
                        else emptyList()

                    foundSequences.map { it to vehicle }
                }.toMap()
                repo.pushVehicles(date, downloaded, reliable = false)
                e.onSuccess()
            }
            Unit
        }
    }

    suspend inline fun getDoc(
        url: String,
        onFail: () -> Nothing,
    ) = try {
        Ksoup.parseGetRequest(url)
    } catch (ex: Exception) {
        ex.printStackTrace()
        recordException(ex)
        onFail()
    }
}