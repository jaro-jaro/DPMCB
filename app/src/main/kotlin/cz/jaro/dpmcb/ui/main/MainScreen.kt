package cz.jaro.dpmcb.ui.main

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.database.database
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.LoadingActivity
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.entities.BusNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.nowFlow
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.two
import cz.jaro.dpmcb.ui.bus.Bus
import cz.jaro.dpmcb.ui.card.Card
import cz.jaro.dpmcb.ui.chooser.Chooser
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.SimpleTime
import cz.jaro.dpmcb.ui.common.enumTypePair
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.common.getRoute
import cz.jaro.dpmcb.ui.common.route
import cz.jaro.dpmcb.ui.common.serializationTypePair
import cz.jaro.dpmcb.ui.departures.Departures
import cz.jaro.dpmcb.ui.favourites.Favourites
import cz.jaro.dpmcb.ui.find_bus.FindBus
import cz.jaro.dpmcb.ui.map.Map
import cz.jaro.dpmcb.ui.now_running.NowRunning
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import cz.jaro.dpmcb.ui.sequence.Sequence
import cz.jaro.dpmcb.ui.timetable.Timetable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

inline fun <reified T : Route> typeMap() = when (T::class) {
    Route.Bus::class -> mapOf(
        serializationTypePair<LongLine>(),
        serializationTypePair<BusNumber>(),
        serializationTypePair<LocalDate>(),
    )

    Route.Chooser::class -> mapOf(
        enumTypePair<ChooserType>(),
        serializationTypePair<ShortLine>(),
        serializationTypePair<LocalDate>(),
    )

    Route.Departures::class -> mapOf(
        serializationTypePair<SimpleTime>(),
        serializationTypePair<Boolean?>(),
        serializationTypePair<ShortLine?>(),
        serializationTypePair<LocalDate>(),
    )

    Route.Favourites::class -> mapOf(
        serializationTypePair<LocalDate>(),
    )

    Route.FindBus::class -> mapOf(
        serializationTypePair<LocalDate>(),
    )

    Route.NowRunning::class -> mapOf(
        enumTypePair<NowRunningType>(),
        serializationTypePair<List<ShortLine>>(),
        serializationTypePair<LocalDate>(),
    )

    Route.Sequence::class -> mapOf(
        serializationTypePair<SequenceCode>(),
        serializationTypePair<LocalDate>(),
    )

    Route.Timetable::class -> mapOf(
        serializationTypePair<ShortLine>(),
        serializationTypePair<LocalDate>(),
    )

    Route.Card::class -> mapOf(
        serializationTypePair<LocalDate>(),
    )

    Route.Map::class -> mapOf(
        serializationTypePair<LocalDate>(),
    )

    Route.FindBus::class -> mapOf(
        serializationTypePair<LocalDate>(),
    )

    else -> emptyMap()
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
        navController.enableOnBackPressed(true)
        val destinationFlow = navController.currentBackStackEntryFlow

        destinationFlow.collect { entry ->
            Firebase.analytics.logEvent("navigation") {
                param("route", entry.generateRouteWithArgs() ?: "")
                param("destination", entry.destination.route?.split("/", "?", limit = 2)?.first() ?: "")
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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            navController.currentBackStackEntryFlow.collect { entry ->
                if (entry.destination.route == null) return@collect
                App.route = entry.generateRouteWithArgs() ?: ""
            }
        }
    }

    MainScreen(
        drawerState = drawerState,
        toggleDrawer = {
            keyboardController?.hide()
            if (drawerState.isClosed) openDrawer() else closeDrawer()
        },
        closeDrawer = closeDrawer,
        isOnline = isOnline,
        isOnlineModeEnabled = isOnlineModeEnabled,
        navigate = navController.navigateFunction,
        editOnlineMode = viewModel.editOnlineMode,
        isAppUpdateNeeded = isAppUpdateNeeded,
        updateApp = viewModel.updateApp,
        isDataUpdateNeeded = isDataUpdateNeeded,
        updateData = viewModel.updateData,
        removeCard = viewModel.removeCard,
        hasCard = hasCard,
        getDate = {
            navController.currentBackStackEntry?.getRoute()?.date ?: SystemClock.todayHere()
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.Favourites,
            popEnterTransition = {
                scaleIn(
                    animationSpec = tween(
                        durationMillis = 100,
                        delayMillis = 35,
                    ),
                    initialScale = 1.1F,
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 100,
                        delayMillis = 35,
                    ),
                )
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 0.9F,
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 35,
                        easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f),
                    ),
                )
            },
        ) {
            route<Route.Favourites> { Favourites(args = it, navController = navController) }
            route<Route.Chooser> { Chooser(args = it, navController = navController) }
            route<Route.Departures> { Departures(args = it, navController = navController) }
            route<Route.NowRunning> { NowRunning(args = it, navController = navController) }
            route<Route.Timetable> { Timetable(args = it, navController = navController) }
            route<Route.Bus> { Bus(args = it, navController = navController) }
            route<Route.Sequence> { Sequence(args = it, navController = navController) }
            route<Route.Card> { Card(args = it, navController = navController) }
            route<Route.Map> { Map(args = it, navController = navController) }
            route<Route.FindBus> { FindBus(args = it, navController = navController) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    drawerState: DrawerState,
    toggleDrawer: () -> Unit,
    closeDrawer: () -> Unit,
    isOnline: State<Boolean>,
    isOnlineModeEnabled: State<Boolean>,
    navigate: NavigateFunction,
    hasCard: State<Boolean>,
    removeCard: () -> Unit,
    editOnlineMode: (Boolean) -> Unit,
    isDataUpdateNeeded: Boolean,
    isAppUpdateNeeded: Boolean,
    updateData: () -> Unit,
    updateApp: () -> Unit,
    getDate: () -> LocalDate,
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
                    val time by nowFlow.collectAsStateWithLifecycle()
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

                        DropdownMenuItem(
                            text = {
                                Text("Sdílet")
                            },
                            onClick = {
                                val deeplink = "https://jaro-jaro.github.io/DPMCB/${App.route}"
                                ctx.startActivity(Intent.createChooser(Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, deeplink)
                                    type = "text/uri-list"
                                }, "Sdílet"))
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

                                    val pinShortcutInfo = ShortcutInfo
                                        .Builder(ctx, "${App.route}-$label")
                                        .setShortLabel(label)
                                        .setLongLabel(label)
                                        .setIcon(
                                            android.graphics.drawable.Icon.createWithResource(
                                                ctx, if (BuildConfig.DEBUG) R.mipmap.logo_jaro else R.mipmap.logo_chytra_cesta
                                            )
                                        )
                                        .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://jaro-jaro.github.io/DPMCB/${App.route}")))
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
                            OutlinedTextField(
                                value = label,
                                onValueChange = {
                                    label = it
                                },
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                label = {
                                    Text("Titulek")
                                },
                            )
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
                var showDialog by rememberSaveable { mutableStateOf(true) }

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
                var showDialog by rememberSaveable { mutableStateOf(true) }

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
                    ModalDrawerSheet(
                        Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        DrawerAction.entries.forEach { action ->
                            DrawerItem(
                                action = action,
                                navigate = navigate,
                                isOnline = isOnline,
                                closeDrawer = closeDrawer,
                                getDate = getDate,
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
    isOnline: State<Boolean>,
    closeDrawer: () -> Unit,
    getDate: () -> LocalDate,
) = when (action) {
    DrawerAction.Feedback -> Feedback(action, isOnline)

    else -> {
        val ctx = LocalContext.current
        NavigationDrawerItem(
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

                action.route?.let {
                    navigate(it(getDate()))
                }
                action.activity?.let {
                    ctx.startActivity(Intent(ctx, it.java).setAction(Intent.ACTION_MAIN))
                }
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Feedback(
    action: DrawerAction,
    isOnline: State<Boolean>,
) {
    var rating by rememberSaveable { mutableIntStateOf(-1) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current

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
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(ctx, Uri.parse("https://github.com/jaro-jaro/DPMCB/discussions/133#discussion-5045148"))
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
                Toast.makeText(ctx, "Jste offline!", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}