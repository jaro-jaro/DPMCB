package cz.jaro.dpmcb

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.DestinationsNavHost
import cz.jaro.dpmcb.ui.NavGraphs

@Composable
fun Navigation(
    navController: NavHostController
) {
    DestinationsNavHost(
        navController = navController,
        navGraph = NavGraphs.root
    )
}
