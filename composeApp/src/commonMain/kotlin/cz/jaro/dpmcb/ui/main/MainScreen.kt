package cz.jaro.dpmcb.ui.main

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.entities.BusNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.atLeastDigits
import cz.jaro.dpmcb.data.helperclasses.navigateToRouteFunction
import cz.jaro.dpmcb.data.helperclasses.nowFlow
import cz.jaro.dpmcb.data.helperclasses.superNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.two
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.bus.Bus
import cz.jaro.dpmcb.ui.card.Card
import cz.jaro.dpmcb.ui.chooser.Chooser
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.SimpleTime
import cz.jaro.dpmcb.ui.common.enumTypePair
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.common.openWebsiteLauncher
import cz.jaro.dpmcb.ui.common.route
import cz.jaro.dpmcb.ui.common.serializationTypePair
import cz.jaro.dpmcb.ui.common.typePair
import cz.jaro.dpmcb.ui.departures.Departures
import cz.jaro.dpmcb.ui.favourites.Favourites
import cz.jaro.dpmcb.ui.find_bus.FindBus
import cz.jaro.dpmcb.ui.main.MainState.OnlineStatus
import cz.jaro.dpmcb.ui.map.Map
import cz.jaro.dpmcb.ui.now_running.NowRunning
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import cz.jaro.dpmcb.ui.sequence.Sequence
import cz.jaro.dpmcb.ui.settings.Settings
import cz.jaro.dpmcb.ui.theme.dpmcb
import cz.jaro.dpmcb.ui.timetable.Timetable
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.compose.LocalKoinApplication

val localDateTypePair = typePair(
    parseValue = {
        if (it == "T") SystemClock.todayHere()
        else LocalDate(
            year = it.substring(0..<4).toInt(),
            monthNumber = it.substring(4..<6).toInt(),
            dayOfMonth = it.substring(6..<8).toInt(),
        )
    },
    serializeAsValue = {
        "${it.year.atLeastDigits(4)}${it.monthNumber.atLeastDigits(2)}${it.dayOfMonth.atLeastDigits(2)}"
    },
)

inline fun <reified T : Route> typeMap() = when (T::class) {
    Route.Bus::class -> mapOf(
        serializationTypePair<LongLine>(),
        serializationTypePair<BusNumber>(),
        localDateTypePair,
    )

    Route.Chooser::class -> mapOf(
        enumTypePair<ChooserType>(),
        serializationTypePair<ShortLine>(),
        localDateTypePair,
    )

    Route.Departures::class -> mapOf(
        serializationTypePair<SimpleTime>(),
        serializationTypePair<Boolean?>(),
        serializationTypePair<ShortLine?>(),
        localDateTypePair,
    )

    Route.Favourites::class -> mapOf(
        localDateTypePair,
    )

    Route.FindBus::class -> mapOf(
        localDateTypePair,
    )

    Route.NowRunning::class -> mapOf(
        enumTypePair<NowRunningType>(),
        serializationTypePair<List<ShortLine>>(),
        localDateTypePair,
    )

    Route.Sequence::class -> mapOf(
        serializationTypePair<SequenceCode>(),
        localDateTypePair,
    )

    Route.Timetable::class -> mapOf(
        serializationTypePair<ShortLine>(),
        localDateTypePair,
    )

    Route.Card::class -> mapOf(
        localDateTypePair,
    )

    Route.Map::class -> mapOf(
        localDateTypePair,
    )

    Route.FindBus::class -> mapOf(
        localDateTypePair,
    )

    else -> emptyMap()
}

