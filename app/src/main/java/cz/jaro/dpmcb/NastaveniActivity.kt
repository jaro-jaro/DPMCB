package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cz.jaro.dpmcb.BuildConfig.DEBUG
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.databinding.ActivityNastaveniBinding
import kotlinx.coroutines.launch

class NastaveniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNastaveniBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityNastaveniBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        title = getString(R.string.nastaveni)

        binding.btnAktualizovat.setOnClickListener {

            val intent = Intent(this, LoadingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            intent.putExtra("update", true)
            startActivity(intent)
        }

        if (DEBUG) {
            lifecycleScope.launch {
                binding.tvVerze.text = "Aktuální verze dat: ${repo.verze}"
            }
        }
    }
}
