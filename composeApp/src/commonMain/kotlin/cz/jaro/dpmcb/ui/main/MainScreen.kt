package cz.jaro.dpmcb.ui.main

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get
import androidx.window.core.layout.WindowSizeClass
import cz.jaro.dpmcb.BuildKonfig
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.entities.BusNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.atLeastDigits
import cz.jaro.dpmcb.data.helperclasses.navigateToRouteFunction
import cz.jaro.dpmcb.data.helperclasses.superNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.timeFlow
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.helperclasses.two
import cz.jaro.dpmcb.data.helperclasses.work
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.bus.Bus
import cz.jaro.dpmcb.ui.card.Card
import cz.jaro.dpmcb.ui.chooser.Chooser
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.SimpleTime
import cz.jaro.dpmcb.ui.common.enumTypePair
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import cz.jaro.dpmcb.ui.common.route
import cz.jaro.dpmcb.ui.common.serializationTypePair
import cz.jaro.dpmcb.ui.common.stringSerializationTypePair
import cz.jaro.dpmcb.ui.common.typePair
import cz.jaro.dpmcb.ui.connection.Connection
import cz.jaro.dpmcb.ui.connection.ConnectionPartDefinition
import cz.jaro.dpmcb.ui.connection_results.ConnectionResults
import cz.jaro.dpmcb.ui.connection_search.ConnectionSearch
import cz.jaro.dpmcb.ui.departures.Departures
import cz.jaro.dpmcb.ui.find_bus.FindBus
import cz.jaro.dpmcb.ui.map.Map
import cz.jaro.dpmcb.ui.now_running.NowRunning
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import cz.jaro.dpmcb.ui.sequence.Sequence
import cz.jaro.dpmcb.ui.settings.Settings
import cz.jaro.dpmcb.ui.theme.Colors
import cz.jaro.dpmcb.ui.timetable.Timetable
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
val localDateTypePair = typePair(
    parseValue = {
        if (it == "T") SystemClock.todayHere()
        else LocalDate(
            year = it.substring(0..<4).toInt(),
            month = it.substring(4..<6).toInt(),
            day = it.substring(6..<8).toInt()
        )
    },
    serializeAsValue = {
        "${it.year.atLeastDigits(4)}${it.month.number.atLeastDigits(2)}${it.day.atLeastDigits(2)}"
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
        stringSerializationTypePair<SimpleTime>(),
        serializationTypePair<Boolean?>(),
        serializationTypePair<ShortLine?>(),
        localDateTypePair,
    )

    Route.ConnectionSearch::class -> mapOf(
        localDateTypePair,
        stringSerializationTypePair<SimpleTime?>(),
        serializationTypePair<Boolean?>(),
    )

    Route.ConnectionResults::class -> mapOf(
        localDateTypePair,
        stringSerializationTypePair<SimpleTime>(),
        serializationTypePair<Boolean>(),
    )

    Route.Connection::class -> mapOf(
        localDateTypePair,
        serializationTypePair<List<ConnectionPartDefinition>>(),
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
        stringSerializationTypePair<Direction>(),
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

@OptIn(ExperimentalTime::class)
@Composable
fun Main(
    superNavController: NavHostController,
    args: SuperRoute.Main,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel(
        MainViewModel.Parameters(
            link = args.link,
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
        AppState.menuState = it
        keyboardController?.hide()

        true
    }

    val scope = rememberCoroutineScope()
    val navigator = rememberNavigator(navController)

    LaunchedEffect(Unit) {
        viewModel.navigator = navigator
        viewModel.currentBackStack.value = navController.navigatorProvider[ComposeNavigator::class].backStack
        viewModel.superNavigate = superNavController.superNavigateFunction
        viewModel.confirmDeeplink(
            confirmDeeplink(navController, scope, drawerState),
            navController.navGraphOrNull(),
        )
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
        appVersionToUpdate = args.appVersionToUpdate,
        isDataUpdateNeeded = args.isDataUpdateNeeded,
        onEvent = viewModel::onEvent,
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.ConnectionSearch(SystemClock.todayHere()),
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
            route<Route.ConnectionSearch> { ConnectionSearch(args = it, navigator, superNavController) }
            route<Route.ConnectionResults> { ConnectionResults(args = it, navigator, superNavController) }
            route<Route.Connection> { Connection(args = it, navigator, superNavController) }
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
    appVersionToUpdate: Version?,
    onEvent: (MainEvent) -> Unit,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass.work()

    val widthAtLeastMedium = remember { windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) }
    val widthAtLeastExpanded = remember { windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) }

    val heightAtLeastExpanded = remember { windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) }

    val useModal = remember { !widthAtLeastMedium }
    val useRail = remember { widthAtLeastMedium && !widthAtLeastExpanded }
    val useDrawer = remember { widthAtLeastExpanded }

    val useTopBar = remember { heightAtLeastExpanded || !widthAtLeastMedium }
    val infoInDrawer = remember { !heightAtLeastExpanded && widthAtLeastExpanded }
    val infoInRail = remember { !heightAtLeastExpanded && widthAtLeastMedium && !widthAtLeastExpanded }

    Scaffold(
        topBar = {
            if (useTopBar) TopBar(state, onEvent, useModal)
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    ) { paddingValues ->
        Surface {
            UpdateDialogs(isDataUpdateNeeded, onEvent, appVersionToUpdate)

            if (useDrawer) Drawer(state, onEvent, paddingValues, infoInDrawer, content)
            else if (useRail) Rail(state, onEvent, paddingValues, infoInRail, content)
            else if (useModal) Modal(onEvent, paddingValues, drawerState, content)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBar(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    useModal: Boolean,
) {
    TopAppBar(
        title = {
            Title()
        },
        navigationIcon = {
            NavIcon(state, onEvent, useModal)
        },
        actions = {
            Time()
            OfflineModeSwitcher(state, onEvent)
            OtherOptions(state, onEvent)
        },
        colors = if (AppState.title == "") TopAppBarDefaults.topAppBarColors(
            containerColor = Colors.dpmcb,
            navigationIconContentColor = Color.Transparent,
            actionIconContentColor = Color.White,
        ) else TopAppBarDefaults.topAppBarColors()
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Drawer(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    paddingValues: PaddingValues,
    infoInDrawer: Boolean,
    content: @Composable (() -> Unit),
) = PermanentNavigationDrawer(
    drawerContent = {
        PermanentDrawerSheet(
            Modifier.fillMaxHeight(),
            windowInsets = WindowInsets(0),
        ) {
            if (infoInDrawer) MediumTopAppBar(
                title = {
                    Title()
                },
                navigationIcon = {
                    NavIcon(state, onEvent, false)
                },
                actions = {
                    Time()
                    OfflineModeSwitcher(state, onEvent)
                    OtherOptions(state, onEvent)
                },
                windowInsets = WindowInsets(0),
            )
            LazyColumn {
                items(DrawerAction.entries) { action ->
                    DrawerItem(
                        action = action,
                        onEvent = onEvent,
                    )
                }
            }
        }
    },
    Modifier.padding(paddingValues),
    content = content,
)

@Composable
private fun Modal(
    onEvent: (MainEvent) -> Unit,
    paddingValues: PaddingValues,
    drawerState: DrawerState,
    content: @Composable (() -> Unit),
) = ModalNavigationDrawer(
    drawerContent = {
        ModalDrawerSheet(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            windowInsets = WindowInsets(top = 16.dp),
        ) {
            DrawerAction.entries.forEach { action ->
                DrawerItem(
                    action = action,
                    onEvent = onEvent,
                )
            }
        }
    },
    Modifier.padding(paddingValues),
    drawerState = drawerState,
    content = content,
)

@Composable
private fun Rail(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    paddingValues: PaddingValues,
    infoInRail: Boolean,
    content: @Composable (() -> Unit),
) = Row(
    Modifier
        .fillMaxSize()
        .padding(paddingValues)
) {
    NavigationRail(
        Modifier.fillMaxHeight(),
        windowInsets = WindowInsets.safeGestures.only(WindowInsetsSides.Start),
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (infoInRail) stickyHeader {
                NavIcon(state, onEvent, false)
                Time()
                Row {
                    OfflineModeSwitcher(state, onEvent)
                    OtherOptions(state, onEvent)
                }
            }
            items(DrawerAction.entries) { action ->
                RailItem(
                    action = action,
                    onEvent = onEvent,
                )
            }
        }
    }
    Box(Modifier.weight(1F)) {
        content()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NavIcon(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    useModal: Boolean,
) {
    if (useModal) IconButton(
        onClick = {
            onEvent(MainEvent.ToggleDrawer)
        }
    ) {
        IconWithTooltip(
            imageVector = Icons.Filled.Menu,
            contentDescription = "Otevřít"
        )
    } else if (state.canGoBack) IconButton(
        onClick = {
            onEvent(MainEvent.NavigateBack)
        }
    ) {
        IconWithTooltip(
            imageVector = Icons.AutoMirrored.Default.ArrowBack,
            contentDescription = "Zpět"
        )
    }
}

@Composable
private fun Title() {
    Text(AppState.title)
}

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun UpdateDialogs(
    isDataUpdateNeeded: Boolean,
    onEvent: (MainEvent) -> Unit,
    appVersionToUpdate: Version?,
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
    if (appVersionToUpdate != null) {
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
                Text("Je k dispozici nová verze aplikace (${BuildKonfig.versionName} ➔ $appVersionToUpdate), chcete ji aktualizovat?")
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
}

@Suppress("AssignedValueIsNeverRead")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OtherOptions(state: MainState, onEvent: (MainEvent) -> Unit) {
    var open by remember { mutableStateOf(false) }

    if (AppState.selected == DrawerAction.TransportCard && state.hasCard || supportsShortcuts() || supportsSharing())
        IconButton(onClick = {
            open = !open
        }) {
            IconWithTooltip(imageVector = Icons.Default.MoreVert, contentDescription = "Více možností")
        }

    val shareManager = if (supportsSharing()) screenShareManager else null

    val shortcutCreator = if (supportsShortcuts()) shortcutCreator else null

    var includeDate by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }

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
            DropdownMenuItem(
                text = {
                    Text("Sdílet")
                },
                onClick = {
                    shareManager?.shareScreen(state)
                    open = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Share, null)
                },
            )
        }

        if (supportsShortcuts()) {
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
        }
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
                shortcutCreator?.createShortcut(includeDate, label, state)
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
                if (localDateTypePair.second.serializeAsValue(state.date) in AppState.route) Row(
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OfflineModeSwitcher(state: MainState, onEvent: (MainEvent) -> Unit) {
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
}

@Composable
private fun Time() {
    val time by timeFlow.collectAsStateWithLifecycle()
    if (AppState.selected != DrawerAction.TransportCard)
        Text("${time.hour.two()}:${time.minute.two()}:${time.second.two()}", color = MaterialTheme.colorScheme.tertiary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerItem(
    action: DrawerAction,
    onEvent: (MainEvent) -> Unit,
) {
    if (!action.hide || AppState.selected == action) {
        NavigationDrawerItem(
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
        if (action.hasDivider) HorizontalDivider(Modifier.fillMaxWidth().padding(all = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RailItem(
    action: DrawerAction,
    onEvent: (MainEvent) -> Unit,
) {
    if (!action.hide || AppState.selected == action) {
        NavigationRailItem(
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
            modifier = Modifier
        )
        if (action.hasDivider) HorizontalDivider(Modifier.fillMaxWidth().padding(all = 8.dp))
    }
}

/*@Composable
@OptIn(ExperimentalMaterial3Api::class, KoinInternalApi::class)
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
            val koin = LocalKoinApplication.current.getValue()
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
}*/