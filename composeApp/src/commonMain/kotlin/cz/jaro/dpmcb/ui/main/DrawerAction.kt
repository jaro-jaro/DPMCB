package cz.jaro.dpmcb.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import cz.jaro.dpmcb.ui.card.supportsCard
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.map.supportsLineDiagram
import kotlinx.datetime.LocalDate

enum class DrawerAction(
    val label: String,
    val icon: ImageVector,
    val multiselect: Boolean,
    val hide: Boolean = false,
    val route: ((LocalDate) -> Route)? = null,
    val hasDivider: Boolean = false,
) {
    Connection(
        label = "Vyhledat spojení",
        icon = Icons.Default.Timeline,
        multiselect = true,
        route = { Route.ConnectionSearch(it) },
    ),
    Favourites(
        label = "Oblíbené spoje",
        icon = Icons.Default.Star,
        multiselect = true,
        route = { Route.Favourites },
    ),
    Departures(
        label = "Odjezdy",
        icon = Icons.Default.DepartureBoard,
        multiselect = true,
        route = {
            Route.Chooser(
                type = ChooserType.Stops,
                date = it,
            )
        },
    ),
    NowRunning(
        label = "Právě jedoucí",
        icon = Icons.Default.FastForward,
        multiselect = true,
        route = { Route.NowRunning() },
    ),
    Timetable(
        label = "Jízdní řády",
        icon = Icons.Default.FormatListNumbered,
        multiselect = true,
        route = {
            Route.Chooser(
                type = ChooserType.Lines,
                date = it,
            )
        },
    ),
    FindBus(
        label = "Najít spoj",
        icon = Icons.Default.Search,
        multiselect = true,
        route = { Route.FindBus(it) },
        hasDivider = true,
    ),
    TransportCard(
        label = "Průkazka",
        icon = Icons.Default.QrCode,
        multiselect = true,
        hide = !supportsCard(),
        route = { Route.Card },
    ),
    LinesMap(
        label = "Schéma linek",
        icon = Icons.Default.Map,
        hide = !supportsLineDiagram(),
        multiselect = true,
        route = { Route.Map(it) },
        hasDivider = true,
    ),
    Settings(
        label = "Nastavení",
        icon = Icons.Default.Settings,
        multiselect = false,
        route = { Route.Settings },
    ),
}