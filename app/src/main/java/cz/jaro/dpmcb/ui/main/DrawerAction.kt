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
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.SettingsActivity
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.ui.chooser.ChooserType
import kotlin.reflect.KClass


enum class DrawerAction(
    @StringRes val label: Int,
    val icon: ImageVector,
    val multiselect: Boolean,
    val onClick: (
        navigate: NavigateFunction,
        close: () -> Unit,
        startActivity: (KClass<out Activity>) -> Unit,
    ) -> Unit,
) {
    /*Connection(
        R.string.vyhledat_spojeni,
        Icons.Default.Timeline,
        true,
        onClick = { navigate, zavrit, _ ->
            navigate(
                Route.SpojeniScreen)
            )
            zavrit()
        }
    ),*/
    Favourites(
        label = R.string.favourites,
        icon = Icons.Default.Star,
        multiselect = true,
        onClick = { navigate, close, _ ->

            navigate(
                Route.Favourites
            )
            close()
        }
    ),
    Departures(
        label = R.string.departures,
        icon = Icons.Default.DepartureBoard,
        multiselect = true,
        onClick = { navigate, close, _ ->

            navigate(
                Route.Chooser(
                    type = ChooserType.Stops,
                    lineNumber = ShortLine.invalid,
                    stop = null
                )
            )
            close()
        }
    ),
    NowRunning(
        label = R.string.now_running,
        icon = Icons.Default.FastForward,
        multiselect = true,
        onClick = { navigate, close, _ ->

            navigate(
                Route.NowRunning()
            )
            close()
        }
    ),
    Timetable(
        R.string.timetable,
        Icons.Default.FormatListNumbered,
        true,
        onClick = { navigate, close, _ ->

            navigate(
                Route.Chooser(
                    type = ChooserType.Lines,
                    lineNumber = ShortLine.invalid,
                    stop = null,
                )
            )
            close()
        }
    ),
    FindBus(
        R.string.find_bus_by_id,
        Icons.Default.Search,
        true,
        onClick = { _, _, _ -> }
    ),
    TransportCard(
        R.string.card,
        icon = Icons.Default.QrCode,
        multiselect = true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                Route.Card
            )
            zavrit()
        }
    ),
    LinesMap(
        R.string.lines_map,
        Icons.Default.Map,
        true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                Route.Map
            )
            zavrit()
        }
    ),
    Date(
        R.string.day_type,
        Icons.Default.Today,
        false,
        onClick = { _, _, _ -> }
    ),
    Settings(
        R.string.settings,
        Icons.Default.Settings,
        false,
        onClick = { _, _, startActivity ->
            startActivity(SettingsActivity::class as KClass<out Activity>)
        }
    ),
    Feedback(
        R.string.feedback,
        Icons.Default.Stars,
        false,
        onClick = { _, _, _ -> }
    ),
/*    Exit(
        R.string.exit_app,
        Icons.Default.PowerSettingsNew,
        false,
        onClick = { _, _, _ -> }
    )*/
}