package cz.jaro.dpmcb

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.settings.Settings
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import org.koin.android.ext.android.inject

class SettingsActivity : AppCompatActivity() {

    val repo by inject<SpojeRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            DPMCBTheme(settings) {
                Settings(
                    finish = ::finish
                )
            }
        }
    }
}
