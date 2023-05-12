package cz.jaro.dpmcb.ui.main

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.marosseleng.compose.material3.datetimepickers.date.ui.dialog.DatePickerDialog
import com.ramcosta.composedestinations.DestinationsNavHost
import cz.jaro.dpmcb.ExitActivity
import cz.jaro.dpmcb.LoadingActivity
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.ui.NavGraphs
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import kotlin.reflect.KClass

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Main(
    link: String?,
    jePotrebaAktualizovatData: Boolean,
    jePotrebaAktualizovatAplikaci: Boolean,
) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val keyboardController = LocalSoftwareKeyboardController.current

    val drawerState = rememberDrawerState(DrawerValue.Open) {
        keyboardController?.hide()

        true
    }
    val closeDrawer = {
        scope.launch {
            drawerState.close()
        }
        Unit
    }
    val openDrawer = {
        scope.launch {
            drawerState.open()
        }
    }

    val ctx = LocalContext.current

    val viewModel: MainViewModel = koinViewModel {
        parametersOf(closeDrawer, link, navController, Intent(ctx, LoadingActivity::class.java), { it: Intent -> ctx.startActivity(it) })
    }

    val jeOnline = viewModel.jeOnline.collectAsStateWithLifecycle()
    val onlineMod = viewModel.onlineMod.collectAsStateWithLifecycle()
    val datum = viewModel.datum.collectAsStateWithLifecycle()

    MainScreen(
        startActivity = {
            ctx.startActivity(Intent(ctx, it.java))
        },
        startIntent = ctx::startActivity,
        drawerState = drawerState,
        toggleDrawer = {
            keyboardController?.hide()
            if (drawerState.isClosed) openDrawer() else closeDrawer()
        },
        closeDrawer = closeDrawer,
        jeOnline = jeOnline,
        onlineMod = onlineMod,
        navigate = navController.navigateFunction,
        showToast = { text, duration ->
            Toast.makeText(ctx, text, duration).show()
        },
        upravitOnlineMod = viewModel.upravitOnlineMod,
        datum = datum,
        upravitDatum = viewModel.upravitDatum,
        tuDuDum = {
            MediaPlayer.create(ctx, R.raw.koncime).apply {
                setOnCompletionListener {
                    ExitActivity.exitApplication(ctx)
                    closeDrawer()
                }
                start()
            }
            ctx.getSystemService(AudioManager::class.java).apply {
                repeat(20) {
                    adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                }
            }
        },
        jePotrebaAktualizovatAplikaci = jePotrebaAktualizovatAplikaci,
        aktualizovatAplikaci = viewModel.aktualizovatAplikaci,
        jePotrebaAktualizovatData = jePotrebaAktualizovatData,
        aktualizovatData = viewModel.aktualizovatData
    ) {
        DestinationsNavHost(
            navController = navController,
            navGraph = NavGraphs.root
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    startActivity: (KClass<out Activity>) -> Unit,
    startIntent: (Intent) -> Unit,
    drawerState: DrawerState,
    toggleDrawer: () -> Unit,
    closeDrawer: () -> Unit,
    jeOnline: State<Boolean>,
    onlineMod: State<Boolean>,
    navigate: NavigateFunction,
    showToast: (String, Int) -> Unit,
    tuDuDum: () -> Unit,
    upravitOnlineMod: (Boolean) -> Unit,
    datum: State<LocalDate>,
    upravitDatum: (LocalDate) -> Unit,
    jePotrebaAktualizovatData: Boolean,
    jePotrebaAktualizovatAplikaci: Boolean,
    aktualizovatData: () -> Unit,
    aktualizovatAplikaci: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(App.title))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            toggleDrawer()
                        }
                    ) {
                        IconWithTooltip(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Otevřít"
                        )
                    }
                },
                actions = {
                    val cas by UtilFunctions.tedFlow.collectAsStateWithLifecycle()
                    Text(cas.toString())

                    IconButton(onClick = {
                        if (!jeOnline.value) return@IconButton

                        upravitOnlineMod(!onlineMod.value)
                    }) {
                        IconWithTooltip(
                            imageVector = if (jeOnline.value && onlineMod.value) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = when {
                                jeOnline.value && onlineMod.value -> "Online, kliknutím přepnete do offline módu"
                                jeOnline.value && !onlineMod.value -> "Offline, kliknutím vypnete offline mód"
                                else -> "Offline, nejste připojeni k internetu"
                            },
                            tint = if (jeOnline.value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        )
                    }
                })
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            if (jePotrebaAktualizovatData) {
                var zobrazitDialog by remember { mutableStateOf(true) }

                if (zobrazitDialog) AlertDialog(
                    onDismissRequest = {
                        zobrazitDialog = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                zobrazitDialog = false
                                aktualizovatData()
                            }
                        ) {
                            Text(stringResource(id = R.string.ano))
                        }
                    },
                    title = {
                        Text(stringResource(id = R.string.aktualizace_jr))
                    },
                    text = {
                        Text(stringResource(id = R.string.chcete_aktualizovat))
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                zobrazitDialog = false
                            }
                        ) {
                            Text(stringResource(id = R.string.ne))
                        }
                    },
                )
            }
            if (jePotrebaAktualizovatAplikaci) {
                var zobrazitDialog by remember { mutableStateOf(true) }

                if (zobrazitDialog) AlertDialog(
                    onDismissRequest = {
                        zobrazitDialog = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                zobrazitDialog = false
                                aktualizovatAplikaci()
                            }
                        ) {
                            Text(stringResource(id = R.string.ano))
                        }
                    },
                    title = {
                        Text("Aktualizace aplikace")
                    },
                    text = {
                        Text("Je k dispozici nová verze aplikace, chcete ji aktualizovat?")
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                zobrazitDialog = false
                            }
                        ) {
                            Text(stringResource(id = R.string.ne))
                        }
                    },
                )
            }
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        SuplikAkce.values().forEach { akce ->
                            when (akce) {
                                SuplikAkce.ZpetnaVazba -> {
                                    var zobrazitDialog by rememberSaveable { mutableStateOf(false) }
                                    var hodnoceni by rememberSaveable { mutableStateOf(-1) }

                                    if (zobrazitDialog) AlertDialog(
                                        onDismissRequest = {
                                            zobrazitDialog = false
                                        },
                                        title = {
                                            Text("Ohodnotit aplikaci")
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
                                                val ref = database.getReference("hodnoceni")
                                                ref.push().setValue("${hodnoceni + 1}/5")
                                                hodnoceni = -1
                                                zobrazitDialog = false
                                            }) {
                                                Text("Odeslat")
                                            }
                                        },
                                        text = {
                                            Column {
                                                Row {
                                                    repeat(5) { i ->
                                                        IconButton(onClick = {
                                                            hodnoceni = if (hodnoceni == i) -1 else i
                                                        }, Modifier.weight(1F)) {
                                                            if (hodnoceni >= i)
                                                                Icon(imageVector = Icons.Outlined.Star, contentDescription = null, tint = Color.Yellow)
                                                            else
                                                                Icon(imageVector = Icons.Outlined.StarOutline, contentDescription = null, tint = Color.Yellow)
                                                        }
                                                    }
                                                }
                                                Text("Chcete něco dodat? Prosím, obraťte se na náš GitHub, kde s vámi můžeme jednoduše komunikovat, nebo nás kontaktujte osobně. :)")
                                                TextButton(onClick = {
                                                    startIntent(Intent().apply {
                                                        action = Intent.ACTION_VIEW
                                                        data = Uri.parse("https://github.com/jaro-jaro/DPMCB/discussions/133#discussion-5045148")
                                                    })
                                                }) {
                                                    Text(text = "Přejít na GitHub")
                                                }
                                            }
                                        }
                                    )
                                    NavigationDrawerItem(
                                        label = {
                                            Text(stringResource(akce.jmeno))
                                        },
                                        icon = {
                                            IconWithTooltip(akce.icon, stringResource(akce.jmeno))
                                        },
                                        selected = false,
                                        onClick = {
                                            if (jeOnline.value)
                                                zobrazitDialog = true
                                            else
                                                showToast("Jste offline!", Toast.LENGTH_SHORT)
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }

                                SuplikAkce.Vypnout -> {
                                    NavigationDrawerItem(
                                        label = {
                                            Text(stringResource(akce.jmeno))
                                        },
                                        icon = {
                                            IconWithTooltip(akce.icon, stringResource(akce.jmeno))
                                        },
                                        selected = false,
                                        onClick = {
                                            tuDuDum()
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }

                                SuplikAkce.Datum -> Row(
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Používané datum: ${datum.value.asString()}",
                                        modifier = Modifier.padding(all = 16.dp)
                                    )
                                    var zobrazitDialog by rememberSaveable { mutableStateOf(false) }
                                    if (zobrazitDialog) DatePickerDialog(
                                        onDismissRequest = {
                                            zobrazitDialog = false
                                        },
                                        onDateChange = {
                                            upravitDatum(it)
                                            zobrazitDialog = false
                                        },
                                        title = {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Vybrat nové datum")
                                                IconButton(
                                                    onClick = {
                                                        upravitDatum(LocalDate.now())
                                                        zobrazitDialog = false
                                                    }
                                                ) {
                                                    IconWithTooltip(Icons.Default.CalendarToday, "Nastavit dnes")
                                                }
                                            }
                                        },
                                        initialDate = datum.value,
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

                                else -> if (akce != SuplikAkce.PraveJedouci || datum.value == LocalDate.now()) NavigationDrawerItem(
                                    label = {
                                        Text(stringResource(akce.jmeno))
                                    },
                                    icon = {
                                        IconWithTooltip(akce.icon, stringResource(akce.jmeno))
                                    },
                                    selected = App.vybrano == akce,
                                    onClick = {
                                        if (akce.multiselect)
                                            App.vybrano = akce

                                        akce.onClick(
                                            navigate,
                                            { closeDrawer() },
                                            { startActivity(it) },
                                        )
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                },
                drawerState = drawerState,
            ) {
                content()
            }
        }
    }
}