package cz.jaro.dpmcb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.loading.Loading
import cz.jaro.dpmcb.ui.main.Main
import cz.jaro.dpmcb.ui.main.SuperRoute
import cz.jaro.dpmcb.ui.theme.DPMCBTheme

@Composable
fun SuperMainContent(
    repo: SpojeRepository,
    link: String?,
    reset: suspend () -> Unit,
) {
    val settings by repo.settings.collectAsStateWithLifecycle()
    DPMCBTheme(settings) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = SuperRoute.Loading(link = link),
        ) {
            composable<SuperRoute.Main> {
//                Text("Ahojda")
                Main(navController, it.toRoute())
            }
            composable<SuperRoute.Loading> {
//                Text("Ahoooooj")
                Loading(navController, it.toRoute(), reset)
            }
        }
    }
}