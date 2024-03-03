package cz.jaro.dpmcb.ui.chooser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateBackFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.Result
import cz.jaro.dpmcb.ui.destinations.ChooserDestination
import cz.jaro.dpmcb.ui.destinations.DeparturesDestination
import cz.jaro.dpmcb.ui.destinations.TimetableDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.text.Normalizer
import java.time.LocalDate

@KoinViewModel
class ChooserViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val type: ChooserType,
        val lineNumber: Int = -1,
        val stop: String?,
        val navigate: NavigateFunction,
        val navigateBack: NavigateBackFunction<Result>,
    )

    private val originalList = repo.date.map { datum ->
        when (params.type) {
            ChooserType.Stops -> repo.stopNames(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            ChooserType.Lines -> repo.lineNumbers(datum).sorted().map { it.toString() }
            ChooserType.LineStops -> repo.stopNamesOfLine(params.lineNumber, datum).distinct()
            ChooserType.NextStop -> repo.nextStopNames(params.lineNumber, params.stop!!, datum)
            ChooserType.ReturnStop1 -> repo.stopNames(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            ChooserType.ReturnStop2 -> repo.stopNames(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            ChooserType.ReturnLine -> repo.lineNumbers(datum).sorted().map { it.toString() }
            ChooserType.ReturnStop -> repo.stopNames(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
        }
    }

    private val _search = MutableStateFlow("")
    val search = _search.asStateFlow()

    val list = _search.combine(originalList) { filter, originalList ->
        if (filter.isBlank()) originalList
        else originalList
            .asSequence()
            .map { item ->
                var normalisedItem = item.lowercase().removeNSM().split(" ")
                item to filter.lowercase().removeNSM().split(" ").map { searchedWord ->
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
                if (list.count() == 1) done(list.first(), repo.date.value)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val info = when (params.type) {
        ChooserType.LineStops -> "${params.lineNumber}: ? -> ?"
        ChooserType.NextStop -> "${params.lineNumber}: ${params.stop} -> ?"
        else -> ""
    }

    fun wroteSomething(search: String) {
        _search.value = search.replace("\n", "")
    }

    fun clickedOnListItem(item: String) = done(item, repo.date.value)

    fun confirm() = list.value.firstOrNull()?.let { done(it, repo.date.value) } ?: Unit

    private fun String.removeNSM() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

    private fun done(
        result: String,
        date: LocalDate,
    ) {
//        if (job != null && typ.name.contains("ZPET")) return
        when (params.type) {
            ChooserType.Stops -> params.navigate(
                DeparturesDestination(
                    stop = result,
                )
            )

            ChooserType.Lines -> params.navigate(
                ChooserDestination(
                    lineNumber = result.toInt(),
                    stop = null,
                    type = ChooserType.LineStops
                )
            )

            ChooserType.LineStops -> viewModelScope.launch(Dispatchers.IO) {
                repo.nextStopNames(params.lineNumber, result, date).let { stops: List<String> ->
                    withContext(Dispatchers.Main) {
                        params.navigate(
                            if (stops.size == 1)
                                TimetableDestination(
                                    lineNumber = params.lineNumber,
                                    stop = result,
                                    nextStop = stops.first(),
                                )
                            else
                                ChooserDestination(
                                    lineNumber = params.lineNumber,
                                    stop = result,
                                    type = ChooserType.NextStop
                                )
                        )
                    }
                }
            }

            ChooserType.NextStop -> params.navigate(
                TimetableDestination(
                    lineNumber = params.lineNumber,
                    stop = params.stop!!,
                    nextStop = result,
                )
            )

            ChooserType.ReturnStop1 -> {
                params.navigateBack(Result(result, params.type))
            }

            ChooserType.ReturnStop2 -> {
                params.navigateBack(Result(result, params.type))
            }

            ChooserType.ReturnLine -> {
                params.navigateBack(Result(result, params.type))
            }

            ChooserType.ReturnStop -> {
                params.navigateBack(Result(result, params.type))
            }
        }
    }
}
