package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.NavigateBackFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.sorted
import cz.jaro.dpmcb.ui.common.ChooserResult
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.text.Normalizer

@KoinViewModel
class ChooserViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val type: ChooserType,
        val lineNumber: ShortLine = ShortLine.invalid,
        val stop: String?,
        val navigate: NavigateFunction,
        val navigateBack: NavigateBackFunction<ChooserResult>,
        val date: LocalDate,
    )

    private val originalList = suspend {
        when (params.type) {
            ChooserType.Lines,
            ChooserType.ReturnLine,
                -> repo.lineNumbers(params.date).sorted().map { it.toString() }

            ChooserType.Stops,
            ChooserType.ReturnStop1,
            ChooserType.ReturnStop2,
            ChooserType.ReturnStop,
                -> repo.stopNames(params.date).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }

            ChooserType.LineStops,
                -> repo.stopNamesOfLine(params.lineNumber, params.date).distinct()

            ChooserType.NextStop,
                -> repo.nextStopNames(params.lineNumber, params.stop!!, params.date)
        }
    }.asFlow()

    private val search = TextFieldState()
    private val searchText = snapshotFlow { search.text }

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
                if (list.count() == 1) done(list.single())
            }
    }

    fun onEvent(e: ChooserEvent) = when (e) {
        is ChooserEvent.ChangeDate -> params.navigate(
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

    private fun String.removeNSM() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

    private fun done(
        result: String,
    ) = when (params.type) {
        ChooserType.Stops -> params.navigate(
            Route.Departures(
                stop = result,
                date = params.date,
            )
        )

        ChooserType.Lines -> params.navigate(
            Route.Chooser(
                lineNumber = result.toShortLine(),
                stop = null,
                type = ChooserType.LineStops,
                date = params.date,
            )
        )

        ChooserType.LineStops -> {
            viewModelScope.launch(Dispatchers.IO) {
                repo.nextStopNames(params.lineNumber, result, params.date).let { stops: List<String> ->
                    withContext(Dispatchers.Main) {
                        params.navigate(
                            if (stops.size == 1)
                                Route.Timetable(
                                    lineNumber = params.lineNumber,
                                    stop = result,
                                    nextStop = stops.single(),
                                    date = params.date,
                                )
                            else
                                Route.Chooser(
                                    lineNumber = params.lineNumber,
                                    stop = result,
                                    type = ChooserType.NextStop,
                                    date = params.date,
                                )
                        )
                    }
                }
            }
            Unit
        }

        ChooserType.NextStop -> params.navigate(
            Route.Timetable(
                lineNumber = params.lineNumber,
                stop = params.stop!!,
                nextStop = result,
                date = params.date,
            )
        )

        ChooserType.ReturnStop1,
        ChooserType.ReturnStop2,
        ChooserType.ReturnLine,
        ChooserType.ReturnStop,
            -> params.navigateBack(ChooserResult(result, params.type))
    }

    fun ChooserState(
        list: List<String> = emptyList(),
    ) = ChooserState(
        type = params.type,
        search = search,
        info = when (params.type) {
            ChooserType.LineStops -> "${params.lineNumber}: ? -> ?"
            ChooserType.NextStop -> "${params.lineNumber}: ${params.stop} -> ?"
            else -> ""
        },
        list = list,
        date = params.date,
    )

    val state =
        list.map(::ChooserState).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChooserState())
}
