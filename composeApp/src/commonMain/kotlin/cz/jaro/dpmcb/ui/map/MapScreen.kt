package cz.jaro.dpmcb.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import org.koin.compose.koinInject

@Suppress("unused")
@Composable
fun Map(
    args: Route.Map,
    navigator: Navigator,
    superNavController: NavHostController,
) {
    AppState.title = "Schéma linek"
    AppState.selected = DrawerAction.LinesMap

    MapScreen(
        diagramData = koinInject<DiagramManager>().imageData
    )
}

@Composable
fun MapScreen(
    diagramData: Any?
) = Box(
    Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)),
    contentAlignment = Alignment.Center
) {
    ShowDiagram(diagramData)
}