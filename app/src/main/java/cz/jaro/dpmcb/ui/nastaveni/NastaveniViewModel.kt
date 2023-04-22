package cz.jaro.dpmcb.ui.nastaveni

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.App.Companion.repo

class NastaveniViewModel(
    private val startActivity: (Intent) -> Unit,
    private val finish: () -> Unit,
    private val loadingActivityIntent: Intent,
    private val jsteOfflineToast: () -> Unit,
) : ViewModel() {

    val nastaveni = repo.nastaveni
    val upravitNastaveni = repo::upravitNastaveni
    val verze = repo.verze

    fun aktualizovatAplikaci() {
        startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases/latest")
        })
    }

    fun aktualizovatData() {
        if (repo.isOnline.value) {
            startActivity(loadingActivityIntent.apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                putExtra("update", true)
            })
            finish()
        } else
            jsteOfflineToast()
    }
}