package cz.jaro.dpmcb.ui.main

import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.marosseleng.compose.material3.datetimepickers.date.ui.dialog.DatePickerDialog
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.LoadingActivity
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.two
import cz.jaro.dpmcb.ui.bus.Bus
import cz.jaro.dpmcb.ui.card.Card
import cz.jaro.dpmcb.ui.chooser.Chooser
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.favourites.Favourites
import cz.jaro.dpmcb.ui.map.Map
import cz.jaro.dpmcb.ui.now_running.NowRunning
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import cz.jaro.dpmcb.ui.sequence.Sequence
import cz.jaro.dpmcb.ui.timetable.Timetable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

inline fun <reified T> getKotlinxSerializationType(
    serializer: KSerializer<T> = serializer(),
) = typeOf<T>() to getKotlinxSerializationNavType<T>(
    serializer = serializer
)

inline fun <reified T> getKotlinxSerializationNavType(
    serializer: KSerializer<T> = serializer(),
) = object : NavType<T>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): T? =
        bundle.getString(key)?.let(::parseValue)

    override fun put(bundle: Bundle, key: String, value: T) =
        bundle.putString(key, serializeAsValue(value))

    override fun parseValue(value: String) = Json.decodeFromString(serializer, value)

    override fun serializeAsValue(value: T) = Json.encodeToString(serializer, value)

    override val name: String = T::class.java.simpleName
}

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
        val destinationFlow = navController.currentBackStackEntryFlow

        destinationFlow.collect { destination ->
            Firebase.analytics.logEvent("navigation") {
                param("route", destination.destination.route ?: "")
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
                App.route = entry.destination.route ?: ""
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
        NavHost(
            navController = navController,
            startDestination = Route.Favourites,
        ) {
            composable<Route.Favourites> {
                val args = it.toRoute<Route.Favourites>()
                Favourites(args = args, navController = navController)
            }
            composable<Route.Chooser>(
                typeMap = mapOf(
                    getKotlinxSerializationType<ChooserType>()
                )
            ) {
                val args = it.toRoute<Route.Chooser>()
                Chooser(args = args, navController = navController)
            }
//            composable<Route.Departures>(
//                typeMap = mapOf(
//                    getKotlinxSerializationType<@Serializable(with = NullableLocalTimeSerializer::class) LocalTime?>(serializer = NullableLocalTimeSerializer())
//                )
//            ) {
//                val args = it.toRoute<Route.Departures>()
//                Departures(args = args, navController = navController)
//            }
            composable<Route.NowRunning>(
                typeMap = mapOf(
                    getKotlinxSerializationType<NowRunningType>()
                )
            ) {
                val args = it.toRoute<Route.NowRunning>()
                NowRunning(args = args, navController = navController)
            }
            composable<Route.Timetable> {
                val args = it.toRoute<Route.Timetable>()
                Timetable(args = args, navController = navController)
            }
            composable<Route.Bus> {
                val args = it.toRoute<Route.Bus>()
                Bus(args = args, navController = navController)
            }
            composable<Route.Sequence> {
                val args = it.toRoute<Route.Sequence>()
                Sequence(args = args, navController = navController)
            }
            composable<Route.Card> {
                val args = it.toRoute<Route.Card>()
                Card(args = args, navController = navController)
            }
            composable<Route.Map> {
                val args = it.toRoute<Route.Map>()
                Map(args = args, navController = navController)
            }
        }
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
                                                ctx, if (BuildConfig.DEBUG) R.mipmap.logo_jaro else R.mipmap.logo_chytra_cesta
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
                            DrawerItem(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerItem(
    action: DrawerAction,
    navigate: NavigateFunction,
    startIntent: (Intent) -> Unit,
    isOnline: State<Boolean>,
    showToast: (String, Int) -> Unit,
    date: State<LocalDate>,
    changeDate: (LocalDate) -> Unit,
    closeDrawer: () -> Unit,
    startActivity: (KClass<out Activity>) -> Unit,
    findBusByEvn: (String, (String?) -> Unit) -> Unit,
    findSequences: (String, (List<Pair<String, String>>) -> Unit) -> Unit,
) = when (action) {
    DrawerAction.Feedback -> Feedback(startIntent, action, isOnline, showToast)

    DrawerAction.FindBus -> FindBus(navigate, closeDrawer, findSequences, isOnline, showToast, findBusByEvn, action)

    DrawerAction.Date -> ChangeDate(date, changeDate)

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

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
private fun ChangeDate(date: State<LocalDate>, changeDate: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Používané datum: ${date.value.asString()}",
            modifier = Modifier
                .padding(all = 16.dp)
                .weight(1F)
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
                    Text("Změnit datum")
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
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FindBus(
    navigate: NavigateFunction,
    closeDrawer: () -> Unit,
    findSequences: (String, (List<Pair<String, String>>) -> Unit) -> Unit,
    isOnline: State<Boolean>,
    showToast: (String, Int) -> Unit,
    findBusByEvn: (String, (String?) -> Unit) -> Unit,
    action: DrawerAction,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    FindBusDialog(
        showDialog = showDialog,
        onDismiss = { showDialog = false },
        navigate = navigate,
        closeDrawer = closeDrawer,
        findSequences = findSequences,
        isOnline = isOnline,
        showToast = showToast,
        findBusByEvn = findBusByEvn
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
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Feedback(
    startIntent: (Intent) -> Unit,
    action: DrawerAction,
    isOnline: State<Boolean>,
    showToast: (String, Int) -> Unit,
) {
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