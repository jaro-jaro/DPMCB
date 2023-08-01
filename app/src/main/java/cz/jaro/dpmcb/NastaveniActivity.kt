package cz.jaro.dpmcb

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
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
            DPMCBTheme(
                repo.darkMode()
            ) {
                Nastaveni(
                    finish = ::finish
                )
            }
        }
    }
}
