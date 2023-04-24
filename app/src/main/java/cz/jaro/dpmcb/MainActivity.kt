package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.darkMode
import cz.jaro.dpmcb.ui.main.Main
import cz.jaro.dpmcb.ui.theme.DPMCBTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val link = intent?.getStringExtra("link")

        setContent {
            DPMCBTheme(
                darkMode()
            ) {
                Main(link)
            }
        }

        if (intent.getBooleanExtra("update", false)) {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(R.string.aktualizace_jr)
                setMessage(R.string.chcete_aktualizovat)
                setNegativeButton(R.string.ne) { dialog, _ -> dialog.cancel() }

                setPositiveButton(R.string.ano) { dialog, _ ->
                    dialog.cancel()

                    val intent = Intent(context, LoadingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                    intent.putExtra("update", true)
                    startActivity(intent)
                }
                show()
            }
        }
    }
}
