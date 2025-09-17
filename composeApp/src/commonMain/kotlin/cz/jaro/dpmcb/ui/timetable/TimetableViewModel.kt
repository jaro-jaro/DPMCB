package cz.jaro.dpmcb.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

class TimetableViewModel(
    private val repo: SpojeRepository,
    private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val lineNumber: ShortLine,
        val stop: String,
        val direction: Direction,
        val date: LocalDate,
    )

    lateinit var navigator: Navigator

    private val list = suspend {
        repo.timetable(params.lineNumber, params.stop, params.direction, params.date).sortedBy { it.departure }
    }.asFlow()

    val endStops = viewModelScope.async {
        repo.endStopNames(params.lineNumber, params.stop, params.date)
            .getValue(params.direction)
            .replace("\n", " / ")
    }

    val state = list.map { list ->
        TimetableState.Success(
            data = list,
            date = params.date,
            lineNumber = params.lineNumber,
            stop = params.stop,
            endStops = endStops.await(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimetableState.Loading(
            date = params.date,
            lineNumber = params.lineNumber,
            stop = params.stop,
        )
    )

    fun onEvent(e: TimetableEvent) = when (e) {
        is TimetableEvent.ChangeDate -> navigator.navigate(
            Route.Timetable(
                lineNumber = params.lineNumber,
                stop = params.stop,
                direction = params.direction,
                date = e.date,
            )
        )

        is TimetableEvent.GoToBus -> navigator.navigate(
            Route.Bus(
                date = params.date,
                busName = e.bus,
            )
        )
    }
}
