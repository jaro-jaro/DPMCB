package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyViewModel
import cz.jaro.dpmcb.ui.spojeni.SpojeniViewModel
import cz.jaro.dpmcb.ui.spojeni.VyhledavacSpojeni
import cz.jaro.dpmcb.ui.spojeni.VysledkySpojeniViewModel
import cz.jaro.dpmcb.ui.vybirator.VybiratorViewModel
import cz.jaro.dpmcb.ui.zjr.JizdniRadyViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

typealias GraphZastavek = Map<String, Set<String>>
typealias MutableGraphZastavek = MutableMap<String, MutableSet<String>>
typealias Spojeni = List<VyhledavacSpojeni.CastSpojeni>

class App : Application() {

    companion object {

        @delegate:StringRes
        var title by mutableStateOf(R.string.app_name)

        lateinit var prefs: SharedPreferences

        lateinit var repo: SpojeRepository
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("PREFS_DPMCB_JARO", Context.MODE_PRIVATE)
        repo = SpojeRepository(this)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(module {

                viewModel {
                    OdjezdyViewModel(it.component1(), it.component2(), it.component3())
                }
                viewModel {
                    JizdniRadyViewModel(it.component1(), it.component2(), it.component3())
                }
                viewModel {
                    VybiratorViewModel(it.component1(), it.component2(), it.component3(), it.component4())
                }
                viewModel {
                    SpojeniViewModel()
                }
                viewModel {
                    VysledkySpojeniViewModel(it.component1())
                }
            })
        }
    }
}
