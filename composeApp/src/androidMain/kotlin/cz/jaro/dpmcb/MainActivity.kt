package cz.jaro.dpmcb

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.work
import cz.jaro.dpmcb.ui.card.AndroidCardManager
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.loading.AndroidAppUpdater
import cz.jaro.dpmcb.ui.loading.AppUpdater
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.compose.LocalKoinApplication
import org.koin.compose.LocalKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module


class MainActivity : ComponentActivity() {
    val repo by inject<SpojeRepository>()

    @OptIn(KoinInternalApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.addOnControllableInsetsChangedListener { c, m ->
                m.work("Insect")
            }
        }

        val updater = AndroidAppUpdater(this)
        val cardManager = AndroidCardManager(this, get())
        loadKoinModules(module(createdAtStart = true) {
            single<AppUpdater> { updater }
            single<CardManager> { cardManager }
        })

        val uri = intent?.action?.equals(Intent.ACTION_VIEW)?.let { intent?.data }?.run { toString().removePrefix("${scheme}://${host}") }

        val koin = getKoin()

        setContent {
            CompositionLocalProvider(
                LocalKoinApplication provides koin,
                LocalKoinScope provides koin.scopeRegistry.rootScope
            ) {
                SuperMainContent(
                    repo = repo,
                    link = uri,
                    reset = {
                        val packageManager = packageManager
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        val componentName = intent!!.component
                        val mainIntent = Intent.makeRestartActivityTask(componentName)
                        mainIntent.setPackage(packageName)
                        startActivity(mainIntent)
                        Runtime.getRuntime().exit(0)
                    },
                )
            }
        }
    }
}