package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.marosseleng.compose.material3.datetimepickers.date.ui.dialog.DatePickerDialog
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.navigate
import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.toDatum
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.ui.NavGraphs
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val link = intent?.getStringExtra("link")

        setContent {
            val keyboardController = LocalSoftwareKeyboardController.current!!

            val nastaveni by repo.nastaveni.collectAsStateWithLifecycle()
            DPMCBTheme(
                if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm
            ) {
                val drawerState = rememberDrawerState(DrawerValue.Open) {
                    keyboardController.hide()

                    true
                }
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                var vybrano by remember { mutableStateOf<SuplikAkce?>(SuplikAkce.Oblibene) }
                LaunchedEffect(Unit) {
                    link?.let {
                        navController.navigate(
                            when {
                                it.startsWith("/spoj") -> DetailSpojeScreenDestination(it.split("/").last())
                                else -> return@let
                            }
                        )
                        vybrano = null
                        drawerState.close()
                    }
                }

                Scaffold(
                    topBar = {
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
                                val cas by Cas.tedFlow.collectAsStateWithLifecycle()
                                Text(cas.toString(true))

                                val jeOnline by repo.isOnline.collectAsStateWithLifecycle()
                                val onlineMod by repo.onlineMod.collectAsStateWithLifecycle()
                                IconButton(onClick = {
                                    if (!jeOnline) return@IconButton

                                    repo.upravitOnlineMod(!onlineMod)
                                }) {
                                    IconWithTooltip(
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
                                            val datum by repo.datum.collectAsStateWithLifecycle()

                                            Text(
                                                text = "Používané datum: $datum",
                                                modifier = Modifier.padding(all = 16.dp)
                                            )
                                            var zobrazitDialog by rememberSaveable { mutableStateOf(false) }
                                            if (zobrazitDialog) DatePickerDialog(
                                                onDismissRequest = {
                                                    zobrazitDialog = false
                                                },
                                                onDateChange = {
                                                    repo.upravitDatum(it.toDatum())
                                                    zobrazitDialog = false
                                                },
                                                title = {
                                                    Text("Vybrat nové datum")
                                                },
                                                initialDate = datum.toLocalDate()
                                            )

                                            Spacer(
                                                modifier = Modifier.weight(1F)
                                            )

                                            IconButton(
                                                onClick = {
                                                    zobrazitDialog = true
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.CalendarMonth, "Změnit datum")
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

                                                akce.onClick(
                                                    navController::navigate,
                                                    {
                                                        scope.launch {
                                                            drawerState.close()
                                                        }
                                                    },
                                                    this@MainActivity
                                                )
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }
                                }
                            },
                            drawerState = drawerState,
//                            gesturesEnabled = vybrano != SuplikAkce.Mapa
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
