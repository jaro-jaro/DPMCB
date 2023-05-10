package cz.jaro.dpmcb.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.defaultModule

class App : Application() {

    companion object {
        var title by mutableStateOf(R.string.app_name)
        var vybrano by mutableStateOf(null as SuplikAkce?)
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            defaultModule()
        }
    }
}
