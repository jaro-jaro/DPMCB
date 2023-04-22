package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.ui.loading.Loading
import cz.jaro.dpmcb.ui.theme.DPMCBTheme

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val update = intent.getBooleanExtra("update", false)
        val uri = intent?.action?.equals(Intent.ACTION_VIEW)?.let { intent?.data }?.path

        setContent {
            val nastaveni by repo.nastaveni.collectAsStateWithLifecycle()
            DPMCBTheme(
                if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm
            ) {
                Loading(uri = uri, update = update, finish = ::finish)
            }
        }
    }
}