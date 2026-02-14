package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.Logger
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.launch
import cz.jaro.dpmcb.data.helperclasses.sorted
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import cz.jaro.dpmcb.ui.common.ChooserResult
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.navigateBackWithResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class ChooserViewModel(
    private val repo: SpojeRepository,
    private val params: Parameters,
) : ViewModel(), Logger by repo {

    data class Parameters(
        val type: ChooserType,
        val lineNumber: LongLine = LongLine.invalid,
        val stop: StopName?,
        val date: LocalDate,
    )

    data class Item(
        val textValue: String,
        val item: Any?,
    )

    lateinit var navigator: Navigator

    private val originalList = suspend {
        when (params.type) {
            ChooserType.Lines,
            ChooserType.ReturnLine,
                -> repo.lineNumbers(params.date).sortedBy { it.toShortLine() }.map { Item(it.toShortLine().toString(), it) }

            ChooserType.Stops,
            ChooserType.ReturnStop1,
            ChooserType.ReturnStop2,
            ChooserType.ReturnStop,
            ChooserType.ReturnStopVia,
                -> repo.stopNames(params.date)
                .groupBy({ it.stopName }, { it.fareZone }).entries
                .sortedBy { it.key.fullName.normalize() }
                .map { Item("${it.key.printName} (${it.value.filterNotNull().sorted().joinToString()})", it.key) }

            ChooserType.LineStops,
                -> repo.stopNamesOfLine(params.lineNumber, params.date).distinct().map { Item(it.printName, it) }

            ChooserType.Platforms,
                -> repo.platformsAndDirections(params.lineNumber, params.stop!!, params.date).await()
                .map { (platform, directions) ->
                    Item("${platform.first} (-> ${directions.joinToString(" / ")})", platform to directions)
                }

            ChooserType.ReturnPlatform,
                -> repo.platformsOfStop(params.stop!!, params.date).await().map { Item(it, it) }
        }
    }.asFlow()

    private val search = TextFieldState()
    private val searchText = snapshotFlow { search.text }

    private val triggered = MutableStateFlow(false)

    init {
        launch {
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
                var normalisedItem = item.textValue.lowercase().removeNSM().split(" ")
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
                item.textValue
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
        result: Item,
    ) = when (params.type) {
        ChooserType.Stops -> navigator.navigate(
            Route.Departures(
                stop = result.item as StopName,
                date = params.date,
            )
        )

        ChooserType.Lines -> navigator.navigate(
            Route.Chooser(
                lineNumber = result.item as LongLine,
                type = ChooserType.LineStops,
                date = params.date,
            )
        )

        ChooserType.LineStops -> {
            launch(Dispatchers.IO) {
                val stop = result.item as StopName
                repo.platformsAndDirections(params.lineNumber, stop, params.date).await().let { stops ->
                    withContext(Dispatchers.Main) {
                        navigator.navigate(
                            if (stops.size == 1)
                                Route.Timetable(
                                    lineNumber = params.lineNumber,
                                    stop = stop,
                                    platform = stops.entries.single().key.first,
                                    date = params.date,
                                    direction = stops.entries.single().key.second,
                                )
                            else
                                Route.Chooser(
                                    lineNumber = params.lineNumber,
                                    stop = stop,
                                    type = ChooserType.Platforms,
                                    date = params.date,
                                )
                        )
                    }
                }
            }
            Unit
        }

        ChooserType.Platforms -> {
            launch {
                val all = repo.platformsAndDirections(params.lineNumber, params.stop!!, params.date).await().toList()
                val (platform, direction) = all[originalList.first().indexOf(result)].first
                navigator.navigate(
                    Route.Timetable(
                        lineNumber = params.lineNumber,
                        stop = params.stop,
                        platform = platform,
                        date = params.date,
                        direction = direction,
                    )
                )
            }
            Unit
        }

        ChooserType.ReturnStop1,
        ChooserType.ReturnStop2,
        ChooserType.ReturnStop,
        ChooserType.ReturnStopVia,
            -> navigator.navigateBackWithResult(ChooserResult(result.item as StopName, params.type))
        ChooserType.ReturnLine,
            -> navigator.navigateBackWithResult(ChooserResult(result.item as LongLine, params.type))
        ChooserType.ReturnPlatform,
            -> navigator.navigateBackWithResult(ChooserResult(result.item as Platform, params.type))
    }

    fun ChooserState(
        list: Collection<Item> = emptyList(),
    ) = ChooserState(
        type = params.type,
        search = search,
        info = when (params.type) {
            ChooserType.LineStops -> "${params.lineNumber.toShortLine()}: ?"
            ChooserType.Platforms -> "${params.lineNumber.toShortLine()}: ${params.stop}"
            else -> ""
        },
        list = list.toList(),
        date = params.date,
    )

    val state =
        list.map(::ChooserState).stateInViewModel(SharingStarted.WhileSubscribed(5_000), ChooserState())
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
