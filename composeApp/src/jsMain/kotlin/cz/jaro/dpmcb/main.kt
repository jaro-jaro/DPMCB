package cz.jaro.dpmcb

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.cash.sqldelight.db.SqlDriver
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.StorageSettings
import com.russhwolf.settings.observable.makeObservable
import cz.jaro.dpmcb.data.DebugManager
import cz.jaro.dpmcb.data.JsLogger
import cz.jaro.dpmcb.data.Logger
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.UserOnlineManager
import cz.jaro.dpmcb.data.database.WebWorkerDriverFactory
import cz.jaro.dpmcb.data.initKoin
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.loading.AppUpdater
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.main.setAppTitle
import cz.jaro.dpmcb.ui.map.DiagramManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.github.z4kn4fein.semver.toVersion
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skiko.wasm.onWasmReady
import org.koin.compose.ComposeContextWrapper
import org.koin.compose.LocalKoinApplication
import org.koin.compose.LocalKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.module

@OptIn(ExperimentalSettingsApi::class)
val jsModule = module(false) {
    single<FirebaseApp> {
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
    single<SqlDriver> { WebWorkerDriverFactory.driver }
    single<UserOnlineManager> { UserOnlineManager { true } }
    single { StorageSettings().makeObservable() }
    single<DetailsOpener> { DetailsOpener {} }
    single<DiagramManager> {
        object : DiagramManager {
            override suspend fun downloadDiagram(path: String, progress: (Float) -> Unit) = Unit
            override val imageData = null
            override fun checkDiagram() = true
        }
    }
    single<AppUpdater> { AppUpdater {} }
    single<CardManager> {
        object : CardManager {
            override fun loadCard() = Unit
            override fun removeCard() = Unit
            override val card = MutableStateFlow(null)
        }
    }
    single { DebugManager { BuildKonfig.versionName.toVersion(strict = false).isPreRelease } }
    single<Logger> { JsLogger(get()) }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalSettingsApi::class, KoinInternalApi::class)
fun main() = try {
    val koinApp = initKoin(jsModule)

    val repo = koinApp.koin.get<SpojeRepository>()

    val link = window.location.hash.removePrefix("#") + window.location.search
    onWasmReady {
        setAppTitle("Lepší DPMCB")
        ComposeViewport {
            CompositionLocalProvider(
                LocalKoinApplication provides ComposeContextWrapper { koinApp.koin },
                LocalKoinScope provides ComposeContextWrapper { koinApp.koin.scopeRegistry.rootScope },
            ) {
                SuperMainContent(
                    repo = repo,
                    link = link.takeUnless { it.isBlank() },
                    reset = {
                        window.location.reload()
                    }
                )
            }
        }
    }
} catch (e: Exception) {
    console.error(e)
    e.printStackTrace()
}