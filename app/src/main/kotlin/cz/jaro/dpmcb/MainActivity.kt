package cz.jaro.dpmcb

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.Main
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    val repo by inject<SpojeRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            DPMCBTheme(settings) {
                Main(
                    link = intent.getStringExtra(LoadingViewModel.EXTRA_KEY_DEEPLINK),
                    isDataUpdateNeeded = intent.getBooleanExtra(LoadingViewModel.EXTRA_KEY_UPDATE_DATA, false),
                    isAppUpdateNeeded = intent.getBooleanExtra(LoadingViewModel.EXTRA_KEY_UPDATE_APP, false),
                )
            }
        }
    }
}
