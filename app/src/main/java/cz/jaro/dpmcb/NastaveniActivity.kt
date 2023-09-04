package cz.jaro.dpmcb

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.darkMode
import cz.jaro.dpmcb.ui.nastaveni.Nastaveni
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import org.koin.android.ext.android.inject

class NastaveniActivity : AppCompatActivity() {

    val repo by inject<SpojeRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val nastaveni by repo.nastaveni.collectAsStateWithLifecycle()
            DPMCBTheme(
                useDarkTheme = nastaveni.darkMode(),
                useDynamicColor = nastaveni.dynamickeBarvy,
                theme = nastaveni.tema,
            ) {
                Nastaveni(
                    finish = ::finish
                )
            }
        }
    }
}
