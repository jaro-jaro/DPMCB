package cz.jaro.dpmcb.ui.main

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.ui.graphics.vector.ImageVector
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.SettingsActivity
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.ui.chooser.ChooserType
import kotlinx.datetime.LocalDate
import kotlin.reflect.KClass


enum class DrawerAction(
    @StringRes val label: Int,
    val icon: ImageVector,
    val multiselect: Boolean,
    val route: ((LocalDate) -> Route)? = null,
    val activity: KClass<out Activity>? = null,
) {
    /*Connection(
        R.string.vyhledat_spojeni,
        Icons.Default.Timeline,
        true,
        route = { Route.Spojeni(it) }
    ),*/
    Favourites(
        label = R.string.favourites,
        icon = Icons.Default.Star,
        multiselect = true,
        route = { Route.Favourites },
    ),
    Departures(
        label = R.string.departures,
        icon = Icons.Default.DepartureBoard,
        multiselect = true,
        route = {
            Route.Chooser(
                type = ChooserType.Stops,
                lineNumber = ShortLine.invalid,
                stop = null,
                date = it,
            )
        }
    ),
    NowRunning(
        label = R.string.now_running,
        icon = Icons.Default.FastForward,
        multiselect = true,
        route = { Route.NowRunning() }
    ),
    Timetable(
        label = R.string.timetable,
        icon = Icons.Default.FormatListNumbered,
        multiselect = true,
        route = {
            Route.Chooser(
                type = ChooserType.Lines,
                lineNumber = ShortLine.invalid,
                stop = null,
                date = it,
            )
        }
    ),
    FindBus(
        label = R.string.find_bus_by_id,
        icon = Icons.Default.Search,
        multiselect = true,
        route = { Route.FindBus(it) }
    ),
    TransportCard(
        label = R.string.card,
        icon = Icons.Default.QrCode,
        multiselect = true,
        route = { Route.Card(it) }
    ),
    LinesMap(
        label = R.string.lines_map,
        icon = Icons.Default.Map,
        multiselect = true,
        route = { Route.Map(it) }
    ),
    Settings(
        label = R.string.settings,
        icon = Icons.Default.Settings,
        multiselect = false,
        activity = SettingsActivity::class
    ),
    Feedback(
        label = R.string.feedback,
        icon = Icons.Default.Stars,
        multiselect = false,
    ),
}