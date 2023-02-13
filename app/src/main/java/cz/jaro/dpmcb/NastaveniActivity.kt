package cz.jaro.dpmcb

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        ) {
                            Switch(checked = false, onCheckedChange = {})
                            Switch(checked = false, onCheckedChange = {})
                            Switch(checked = false, onCheckedChange = {})
                            Switch(checked = false, onCheckedChange = {})
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(this@NastaveniActivity, LoadingActivity::class.java)
                                        intent.flags = FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                                        intent.putExtra("update", true)
                                        startActivity(intent)
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
