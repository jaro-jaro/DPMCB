package cz.jaro.dpmcb

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.StorageSettings
import com.russhwolf.settings.observable.makeObservable
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.UserOnlineManager
import cz.jaro.dpmcb.data.initKoin
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.loading.AppUpdater
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.map.DiagramManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.storage.StorageReference
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skiko.wasm.onWasmReady
import org.koin.compose.LocalKoinApplication
import org.koin.compose.LocalKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.w3c.dom.Worker

@OptIn(ExperimentalComposeUiApi::class, ExperimentalSettingsApi::class, KoinInternalApi::class)
fun main() {
    val location = window.location.hash.removePrefix("#") + window.location.search

    val koinApp = initKoin {
        single {
            Firebase.initialize(
                null, FirebaseOptions(
                    apiKey = "AIzaSyA9O1-nYFEmY0pszqGV5AyXPvJLIsuwvFg",
                    authDomain = "dpmcb-jaro.firebaseapp.com",
                    databaseUrl = "https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/",
                    projectId = "dpmcb-jaro",
                    storageBucket = "dpmcb-jaro.appspot.com",
                    gcmSenderId = "867578529394",
                    applicationId = "1:867578529394:web:651bf2325825b415bba4eb",
                    gaTrackingId = "G-EBQL901DWT",
                )
            )
        }
        single { UserOnlineManager { true } }
        single { StorageSettings().makeObservable() }
        single<SqlDriver> {
            WebWorkerDriver(
                Worker(
                    js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
                )
            )
        }
        single { DetailsOpener {} }
        single<DiagramManager> { object : DiagramManager {
            override suspend fun downloadDiagram(reference: StorageReference, progress: (Float) -> Unit) = Unit
            override val imageData = null
            override fun checkDiagram() = true
        } }
        single { AppUpdater {} }
        single { object : CardManager {
            override fun loadCard() = Unit
            override fun removeCard() = Unit
            override val card = MutableStateFlow(null)
        } }
    }

    val repo = koinApp.koin.get<SpojeRepository>()

    val link = window.location.run { href.removePrefix("${protocol}://${host}") }
    onWasmReady {
        CanvasBasedWindow(
            title = "Gymceska",
        ) {
            CompositionLocalProvider(
                LocalKoinApplication provides koinApp.koin,
                LocalKoinScope provides koinApp.koin.scopeRegistry.rootScope
            ) {
                SuperMainContent(repo, link)
            }
        }
    }
}
