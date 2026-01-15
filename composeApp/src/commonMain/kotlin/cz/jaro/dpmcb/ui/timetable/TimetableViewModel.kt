package cz.jaro.dpmcb.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.async
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TimetableViewModel(
    private val repo: SpojeRepository,
    private val params: Route.Timetable,
) : ViewModel() {

    lateinit var navigator: Navigator

    private val list = suspend {
        repo.timetable(params.lineNumber, params.stop, params.platform, params.direction, params.date).sortedBy { it.departure }
    }.asFlow()

    val endStops = async {
        repo.platformsAndDirections(params.lineNumber, params.stop, params.date).await()
            .getValue(params.platform to params.direction)
            .joinToString(" / ")
    }

    val state = list.map { list ->
        TimetableState.Success(
            data = list,
            date = params.date,
            lineNumber = params.lineNumber,
            stop = params.stop,
            platform = params.platform,
            endStops = endStops.await(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimetableState.Loading(
            date = params.date,
            lineNumber = params.lineNumber,
            stop = params.stop,
            platform = params.platform,
        )
    )

    fun onEvent(e: TimetableEvent) = when (e) {
        is TimetableEvent.ChangeDate -> navigator.navigate(
            Route.Timetable(
                lineNumber = params.lineNumber,
                stop = params.stop,
                platform = params.platform,
                date = e.date,
                direction = Direction.POSITIVE, // TODO
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
