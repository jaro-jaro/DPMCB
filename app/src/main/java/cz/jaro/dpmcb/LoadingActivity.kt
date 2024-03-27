package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.darkMode
import cz.jaro.dpmcb.ui.loading.Loading
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import org.koin.android.ext.android.inject

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val update = intent.getBooleanExtra("update", false)
        val uri = intent?.action?.equals(Intent.ACTION_VIEW)?.let { intent?.data }?.run { toString().removePrefix("${scheme}://${host}") }

        val repo by inject<SpojeRepository>()

        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            DPMCBTheme(
                useDarkTheme = settings.darkMode(),
                useDynamicColor = settings.dynamicColors,
                theme = settings.theme,
            ) {
                Loading(uri = uri, update = update, finish = ::finish)
            }
        }
    }
}