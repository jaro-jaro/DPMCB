package cz.jaro.dpmcb

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.ui.nastaveni.Nastaveni
import cz.jaro.dpmcb.ui.theme.DPMCBTheme

class NastaveniActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val nastaveni by repo.nastaveni.collectAsStateWithLifecycle()
            DPMCBTheme(
                if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm
            ) {
                Nastaveni()
            }
        }
    }
}
