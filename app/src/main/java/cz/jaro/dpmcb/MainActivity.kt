package cz.jaro.dpmcb

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.darkMode
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.Main
import cz.jaro.dpmcb.ui.theme.DPMCBTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            DPMCBTheme(
                darkMode()
            ) {
                Main(
                    link = intent.getStringExtra(LoadingViewModel.EXTRA_KEY_DEEPLINK),
                    jePotrebaAktualizovatData = intent.getBooleanExtra(LoadingViewModel.EXTRA_KEY_AKTUALIZOVAT_DATA, false),
                    jePotrebaAktualizovatAplikaci = intent.getBooleanExtra(LoadingViewModel.EXTRA_KEY_AKTUALIZOVAT_APLIKACI, false),
                )
            }
        }
    }
}
