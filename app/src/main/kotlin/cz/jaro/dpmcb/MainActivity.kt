package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.loading.Loading
import cz.jaro.dpmcb.ui.main.Main
import cz.jaro.dpmcb.ui.main.SuperRoute.Loading
import cz.jaro.dpmcb.ui.main.SuperRoute.Main
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    val repo by inject<SpojeRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val uri = intent?.action?.equals(Intent.ACTION_VIEW)?.let { intent?.data }?.run { toString().removePrefix("${scheme}://${host}") }

        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            DPMCBTheme(settings) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Loading(link = uri),
                ) {
                    composable<Main> {
                        Main(navController, it.toRoute())
                    }
                    composable<Loading> {
                        Loading(navController, it.toRoute())
                    }
                }
            }
        }
    }
}