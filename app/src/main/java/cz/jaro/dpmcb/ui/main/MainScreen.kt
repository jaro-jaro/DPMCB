package cz.jaro.dpmcb.ui.main

import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutManager
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.marosseleng.compose.material3.datetimepickers.date.ui.dialog.DatePickerDialog
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.utils.destination
import cz.jaro.dpmcb.BuildConfig
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
import cz.jaro.dpmcb.ui.appCurrentDestinationFlow
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyDestination
import cz.jaro.dpmcb.ui.destinations.PraveJedouciDestination
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorDestination
import cz.jaro.dpmcb.ui.navArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    LaunchedEffect(Unit) {
        val destinationFlow = navController.appCurrentDestinationFlow

        destinationFlow.collect { destination ->
            Firebase.analytics.logEvent("navigation") {
                param("route", destination.route)
            }
        }
    }

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

    val chyba = { it: String ->
        Toast.makeText(ctx, it, Toast.LENGTH_SHORT)
    }

    val viewModel: MainViewModel = koinViewModel {
        parametersOf(closeDrawer, link, navController, Intent(ctx, LoadingActivity::class.java), { it: Intent -> ctx.startActivity(it) }, chyba)
    }

    val jeOnline = viewModel.jeOnline.collectAsStateWithLifecycle()
    val onlineMod = viewModel.onlineMod.collectAsStateWithLifecycle()
    val datum = viewModel.datum.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            navController.currentBackStackEntryFlow.collect { entry ->
                @Suppress("UNCHECKED_CAST")
                val dest = entry.destination() as DestinationSpec<Any>

                val args: Any = when (dest::class) {
                    PraveJedouciDestination::class -> entry.navArgs<PraveJedouciDestination.NavArgs>()
                    OdjezdyDestination::class -> entry.navArgs<OdjezdyDestination.NavArgs>()
                    SpojDestination::class -> entry.navArgs<SpojDestination.NavArgs>()
                    VybiratorDestination::class -> entry.navArgs<VybiratorDestination.NavArgs>()
                    JizdniRadyDestination::class -> entry.navArgs<JizdniRadyDestination.NavArgs>()
                    else -> Unit
                }

                App.route = dest(args).route
            }
        }
    }

    MainScreen(
        startActivity = {
            ctx.startActivity(Intent(ctx, it.java).setAction(Intent.ACTION_MAIN))
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
        aktualizovatData = viewModel.aktualizovatData,
    ) {
        DestinationsNavHost(
            navController = navController,
            navGraph = NavGraphs.root
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

                    var open by remember { mutableStateOf(false) }
                    var show by remember { mutableStateOf(false) }
                    var route by remember { mutableStateOf("") }
                    var label by remember { mutableStateOf("") }

                    val ctx = LocalContext.current
                    val shortcutManager = ctx.getSystemService(ShortcutManager::class.java)!!
                    val res = ctx.resources

                    IconButton(onClick = {
                        open = !open
                    }) {
                        IconWithTooltip(imageVector = Icons.Default.MoreVert, contentDescription = "Více možností")
                    }

                    DropdownMenu(
                        expanded = open,
                        onDismissRequest = {
                            open = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Připnout zkratku na domovskou obrazovku")
                            },
                            onClick = {
                                route = App.route
                                label = res.getString(App.title)
                                open = false
                                show = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PushPin, null)
                            },
                        )
                    }

                    if (show) AlertDialog(
                        onDismissRequest = {
                            show = false
                        },
                        title = {
                            Text("Přidat zkratku na aktuální stránku na domovskou obrazovku")
                        },
                        icon = {
                            Icon(Icons.Default.PushPin, null)
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (shortcutManager.isRequestPinShortcutSupported) {

                                    val baseRoute = route.split("/")[0]

                                    val pinShortcutInfo = android.content.pm.ShortcutInfo
                                        .Builder(ctx, "shortcut-$baseRoute-$label")
                                        .setShortLabel(label)
                                        .setLongLabel(label)
                                        .setIcon(
                                            android.graphics.drawable.Icon.createWithResource(
                                                ctx, if (BuildConfig.DEBUG) R.drawable.logo_jaro else R.drawable.logo_dpmcb
                                            )
                                        )
                                        .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://jaro-jaro.github.io/DPMCB/$route")))
                                        .build()

                                    shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                                }
                                show = false
                            }) {
                                Text("Přidat")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                show = false
                            }) {
                                Text("Zrušit")
                            }
                        },
                        text = {
                            val focusManager = LocalFocusManager.current
                            Column {
                                TextField(
                                    value = route,
                                    onValueChange = {
                                        route = it
                                    },
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    label = {
                                        Text("Route")
                                    },
                                    keyboardActions = KeyboardActions {
                                        focusManager.moveFocus(FocusDirection.Down)
                                    },
                                )
                                TextField(
                                    value = label,
                                    onValueChange = {
                                        label = it
                                    },
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    label = {
                                        Text("Label")
                                    },
                                )
                            }
                        }
                    )
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
                            VecZeSupliku(
                                akce = akce,
                                navigate = navigate,
                                startIntent = startIntent,
                                jeOnline = jeOnline,
                                showToast = showToast,
                                datum = datum,
                                upravitDatum = upravitDatum,
                                tuDuDum = tuDuDum,
                                closeDrawer = closeDrawer,
                                startActivity = startActivity
                            )
                        }
                    }
                },
                drawerState = drawerState,
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun VecZeSupliku(
    akce: SuplikAkce,
    navigate: NavigateFunction,
    startIntent: (Intent) -> Unit,
    jeOnline: State<Boolean>,
    showToast: (String, Int) -> Unit,
    datum: State<LocalDate>,
    upravitDatum: (LocalDate) -> Unit,
    tuDuDum: () -> Unit,
    closeDrawer: () -> Unit,
    startActivity: (KClass<out Activity>) -> Unit,
) = when (akce) {
    SuplikAkce.ZpetnaVazba -> {
        var hodnoceni by rememberSaveable { mutableStateOf(-1) }
        var zobrazitDialog by rememberSaveable { mutableStateOf(false) }

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

    SuplikAkce.SpojPodleId -> {
        var zobrazitDialog by rememberSaveable { mutableStateOf(false) }
        var id by rememberSaveable { mutableStateOf("") }
        var jmeno by rememberSaveable { mutableStateOf("") }
        var linka by rememberSaveable { mutableStateOf("") }
        var cislo by rememberSaveable { mutableStateOf("") }

        val focusRequester = remember { FocusRequester() }

        fun potvrdit(spojId: String) {
            navigate(
                SpojDestination(
                    spojId = spojId
                )
            )
            zobrazitDialog = false
            closeDrawer()
            id = ""
            jmeno = ""
            linka = ""
            cislo = ""
        }

        if (zobrazitDialog) AlertDialog(
            onDismissRequest = {
                zobrazitDialog = false
                id = ""
                jmeno = ""
                linka = ""
                cislo = ""
            },
            title = {
                Text(stringResource(id = R.string.spoj_podle_id))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (linka.isNotEmpty() && cislo.isNotEmpty()) potvrdit(
                        "S-325${
                            when (linka.length) {
                                1 -> "00$linka"
                                2 -> "0$linka"
                                else -> linka
                            }
                        }-$cislo"
                    )
                    else if (jmeno.isNotEmpty()) potvrdit("S-${jmeno.replace("/", "-")}")
                    else potvrdit(id)
                }) {
                    Text("Vyhledat")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    zobrazitDialog = false
                    id = ""
                    jmeno = ""
                    linka = ""
                    cislo = ""
                }) {
                    Text("Zrušit")
                }
            },
            text = {
                val focusManager = LocalFocusManager.current
                Column {
                    Row {
                        TextField(
                            value = linka,
                            onValueChange = {
                                linka = it
                            },
                            Modifier
                                .weight(1F)
                                .padding(end = 8.dp)
                                .focusRequester(focusRequester),
                            label = {
                                Text("Linka")
                            },
                            keyboardActions = KeyboardActions {
                                focusManager.moveFocus(FocusDirection.Right)
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Number,
                            ),
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        TextField(
                            value = cislo,
                            onValueChange = {
                                cislo = it
                            },
                            Modifier.weight(1F),
                            label = {
                                Text("Č. spoje")
                            },
                            keyboardActions = KeyboardActions {
                                if (linka.isNotEmpty() && cislo.isNotEmpty())
                                    potvrdit(
                                        "S-325${
                                            when (linka.length) {
                                                1 -> "00$linka"
                                                2 -> "0$linka"
                                                else -> linka
                                            }
                                        }-$cislo"
                                    )
                                else
                                    focusManager.moveFocus(FocusDirection.Down)
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (linka.isNotEmpty() && cislo.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                                keyboardType = KeyboardType.Number,
                            ),
                        )
                    }
                    TextField(
                        value = jmeno,
                        onValueChange = {
                            jmeno = it
                        },
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = {
                            Text("Jméno spoje")
                        },
                        keyboardActions = KeyboardActions {
                            if (jmeno.isNotEmpty())
                                potvrdit("S-${jmeno.replace("/", "-")}")
                            else
                                focusManager.moveFocus(FocusDirection.Down)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (jmeno.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                        ),
                    )
                    TextField(
                        value = id,
                        onValueChange = {
                            id = it
                        },
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = {
                            Text("ID spoje")
                        },
                        keyboardActions = KeyboardActions {
                            potvrdit(id)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search,
                        ),
                    )
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
            selected = App.vybrano == akce,
            onClick = {
                zobrazitDialog = true
//                focusRequester.requestFocus()
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

    else -> if (akce == SuplikAkce.PraveJedouci && datum.value != LocalDate.now()) Unit
    else NavigationDrawerItem(
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