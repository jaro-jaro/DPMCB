package cz.jaro.dpmcb.ui.main

import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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
import cz.jaro.dpmcb.ui.chooser.autoFocus
import cz.jaro.dpmcb.ui.destinations.BusDestination
import cz.jaro.dpmcb.ui.destinations.SequenceDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import kotlin.reflect.KClass

@Composable
fun Main(
    link: String?,
    isDataUpdateNeeded: Boolean,
    isAppUpdateNeeded: Boolean,
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

    val logError = { it: String ->
        Toast.makeText(ctx, it, Toast.LENGTH_SHORT)
    }

    val viewModel: MainViewModel = koinViewModel {
        parametersOf(closeDrawer, link, navController, Intent(ctx, LoadingActivity::class.java), { it: Intent -> ctx.startActivity(it) }, logError)
    }

    val isOnline = viewModel.isOnline.collectAsStateWithLifecycle()
    val hasCard = viewModel.hasCard.collectAsStateWithLifecycle()
    val isOnlineModeEnabled = viewModel.isOnlineModeEnabled.collectAsStateWithLifecycle()
    val date = viewModel.date.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            navController.currentBackStackEntryFlow.collect { entry ->
                @Suppress("UNCHECKED_CAST")
                val dest = entry.destination() as DestinationSpec<Any>

                App.route = dest(dest.argsFrom(entry)).route
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
        isOnline = isOnline,
        isOnlineModeEnabled = isOnlineModeEnabled,
        navigate = navController.navigateFunction,
        showToast = { text, duration ->
            Toast.makeText(ctx, text, duration).show()
        },
        editOnlineMode = viewModel.editOnlineMode,
        date = date,
        changeDate = viewModel.changeDate,
//        tuDuDum = {
//            MediaPlayer.create(ctx, R.raw.koncime).apply {
//                setOnCompletionListener {
//                    ExitActivity.exitApplication(ctx)
//                    closeDrawer()
//                }
//                start()
//            }
//            ctx.getSystemService(AudioManager::class.java).apply {
//                repeat(20) {
//                    adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
//                }
//            }
//        },
        isAppUpdateNeeded = isAppUpdateNeeded,
        updateApp = viewModel.updateApp,
        isDataUpdateNeeded = isDataUpdateNeeded,
        updateData = viewModel.updateData,
        removeCard = viewModel.removeCard,
        hasCard = hasCard,
        findBusByEvn = viewModel.findBusByEvn,
        findSequences = viewModel.findSequences,
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
    isOnline: State<Boolean>,
    isOnlineModeEnabled: State<Boolean>,
    navigate: NavigateFunction,
    showToast: (String, Int) -> Unit,
//    tuDuDum: () -> Unit,
    hasCard: State<Boolean>,
    removeCard: () -> Unit,
    editOnlineMode: (Boolean) -> Unit,
    date: State<LocalDate>,
    changeDate: (LocalDate) -> Unit,
    isDataUpdateNeeded: Boolean,
    isAppUpdateNeeded: Boolean,
    updateData: () -> Unit,
    updateApp: () -> Unit,
    findBusByEvn: (String, (String?) -> Unit) -> Unit,
    findSequences: (String, (List<Pair<String, String>>) -> Unit) -> Unit,
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
                    val time by UtilFunctions.nowFlow.collectAsStateWithLifecycle()
                    if (App.selected != DrawerAction.TransportCard)
                        Text("${time.hour.two()}:${time.minute.two()}:${time.second.two()}", color = MaterialTheme.colorScheme.tertiary)

                    if (App.selected != DrawerAction.TransportCard) IconButton(onClick = {
                        if (!isOnline.value) return@IconButton

                        editOnlineMode(!isOnlineModeEnabled.value)
                    }) {
                        IconWithTooltip(
                            imageVector = if (isOnline.value && isOnlineModeEnabled.value) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = when {
                                isOnline.value && isOnlineModeEnabled.value -> "Online, kliknutím přepnete do offline módu"
                                isOnline.value && !isOnlineModeEnabled.value -> "Offline, kliknutím vypnete offline mód"
                                else -> "Offline, nejste připojeni k internetu"
                            },
                            tint = if (isOnline.value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
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
                        if (App.selected == DrawerAction.TransportCard && hasCard.value) DropdownMenuItem(
                            text = {
                                Text("Odstranit QR kód")
                            },
                            onClick = {
                                removeCard()
                                open = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteForever, null)
                            },
                        )

                        if (App.selected == DrawerAction.FindBus) DropdownMenuItem(
                            text = {
                                Text("Sdílet spoj")
                            },
                            onClick = {
                                val deeplink = "https://jaro-jaro.github.io/DPMCB/${App.route}"
                                ctx.startActivity(Intent.createChooser(Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, deeplink)
                                    type = "text/uri-list"
                                }, "Sdílet spoj"))
                                open = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, null)
                            },
                        )

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
                                                ctx, if (BuildConfig.DEBUG) R.mipmap.logo_chytra_cesta else R.mipmap.logo_chytra_cesta
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
                },
                colors = if (App.title == R.string.empty) TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD73139),
                    navigationIconContentColor = Color.Transparent,
                    actionIconContentColor = Color.White,
                ) else TopAppBarDefaults.topAppBarColors()
            )
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            if (isDataUpdateNeeded) {
                var showDialog by remember { mutableStateOf(true) }

                if (showDialog) AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                                updateData()
                            }
                        ) {
                            Text(stringResource(id = R.string.yes))
                        }
                    },
                    title = {
                        Text(stringResource(id = R.string.data_update))
                    },
                    text = {
                        Text(stringResource(id = R.string.do_you_want_to_update))
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                            }
                        ) {
                            Text(stringResource(id = R.string.no))
                        }
                    },
                )
            }
            if (isAppUpdateNeeded) {
                var showDialog by remember { mutableStateOf(true) }

                if (showDialog) AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                                updateApp()
                            }
                        ) {
                            Text(stringResource(id = R.string.yes))
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
                                showDialog = false
                            }
                        ) {
                            Text(stringResource(id = R.string.no))
                        }
                    },
                )
            }
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        DrawerAction.entries.forEach { action ->
                            VecZeSupliku(
                                action = action,
                                navigate = navigate,
                                startIntent = startIntent,
                                isOnline = isOnline,
                                showToast = showToast,
                                date = date,
                                changeDate = changeDate,
//                                tuDuDum = tuDuDum,
                                closeDrawer = closeDrawer,
                                startActivity = startActivity,
                                findBusByEvn = findBusByEvn,
                                findSequences = findSequences
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

private fun Int.two() = plus(100).toString().takeLast(2)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun VecZeSupliku(
    action: DrawerAction,
    navigate: NavigateFunction,
    startIntent: (Intent) -> Unit,
    isOnline: State<Boolean>,
    showToast: (String, Int) -> Unit,
    date: State<LocalDate>,
    changeDate: (LocalDate) -> Unit,
//    tuDuDum: () -> Unit,
    closeDrawer: () -> Unit,
    startActivity: (KClass<out Activity>) -> Unit,
    findBusByEvn: (String, (String?) -> Unit) -> Unit,
    findSequences: (String, (List<Pair<String, String>>) -> Unit) -> Unit,
) = when (action) {
    DrawerAction.Feedback -> {
        var rating by rememberSaveable { mutableIntStateOf(-1) }
        var showDialog by rememberSaveable { mutableStateOf(false) }

        if (showDialog) AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text("Ohodnotit aplikaci")
            },
            confirmButton = {
                TextButton(onClick = {
                    val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
                    val ref = database.getReference("hodnoceni")
                    ref.push().setValue("${rating + 1}/5")
                    rating = -1
                    showDialog = false
                }) {
                    Text("Odeslat")
                }
            },
            text = {
                Column {
                    Row {
                        repeat(5) { i ->
                            IconButton(onClick = {
                                rating = if (rating == i) -1 else i
                            }, Modifier.weight(1F)) {
                                if (rating >= i)
                                    Icon(imageVector = Icons.Outlined.Star, contentDescription = null, tint = Color.Yellow)
                                else
                                    Icon(imageVector = Icons.Outlined.StarOutline, contentDescription = null, tint = Color.Yellow)
                            }
                        }
                    }
                    Text("Chcete něco dodat? Prosím, obraťte se na náš GitHub, kde s vámi můžeme jednoduše komunikovat, nebo nás kontaktujte osobně. :)")
                    TextButton(onClick = {
                        startIntent(Intent().apply {
                            this.action = Intent.ACTION_VIEW
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
                Text(stringResource(action.label))
            },
            icon = {
                IconWithTooltip(action.icon, stringResource(action.label))
            },
            selected = false,
            onClick = {
                if (isOnline.value)
                    showDialog = true
                else
                    showToast("Jste offline!", Toast.LENGTH_SHORT)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }

    DrawerAction.FindBus -> {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        var isNotFound by rememberSaveable { mutableStateOf(false) }
        var options by rememberSaveable { mutableStateOf(null as List<Pair<String, String>>?) }
        var id by rememberSaveable { mutableStateOf("") }
        var sequence by rememberSaveable { mutableStateOf("") }
        var name by rememberSaveable { mutableStateOf("") }
        var evn by rememberSaveable { mutableStateOf("") }
        var line by rememberSaveable { mutableStateOf("") }
        var number by rememberSaveable { mutableStateOf("") }

        fun confirm(busId: String) {
            navigate(
                BusDestination(
                    busId = busId
                )
            )
            showDialog = false
            closeDrawer()
            id = ""
            sequence = ""
            name = ""
            evn = ""
            line = ""
            number = ""
        }

        fun confirmSeq(seqId: String) {
            navigate(
                SequenceDestination(
                    sequence = seqId
                )
            )
            showDialog = false
            closeDrawer()
            id = ""
            sequence = ""
            options = null
            name = ""
            evn = ""
            line = ""
            number = ""
        }

        fun findSequence(searched: String) = findSequences(searched) {
            if (it.isEmpty()) isNotFound = true
            else if (it.size == 1) confirmSeq(it[0].first)
            else options = it
        }

        if (showDialog) AlertDialog(
            onDismissRequest = {
                showDialog = false
                id = ""
                sequence = ""
                name = ""
                evn = ""
                line = ""
                number = ""
            },
            title = {
                Text(stringResource(id = R.string.find_bus_by_id))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (line.isNotEmpty() && number.isNotEmpty()) confirm(
                        "S-325${
                            when (line.length) {
                                1 -> "00$line"
                                2 -> "0$line"
                                else -> line
                            }
                        }-$number"
                    )
                    else if (evn.isNotEmpty()) {
                        if (!isOnline.value) {
                            showToast("Jste offline", Toast.LENGTH_SHORT)
                            showDialog = false
                            return@TextButton
                        }
                        findBusByEvn(evn) {
                            if (it == null) {
                                showToast("Vůz ev. č. $evn nebyl nalezen.", Toast.LENGTH_LONG)
                                showDialog = false
                                return@findBusByEvn
                            }
                            confirm(it)
                        }
                    } else if (name.isNotEmpty()) confirm("S-${name.replace("/", "-")}")
                    else if (sequence.isNotEmpty()) {
                        findSequence(sequence)
                    } else confirm(id)
                }) {
                    Text("Vyhledat")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    id = ""
                    sequence = ""
                    name = ""
                    line = ""
                    number = ""
                }) {
                    Text("Zrušit")
                }
            },
            text = {
                val focusManager = LocalFocusManager.current
                Column {
                    Row {
                        TextField(
                            value = line,
                            onValueChange = {
                                line = it
                            },
                            Modifier
                                .weight(1F)
                                .padding(end = 8.dp)
                                .autoFocus(),
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
                        TextField(
                            value = number,
                            onValueChange = {
                                number = it
                            },
                            Modifier.weight(1F),
                            label = {
                                Text("Č. spoje")
                            },
                            keyboardActions = KeyboardActions {
                                if (line.isNotEmpty() && number.isNotEmpty())
                                    confirm(
                                        "S-325${
                                            when (line.length) {
                                                1 -> "00$line"
                                                2 -> "0$line"
                                                else -> line
                                            }
                                        }-$number"
                                    )
                                else
                                    focusManager.moveFocus(FocusDirection.Down)
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (line.isNotEmpty() && number.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                                keyboardType = KeyboardType.Number,
                            ),
                        )
                    }
                    TextField(
                        value = evn,
                        onValueChange = {
                            evn = it
                        },
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = {
                            Text("Ev. č. vozu")
                        },
                        keyboardActions = KeyboardActions {
                            if (evn.isNotEmpty()) {
                                if (!isOnline.value) {
                                    showToast("Jste offline", Toast.LENGTH_SHORT)
                                    showDialog = false
                                    return@KeyboardActions
                                }
                                findBusByEvn(evn) {
                                    if (it == null) {
                                        showToast("Vůz ev. č. $evn nebyl nalezen.", Toast.LENGTH_LONG)
                                        showDialog = false
                                        return@findBusByEvn
                                    }
                                    confirm(it)
                                }
                            } else
                                focusManager.moveFocus(FocusDirection.Down)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (evn.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                            keyboardType = KeyboardType.Number,
                        ),
                    )
                    TextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = {
                            Text("Jméno spoje")
                        },
                        keyboardActions = KeyboardActions {
                            if (name.isNotEmpty())
                                confirm("S-${name.replace("/", "-")}")
                            else
                                focusManager.moveFocus(FocusDirection.Down)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (name.isNotEmpty()) ImeAction.Search else ImeAction.Next,
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
                            confirm(id)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search,
                        ),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    TextField(
                        value = sequence,
                        onValueChange = {
                            sequence = it
                        },
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = {
                            Text("Název kurzu")
                        },
                        keyboardActions = KeyboardActions {
                            findSequence(sequence)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search,
                        ),
                    )
                }
            }
        )

        if (isNotFound) AlertDialog(
            onDismissRequest = {
                isNotFound = false
            },
            title = {
                Text("Kurz nenalezen")
            },
            text = {
                Text("Tento kurz ($sequence) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isNotFound = false
                    }
                ) {
                    Text("OK")
                }
            }
        )

        if (options != null) AlertDialog(
            onDismissRequest = {
                options = null
            },
            title = {
                Text("Nalezeno více kurzů")
            },
            text = {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("\"${sequence}\" by mohlo označovat více kurzů, vyberte který jste měli na mysli:")
                    options!!.forEach {
                        HorizontalDivider(Modifier.fillMaxWidth())
                        ListItem(
                            headlineContent = {
                                TextButton(
                                    onClick = {
                                        confirmSeq(it.first)
                                    }
                                ) {
                                    Text(it.second)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        options = null
                    }
                ) {
                    Text("Zrušit")
                }
            }
        )

        NavigationDrawerItem(
            label = {
                Text(stringResource(action.label))
            },
            icon = {
                IconWithTooltip(action.icon, stringResource(action.label))
            },
            selected = App.selected == action,
            onClick = {
                showDialog = true
//                focusRequester.requestFocus()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }

//    DrawerAction.Exit -> {
//        NavigationDrawerItem(
//            label = {
//                Text(stringResource(action.label))
//            },
//            icon = {
//                IconWithTooltip(action.icon, stringResource(action.label))
//            },
//            selected = false,
//            onClick = {
//                tuDuDum()
//            },
//            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
//        )
//    }

    DrawerAction.Date -> Row(
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Používané datum: ${date.value.asString()}",
            modifier = Modifier.padding(all = 16.dp).weight(1F)
        )
        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) DatePickerDialog(
            onDismissRequest = {
                showDialog = false
            },
            onDateChange = {
                changeDate(it)
                showDialog = false
            },
            title = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vybrat nové datum")
                    TextButton(
                        onClick = {
                            changeDate(LocalDate.now())
                            showDialog = false
                        }
                    ) {
                        Text("Dnes")
                    }
                }
            },
            initialDate = date.value,
        )

        IconButton(
            onClick = {
                showDialog = true
            }
        ) {
            IconWithTooltip(Icons.Default.CalendarMonth, "Změnit datum")
        }
    }

    else -> if (action == DrawerAction.NowRunning && date.value != LocalDate.now()) Unit
    else NavigationDrawerItem(
        label = {
            Text(stringResource(action.label))
        },
        icon = {
            IconWithTooltip(action.icon, stringResource(action.label))
        },
        selected = App.selected == action,
        onClick = {
            if (action.multiselect)
                App.selected = action

            action.onClick(
                navigate,
                { closeDrawer() },
                { startActivity(it) },
            )
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}