package cz.jaro.dpmcb

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.ui.theme.DPMCBTheme

class NastaveniActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            DPMCBTheme {
                Surface {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Nastavení")
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
                                            startActivity(intent)
                                        }
                                    ) {
                                        IconWithTooltip(Icons.Default.ArrowBack, "Zpět")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        val nastaveni by repo.nastaveni.collectAsStateWithLifecycle()
                        val upravit = repo::upravitNastaveni

                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Určit tmavý režim podle systému")

                                Switch(
                                    checked = nastaveni.dmPodleSystemu,
                                    onCheckedChange = { value ->
                                        upravit {
                                            it.copy(dmPodleSystemu = value)
                                        }
                                    },
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Tmavý režim")

                                Switch(
                                    checked = if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm,
                                    onCheckedChange = { value ->
                                        upravit {
                                            it.copy(dm = value)
                                        }
                                    },
                                    enabled = !nastaveni.dmPodleSystemu
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Automaticky zakázat připojení k internetu po zapnutí aplikace")

                                Switch(
                                    checked = !nastaveni.autoOnline,
                                    onCheckedChange = { value ->
                                        upravit {
                                            it.copy(autoOnline = !value)
                                        }
                                    },
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Provádět kontrolu dostupnosti aktualizací při startu aplikace")

                                Switch(
                                    checked = nastaveni.kontrolaAktualizaci,
                                    onCheckedChange = { value ->
                                        upravit {
                                            it.copy(kontrolaAktualizaci = value)
                                        }
                                    },
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = {
                                        startActivity(Intent().apply {
                                            action = Intent.ACTION_VIEW
                                            data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases")
                                        })
                                    }
                                ) {
                                    Text("Aktualizovat aplikaci")
                                }
                                Button(
                                    onClick = {
                                        startActivity(Intent(this@NastaveniActivity, LoadingActivity::class.java).apply {
                                            flags = FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                                            putExtra("update", true)
                                        })
                                    }
                                ) {
                                    Text("Aktualizovat data")
                                }
                                //if (BuildConfig.DEBUG) Text("Aktuální verze dat: ${repo.verze}")
                            }
                        }
                    }
                }
            }
        }
    }
}
