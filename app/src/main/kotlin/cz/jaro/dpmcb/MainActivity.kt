package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.Main
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {

    val repo by inject<SpojeRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            DPMCBTheme(settings) {
                val scope = rememberCoroutineScope()
                Main(
                    link = intent.getStringExtra(LoadingViewModel.EXTRA_KEY_DEEPLINK),
                    isDataUpdateNeeded = intent.getBooleanExtra(LoadingViewModel.EXTRA_KEY_UPDATE_DATA, false),
                    isAppUpdateNeeded = intent.getBooleanExtra(LoadingViewModel.EXTRA_KEY_UPDATE_APP, false),
                    updateApp = { // Temporary, will be replaced with AppUpdater in the future
                        scope.launch(Dispatchers.IO) {
                            val doc = try {
                                withContext(Dispatchers.IO) {
                                    Ksoup.parseGetRequest("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/app/version.txt")
                                }
                            } catch (e: SocketTimeoutException) {
                                Firebase.crashlytics.recordException(e)
                                return@launch
                            }

                            val newestVersion = doc.text()

                            startActivity(Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = "https://github.com/jaro-jaro/DPMCB/releases/download/v$newestVersion/Lepsi-DPMCB-v$newestVersion.apk".toUri()
                            })
                        }
                        Unit
                    }
                )
            }
        }
    }
}
