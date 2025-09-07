package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.sorted
import cz.jaro.dpmcb.data.helperclasses.stateIn
import cz.jaro.dpmcb.ui.common.ChooserResult
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.navigateBackWithResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class ChooserViewModel(
    private val repo: SpojeRepository,
    private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val type: ChooserType,
        val lineNumber: ShortLine = ShortLine.invalid,
        val stop: String?,
        val date: LocalDate,
    )

    lateinit var navigator: Navigator

    private val originalList = suspend {
        when (params.type) {
            ChooserType.Lines,
            ChooserType.ReturnLine,
                -> repo.lineNumbers(params.date).sorted().map { it.toString() }

            ChooserType.Stops,
            ChooserType.ReturnStop1,
            ChooserType.ReturnStop2,
            ChooserType.ReturnStop,
                -> repo.stopNames(params.date).sortedBy { it.normalize() }

            ChooserType.LineStops,
                -> repo.stopNamesOfLine(params.lineNumber, params.date).distinct()

            ChooserType.EndStop,
                -> repo.endStopNames(params.lineNumber, params.stop!!, params.date).values
        }
    }.asFlow()

    private val search = TextFieldState()
    private val searchText = snapshotFlow { search.text }

    private val triggered = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            searchText.collect {
                triggered.value = false
            }
        }
    }

    val list = searchText.combine(originalList) { filter, originalList ->
        if (filter.isBlank()) originalList
        else originalList
            .asSequence()
            .map { item ->
                var normalisedItem = item.lowercase().removeNSM().split(" ")
                item to filter.toString().lowercase().removeNSM().split(" ").map { searchedWord ->
                    normalisedItem.indexOfFirst { itemWord ->
                        itemWord.startsWith(searchedWord)
                    }.also { i ->
                        if (i != -1) {
                            normalisedItem = normalisedItem.drop(i + 1)
                        }
                    }
                }
            }
            .filter { (_, searchedWordIndexes) ->
                searchedWordIndexes.all { it != -1 }
            }
            .sortedBy { (item, _) ->
                item
            }
            .sortedWith(
                Comparator { (_, aList), (_, bList) ->
                    aList.zip(bList).forEach { (a, b) ->
                        if (a == b) return@forEach
                        return@Comparator a - b
                    }
                    return@Comparator 0
                }
            )
            .map { (item, _) ->
                item
            }
            .toList()
            .also { list ->
                if (list.count() == 1 && !triggered.value) {
                    triggered.value = true
                    done(list.single())
                }
            }
    }

    fun onEvent(e: ChooserEvent) = when (e) {
        is ChooserEvent.ChangeDate -> navigator.navigate(
            Route.Chooser(
                lineNumber = params.lineNumber,
                stop = params.stop,
                type = params.type,
                date = e.date,
            )
        )

        ChooserEvent.Confirm -> state.value.list.firstOrNull()?.let { done(it) } ?: Unit
        is ChooserEvent.ClickedOnListItem -> done(e.item)
    }

    private fun String.removeNSM() = normalize().replace("[ˇ'°]".toRegex(), "")

    private fun done(
        result: String,
    ) = when (params.type) {
        ChooserType.Stops -> navigator.navigate(
            Route.Departures(
                stop = result,
                date = params.date,
            )
        )

        ChooserType.Lines -> navigator.navigate(
            Route.Chooser(
                lineNumber = result.toShortLine(),
                type = ChooserType.LineStops,
                date = params.date,
            )
        )

        ChooserType.LineStops -> {
            viewModelScope.launch(Dispatchers.IO) {
                repo.endStopNames(params.lineNumber, result, params.date).let { stops ->
                    withContext(Dispatchers.Main) {
                        navigator.navigate(
                            if (stops.size == 1)
                                Route.Timetable(
                                    lineNumber = params.lineNumber,
                                    stop = result,
                                    direction = stops.entries.single().key,
                                    date = params.date,
                                )
                            else
                                Route.Chooser(
                                    lineNumber = params.lineNumber,
                                    stop = result,
                                    type = ChooserType.EndStop,
                                    date = params.date,
                                )
                        )
                    }
                }
            }
            Unit
        }

        ChooserType.EndStop -> {
            viewModelScope.launch {
                val direction = repo.endStopNames(params.lineNumber, params.stop!!, params.date)
                    .entries.find { it.value == result }!!.key
                navigator.navigate(
                    Route.Timetable(
                        lineNumber = params.lineNumber,
                        stop = params.stop,
                        direction = direction,
                        date = params.date,
                    )
                )
            }
            Unit
        }

        ChooserType.ReturnStop1,
        ChooserType.ReturnStop2,
        ChooserType.ReturnLine,
        ChooserType.ReturnStop,
            -> navigator.navigateBackWithResult(ChooserResult(result, params.type))
    }

    fun ChooserState(
        list: Collection<String> = emptyList(),
    ) = ChooserState(
        type = params.type,
        search = search,
        info = when (params.type) {
            ChooserType.LineStops -> "${params.lineNumber}: ? -> ?"
            ChooserType.EndStop -> "${params.lineNumber}: ${params.stop} -> ?"
            else -> ""
        },
        list = list.toList(),
        date = params.date,
    )

    val state =
        list.map(::ChooserState).stateIn(SharingStarted.WhileSubscribed(5_000), ChooserState())
}

private fun String.normalize() = this
    .replace("ě", "eˇ")
    .replace("š", "sˇ")
    .replace("č", "cˇ")
    .replace("ř", "rˇ")
    .replace("ž", "zˇ")
    .replace("ý", "y'")
    .replace("á", "a'")
    .replace("í", "i'")
    .replace("é", "e'")
    .replace("ó", "o'")
    .replace("ů", "u°")
    .replace("ú", "u'")
    .replace("ď", "dˇ")
    .replace("ǧ", "gˇ")
    .replace("ň", "nˇ")
    .replace("ť", "tˇ")
