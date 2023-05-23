package cz.jaro.dpmcb.ui.nastaveni

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.SpojeRepository
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class NastaveniViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val startActivity: (Intent) -> Unit,
        val finish: () -> Unit,
        val loadingActivityIntent: Intent,
        val jsteOfflineToast: () -> Unit,
    )

    val nastaveni = repo.nastaveni
    val upravitNastaveni = repo::upravitNastaveni
    val verze = repo.verze

    fun aktualizovatAplikaci() {
        params.startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases/latest")
        })
    }

    fun aktualizovatData() {
        if (repo.isOnline.value) {
            params.startActivity(params.loadingActivityIntent.apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                putExtra("update", true)
            })
            params.finish()
        } else
            params.jsteOfflineToast()
    }
}