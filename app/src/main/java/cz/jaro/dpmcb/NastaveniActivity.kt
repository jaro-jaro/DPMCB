package cz.jaro.dpmcb

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.ui.theme.DPMCBTheme

class NastaveniActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val nastaveni by repo.nastaveni.collectAsStateWithLifecycle()
            DPMCBTheme(
                if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm
            ) {
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
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Určit tmavý režim podle systému", Modifier.weight(1F))

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
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Tmavý režim", Modifier.weight(1F))

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
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Automaticky zakázat připojení k internetu po zapnutí aplikace", Modifier.weight(1F))

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
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Provádět kontrolu dostupnosti aktualizací při startu aplikace", Modifier.weight(1F))

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
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Zapamatovat si volbu Zobrazit nízkopodlažnosti v JŘ", Modifier.weight(1F))

                                Switch(
                                    checked = nastaveni.zachovavatNizkopodlaznost,
                                    onCheckedChange = { value ->
                                        upravit {
                                            it.copy(zachovavatNizkopodlaznost = value)
                                        }
                                    },
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.weight(1F),
                                    contentAlignment = Alignment.CenterStart
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
                                }
                                Box(
                                    Modifier.weight(1F),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Button(onClick = {

                                        if (isOnline)
                                            startActivity(Intent(this@NastaveniActivity, LoadingActivity::class.java).apply {
                                                flags = FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                                                putExtra("update", true)
                                            })
                                        else
                                            Toast.makeText(this@NastaveniActivity, "Jste offline!", Toast.LENGTH_SHORT).show()
                                    }
                                    ) {
                                        Text("Aktualizovat data")
                                    }
                                }
                            }
                            Text("Aktuální verze dat: ${repo.verze}")
                            Text("Aktuální verze Aplikace: ${BuildConfig.VERSION_NAME}")
                            Text("2019-2023 RO studios; Za zobrazená data neručíme")
                        }
                    }
                }
            }
        }
    }
}