@Composable
fun Main(
    superNavController: NavHostController,
    args: SuperRoute.Main,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel(
        MainViewModel.Parameters(
            link = args.link,
            currentBackStackEntry = navController.currentBackStackEntryFlow,
        )
    ),
) {
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

    val scope = rememberCoroutineScope()
    val navigator = rememberNavigator(navController)

    LaunchedEffect(Unit) {
        viewModel.confirmDeeplink(
            confirmDeeplink(navController, scope, drawerState),
            navController.navGraphOrNull(),
        )
        viewModel.navigator = navigator
        viewModel.superNavigate = superNavController.superNavigateFunction
        viewModel.updateDrawerState = { mutate ->
            val newValue = mutate(drawerState.isOpen)
            scope.launch(Dispatchers.Main) {
                if (newValue) drawerState.open() else drawerState.close()
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            navController.currentBackStackEntryFlow.collect { entry ->
                if (entry.destination.route == null) return@collect
                AppState.route = entry.generateRouteWithArgs() ?: ""
            }
        }
    }

    MainScreen(
        state = state,
        drawerState = drawerState,
        isAppUpdateNeeded = args.isAppDataUpdateNeeded,
        isDataUpdateNeeded = args.isDataUpdateNeeded,
        onEvent = viewModel::onEvent,
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
            route<Route.Favourites> { Favourites(args = it, navigator, superNavController) }
            route<Route.Chooser> { Chooser(args = it, navigator, superNavController) }
            route<Route.Departures> { Departures(args = it, navigator, superNavController) }
            route<Route.NowRunning> { NowRunning(args = it, navigator, superNavController) }
            route<Route.Timetable> { Timetable(args = it, navigator, superNavController) }
            route<Route.Bus> { Bus(args = it, navigator, superNavController) }
            route<Route.Sequence> { Sequence(args = it, navigator, superNavController) }
            route<Route.Card> { Card(args = it, navigator, superNavController) }
            route<Route.Map> { Map(args = it, navigator, superNavController) }
            route<Route.FindBus> { FindBus(args = it, navigator, superNavController) }
            route<Route.Settings> { Settings(args = it, navigator, superNavController) }
        }
    }
}

private fun confirmDeeplink(
    navController: NavHostController,
    scope: CoroutineScope,
    drawerState: DrawerState,
): (String) -> Unit = { path ->
    navController.navigateToRouteFunction(path)
    scope.launch(Dispatchers.Main) {
        drawerState.close()
    }
}

