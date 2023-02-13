package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ramcosta.composedestinations.DestinationsNavHost
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Datum
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.VDP
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toChar
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.typDne
import cz.jaro.dpmcb.ui.NavGraphs
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val keyboardController = LocalSoftwareKeyboardController.current!!

            DPMCBTheme {
                val drawerState = rememberDrawerState(DrawerValue.Open) {
                    keyboardController.hide()

                    true
                }
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                var vybrano by remember { mutableStateOf(SuplikAkce.Oblibene) }

                Scaffold(
                    topBar = {
                        // IconButtony
                        TopAppBar(
                            title = {
                                Text(stringResource(App.title))
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawerState.apply {
                                                keyboardController.hide()
                                                if (isClosed) open() else close()
                                            }
                                        }
                                    }
                                ) {
                                    IconWithTooltip(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Otevřít"
                                    )
                                }
                            },
                            actions = {
                                val jeOnline by repo.isOnline.collectAsState(false)
                                val onlineMod by repo.onlineMod.collectAsState(false)
                                IconButton(onClick = {
                                    if (!jeOnline) return@IconButton

                                    repo.upravitOnlineMod(!onlineMod)
                                }) {
                                    Icon(
                                        imageVector = if (jeOnline && onlineMod) Icons.Default.Wifi else Icons.Default.WifiOff,
                                        contentDescription = when {
                                            jeOnline && onlineMod -> "Online, kliknutím přepnete do offline módu"
                                            jeOnline && !onlineMod -> "Offline, kliknutím vypnete offline méó"
                                            else -> "Offline, nejste připojeni k internetu"
                                        },
                                        tint = if (jeOnline) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                    )
                                }
                            })
                    },
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .padding(paddingValues)
                    ) {
                        ModalNavigationDrawer(
                            drawerContent = {
                                ModalDrawerSheet {
                                    SuplikAkce.values().forEach { akce ->
                                        if (akce == SuplikAkce.Datum) Row(
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                            verticalAlignment = CenterVertically
                                        ) {
                                            var typDne by remember { mutableStateOf(repo.typDne.value) }

                                            Text(
                                                text = "Typ dne:",
                                                modifier = Modifier.padding(all = 16.dp)
                                            )

                                            VDP.values().forEach { vdp ->
                                                OutlinedIconToggleButton(
                                                    checked = typDne == vdp,
                                                    onCheckedChange = {
                                                        typDne = vdp
                                                        scope.launch {
                                                            repo.upravitTypDne(vdp)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                ) {
                                                    Text(vdp.toChar().toString())
                                                }
                                            }

                                            Spacer(
                                                modifier = Modifier.weight(1F)
                                            )

                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        MaterialAlertDialogBuilder(this@MainActivity).apply {
                                                            setTitle("Vybrat typ dne podle data")

                                                            val ll = LinearLayout(context)

                                                            val dp = android.widget.DatePicker(context)
                                                            //dp.maxDate = Calendar.getInstance().apply { set(3000, 12, 30) }.timeInMillis
                                                            dp.layoutParams = LinearLayout.LayoutParams(
                                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                                            )
                                                            dp.updateLayoutParams<LinearLayout.LayoutParams> {
                                                                updateMargins(top = 16)
                                                            }

                                                            ll.addView(dp)

                                                            setView(ll)

                                                            setPositiveButton("Zvolit") { dialog, _ ->
                                                                dialog.cancel()

                                                                val typ = Datum(dp.dayOfMonth, dp.month + 1, dp.year).typDne
                                                                typDne = typ
                                                                repo.upravitTypDne(typ)
                                                            }
                                                            show()
                                                        }
                                                    }
                                                },
                                            ) {
                                                IconWithTooltip(Icons.Default.CalendarMonth, "Vybrat podle data")
                                            }
                                        }
                                        else NavigationDrawerItem(
                                            label = {
                                                Text(stringResource(akce.jmeno))
                                            },
                                            icon = {
                                                IconWithTooltip(akce.icon, stringResource(akce.jmeno))
                                            },
                                            selected = vybrano == akce,
                                            onClick = {
                                                if (akce.multiselect)
                                                    vybrano = akce
                                                akce.onClick(navController, { scope.launch { drawerState.close() } }, this@MainActivity)
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }
                                }
                            },
                            drawerState = drawerState,
                            gesturesEnabled = vybrano != SuplikAkce.Mapa
                        ) {
                            DestinationsNavHost(
                                navController = navController,
                                navGraph = NavGraphs.root
                            )
                        }
                    }
                }
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
