package cz.jaro.dpmcb.ui.connection

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import kotlin.time.ExperimentalTime

@Suppress("unused")
@Composable
fun Connection(
    args: Route.Connection,
    navigator: Navigator,
    superNavController: NavHostController,
//    viewModel: ConnectionViewModel = viewModel(args.date),
) {
    AppState.title = "Vyhledávání spojení"
    AppState.selected = DrawerAction.Connection

    LaunchedEffect(Unit) {
//        viewModel.navigator = navigator
    }

//    val state by viewModel.state.collectAsStateWithLifecycle()

    ConnectionScreen(
//        state = state,
//        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ConnectionScreen(
//    state: ConnectionState,
//    onEvent: (ConnectionEvent) -> Unit,
) {

}