private fun NavHostController.navGraphOrNull(): () -> NavGraph? = {
    try {
        graph
    } catch (_: IllegalStateException) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainState,
    drawerState: DrawerState,
    isDataUpdateNeeded: Boolean,
    isAppUpdateNeeded: Boolean,
    onEvent: (MainEvent) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(AppState.title)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onEvent(MainEvent.ToggleDrawer)
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
                    if (AppState.selected != DrawerAction.TransportCard)
                        Text("${time.hour.two()}:${time.minute.two()}:${time.second.two()}", color = MaterialTheme.colorScheme.tertiary)

                    if (AppState.selected != DrawerAction.TransportCard) IconButton(onClick = {
                        if (state.onlineStatus is MainState.OnlineStatus.Online)
                            onEvent(MainEvent.ToggleOnlineMode)
                    }) {
                        IconWithTooltip(
                            imageVector = if (state.onlineStatus is MainState.OnlineStatus.Online && state.onlineStatus.onlineMode) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = when (state.onlineStatus) {
                                is MainState.OnlineStatus.Online if state.onlineStatus.onlineMode -> "Online, kliknutím přepnete do offline módu"
                                is MainState.OnlineStatus.Online -> "Offline, kliknutím vypnete offline mód"
                                is MainState.OnlineStatus.Offline -> "Offline, nejste připojeni k internetu"
                            },
                            tint = if (state.onlineStatus is MainState.OnlineStatus.Online) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        )
                    }

                    var open by remember { mutableStateOf(false) }

                    if (AppState.selected == DrawerAction.TransportCard && state.hasCard || supportsShortcuts() || supportsSharing())
                        IconButton(onClick = {
                            open = !open
                        }) {
                            IconWithTooltip(imageVector = Icons.Default.MoreVert, contentDescription = "Více možností")
                        }

                    DropdownMenu(
                        expanded = open,
                        onDismissRequest = {
                            open = false
                        },
                        properties = PopupProperties(
                            focusable = false
                        ),
                    ) {
                        if (AppState.selected == DrawerAction.TransportCard && state.hasCard) DropdownMenuItem(
                            text = {
                                Text("Odstranit QR kód")
                            },
                            onClick = {
                                onEvent(MainEvent.RemoveCard)
                                open = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteForever, null)
                            },
                        )

                        if (supportsSharing()) {
                            val shareManager = screenShareManager
                            DropdownMenuItem(
                                text = {
                                    Text("Sdílet")
                                },
                                onClick = {
                                    shareManager.shareScreen(state)
                                    open = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, null)
                                },
                            )
                        }

                        if (supportsShortcuts()) {
                            val shortcutCreator = shortcutCreator

                            var show by remember { mutableStateOf(false) }
                            var includeDate by remember { mutableStateOf(true) }
                            var label by remember { mutableStateOf("") }

                            DropdownMenuItem(
                                text = {
                                    Text("Připnout zkratku na domovskou obrazovku")
                                },
                                onClick = {
                                    label = AppState.title
                                    open = false
                                    show = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PushPin, null)
                                },
                            )
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
                                        shortcutCreator.createShortcut(includeDate, label, state)
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
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = label,
                                            onValueChange = {
                                                label = it
                                            },
                                            Modifier
                                                .fillMaxWidth(),
                                            label = {
                                                Text("Titulek")
                                            },
                                        )
                                        Row(
                                            Modifier.padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Switch(checked = includeDate, onCheckedChange = {
                                                includeDate = it
                                            })
                                            Text(
                                                "Ponechat datum ve zkratce", Modifier
                                                    .clickable {
                                                        includeDate = !includeDate
                                                    }
                                                    .padding(start = 8.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }

                },
                colors = if (AppState.title == "") TopAppBarDefaults.topAppBarColors(
                    containerColor = dpmcb,
                    navigationIconContentColor = Color.Transparent,
                    actionIconContentColor = Color.White,
                ) else TopAppBarDefaults.topAppBarColors()
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    ) { paddingValues ->
        Surface {
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
                                onEvent(MainEvent.UpdateData)
                            }
                        ) {
                            Text("Ano")
                        }
                    },
                    title = {
                        Text("Aktualizace JŘ")
                    },
                    text = {
                        Text("Je k dispozici nová verze jízdních řádů, chcete je aktualizovat?")
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                            }
                        ) {
                            Text("Ne")
                        }
                    },
                )
            }
            if (isAppUpdateNeeded) {
                var showDialog by rememberSaveable { mutableStateOf(true) }
                var loading by rememberSaveable { mutableStateOf(null as String?) }

                if (loading != null) AlertDialog(
                    onDismissRequest = {
                        loading = null
                    },
                    confirmButton = {},
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator()
                            Text(loading!!, Modifier.padding(start = 8.dp))
                        }
                    },
                )

                if (showDialog) AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                                onEvent(MainEvent.UpdateApp { loading = it })
                            }
                        ) {
                            Text("Ano")
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
                            Text("Ne")
                        }
                    },
                )
            }
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet(
                        Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DrawerAction.entries.forEach { action ->
                            DrawerItem(
                                isOnline = state.onlineStatus is OnlineStatus.Online,
                                action = action,
                                onEvent = onEvent,
                            )
                        }
                    }
                },
                Modifier.padding(paddingValues),
                drawerState = drawerState,
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerItem(
    isOnline: Boolean,
    action: DrawerAction,
    onEvent: (MainEvent) -> Unit,
) = when (action) {
    DrawerAction.Feedback -> Feedback(isOnline, action)

    else if action.hide -> {}

    else -> NavigationDrawerItem(
        label = {
            Text(action.label)
        },
        icon = {
            IconWithTooltip(action.icon, action.label)
        },
        selected = AppState.selected == action,
        onClick = {
            onEvent(MainEvent.DrawerItemClicked(action))
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Feedback(
    isOnline: Boolean,
    action: DrawerAction,
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
            val scope = rememberCoroutineScope()
            val koin = LocalKoinApplication.current
            TextButton(onClick = {
                val database = Firebase.database(app = koin.get())
                val ref = database.reference("hodnoceni")
                scope.launch {
                    ref.push().setValue("${rating + 1}/5")
                }
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
                val openWebsite = openWebsiteLauncher
                TextButton(onClick = {
                    openWebsite("https://github.com/jaro-jaro/DPMCB/discussions/133#discussion-5045148")
                }) {
                    Text(text = "Přejít na GitHub")
                }
            }
        }
    )
    NavigationDrawerItem(
        label = {
            Text(action.label)
        },
        icon = {
            IconWithTooltip(action.icon, action.label)
        },
        selected = false,
        onClick = {
            if (isOnline)
                showDialog = true
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}