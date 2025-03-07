package cz.jaro.dpmcb

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.bundle.Bundle
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.card.AndroidCardManager
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.loading.AppUpdater
import cz.jaro.dpmcb.ui.loading.Loading
import cz.jaro.dpmcb.ui.main.Main
import cz.jaro.dpmcb.ui.main.SuperRoute
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    val repo by inject<SpojeRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val updater = AppUpdater(this)
        val cardManager: CardManager = AndroidCardManager(this, get())
        loadKoinModules(module(createdAtStart = true) {
            single { updater }
            single { cardManager }
        })

        val uri = intent?.action?.equals(Intent.ACTION_VIEW)?.let { intent?.data }?.run { toString().removePrefix("${scheme}://${host}") }

        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            DPMCBTheme(settings) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = SuperRoute.Loading(link = uri),
                ) {
                    composable<SuperRoute.Main> {
                        Main(navController, it.toRoute())
                    }
                    composable<SuperRoute.Loading> {
                        Loading(navController, it.toRoute())
                    }
                }
            }
        }
    }
}