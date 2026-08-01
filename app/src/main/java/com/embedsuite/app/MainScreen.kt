package com.embedsuite.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.FirmwareProfile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embedsuite.app.notifications.EmbedNotificationHelper
import com.embedsuite.app.field.FieldOperationManager
import com.embedsuite.app.ui.components.*
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.EmbedViewModelFactory
import kotlinx.coroutines.launch

data class NavTab(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
private fun mainTabs() = listOf(
    NavTab("dashboard", R.string.nav_home, Icons.Default.Home),
    NavTab("rf", R.string.nav_rf, Icons.Default.SettingsInputAntenna),
    NavTab("wireless", R.string.nav_wifi, Icons.Default.Wifi),
    NavTab("nfc_ir", R.string.nav_nfc, Icons.Default.Nfc),
    NavTab("terminal", R.string.nav_cli, Icons.Default.Terminal),
    NavTab("ai", R.string.nav_ai, Icons.Default.Psychology),
    NavTab("map_tools", R.string.nav_tools, Icons.Default.Map)
)

@Composable
fun MainScreen(
    container: AppContainer,
    deepLink: DeepLinkParams? = null
) {
    var showSplash by remember { mutableStateOf(!container.appPreferences.splashShown) }
    var showOnboarding by remember { mutableStateOf(!container.appPreferences.onboardingComplete) }
    var showPermissions by remember { mutableStateOf(!container.appPreferences.permissionsComplete) }

    if (showSplash) {
        SplashScreen(onFinished = {
            container.appPreferences.splashShown = true
            showSplash = false
        })
        return
    }

    if (showOnboarding) {
        OnboardingScreen(onComplete = {
            container.appPreferences.onboardingComplete = true
            showOnboarding = false
        })
        return
    }

    if (showPermissions) {
        PermissionsFlowScreen(onComplete = {
            container.appPreferences.permissionsComplete = true
            showPermissions = false
        }, modifier = Modifier.background(BlackAMOLED))
        return
    }

    val navController = rememberNavController()
    val viewModelFactory = remember { EmbedViewModelFactory(container) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    val connectionState by container.connectionManager.connectionState.collectAsState()
    val detectedProfile by container.connectionManager.detectedProfile.collectAsState()
    val scanlinesEnabled by container.appPreferences.scanlinesEnabled.collectAsState()
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    var showTehLinkPairingGuide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(connectionState, detectedProfile) {
        if (connectionState is ConnectionState.Connected &&
            detectedProfile == FirmwareProfile.XIBALBA &&
            !container.appPreferences.tehLinkPairingGuideSeen
        ) {
            showTehLinkPairingGuide = true
        }
    }

    if (showTehLinkPairingGuide) {
        TehLinkPairingDialog(
            isMockMode = container.appPreferences.useMockTransport,
            onDismiss = {
                container.appPreferences.tehLinkPairingGuideSeen = true
                showTehLinkPairingGuide = false
            },
            onSimulateLongPress = if (container.appPreferences.useMockTransport) {
                {
                    container.connectionManager.simulateMockLongPress()
                    scope.launch {
                        container.connectionManager.rePairTehLink()
                    }
                }
            } else null
        )
    }

    LaunchedEffect(Unit) {
        FieldOperationManager.isActiveFlow.collect { active ->
            if (active && container.appPreferences.fieldKeepScreenOn) {
                activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val tabs = mainTabs()
    val isSettings = currentRoute == "settings" || currentRoute == "about" || currentRoute == "hardware_bringup" || currentRoute == "manual"
    val isMainTab = tabs.any { it.route == currentRoute }
    val fieldActive by FieldOperationManager.isActiveFlow.collectAsState()

    LaunchedEffect(currentRoute, fieldActive) {
        if (currentRoute != "wireless") {
            container.wirelessScanner.stopBleScan()
        }
        if (currentRoute != "map_tools" && !fieldActive) {
            container.locationTracker.stopTracking()
        }
    }

    // Batería: al ir a segundo plano detener BLE/GPS (modo campo sigue en FGS)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentRoute, fieldActive) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    container.wirelessScanner.stopBleScan()
                    if (!fieldActive) {
                        container.locationTracker.stopTracking()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val state = container.connectionManager.connectionState.value
        if (state is ConnectionState.Disconnected || state is ConnectionState.Error) {
            container.connectionManager.connect(container.appPreferences.defaultTransport.value)
        }
    }

    LaunchedEffect(deepLink?.token) {
        deepLink?.route?.let { route ->
            val target = if (route == "rf") "rf" else route
            if (tabs.any { it.route == target } || target == "settings") {
                navController.navigate(target) {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    var lastNotifiedState by remember { mutableStateOf<ConnectionState?>(null) }

    LaunchedEffect(connectionState) {
        if (connectionState != lastNotifiedState) {
            when (connectionState) {
                is ConnectionState.Connected -> EmbedNotificationHelper.notifyConnection(
                    context,
                    context.getString(R.string.notif_connected_title),
                    context.getString(R.string.notif_connected_body, (connectionState as ConnectionState.Connected).type.name)
                )
                ConnectionState.Disconnected -> if (lastNotifiedState is ConnectionState.Connected) {
                    EmbedNotificationHelper.notifyConnection(
                        context,
                        context.getString(R.string.notif_disconnected_title),
                        context.getString(R.string.notif_disconnected_body)
                    )
                }
                else -> {}
            }
            lastNotifiedState = connectionState
        }
    }

    val (statusText, statusColor) = when (connectionState) {
        ConnectionState.Disconnected -> stringResource(R.string.status_offline) to NeonRed
        ConnectionState.Connecting -> stringResource(R.string.status_sync) to NeonOrange
        is ConnectionState.Connected -> stringResource(R.string.status_link_ok) to MatrixGreen
        is ConnectionState.Error -> stringResource(R.string.status_err) to NeonRed
    }

    Box(Modifier.fillMaxSize().background(BlackAMOLED)) {
        GlassBackground()
        if (scanlinesEnabled) ScanlineOverlay(Modifier.fillMaxSize())

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (!isSettings) {
                    EmbedTopBar(
                        title = stringResource(R.string.app_name),
                        subtitle = stringResource(R.string.topbar_subtitle),
                        statusText = statusText,
                        statusColor = statusColor,
                        onSettingsClick = {
                            navController.navigate("settings") { launchSingleTop = true }
                        }
                    )
                }
            },
            bottomBar = {
                if (isMainTab) {
                    Column(Modifier.background(DarkSurface)) {
                        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.25f), thickness = 1.dp)
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    label = {
                                        Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                    },
                                    icon = { Icon(tab.icon, label, Modifier.size(22.dp)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MatrixGreen, selectedTextColor = MatrixGreen,
                                        unselectedIconColor = TextGray, unselectedTextColor = TextGray,
                                        indicatorColor = MatrixGreen.copy(alpha = 0.18f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = "dashboard", Modifier.padding(innerPadding).fillMaxSize()) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        connectionManager = container.connectionManager,
                        rfReplayEngine = container.rfReplayEngine,
                        sessionReportGenerator = container.sessionReportGenerator,
                        appPreferences = container.appPreferences,
                        onNavigateRf = { navController.navigate("rf") },
                        onNavigateTools = { navController.navigate("map_tools") },
                        onNavigateHardwareBringup = {
                            navController.navigate("hardware_bringup") { launchSingleTop = true }
                        }
                    )
                }
                composable("rf") {
                    RfHubScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        signalRepository = container.signalRepository,
                        connectionManager = container.connectionManager,
                        rfReplayEngine = container.rfReplayEngine,
                        aiEngine = container.aiEngine,
                        initialTab = deepLink?.rfTab,
                        highlightSignalId = deepLink?.signalId
                    )
                }
                composable("wireless") {
                    WirelessScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        sessionStats = container.sessionStats,
                        onSaveDevice = { device ->
                            val (lat, lng) = container.locationTracker.currentLatLng()
                            container.signalRepository.saveWirelessDevice(device, lat, lng)
                            if (device.type == "WIFI") container.sessionStats.incrementAps()
                        }
                    )
                }
                composable("nfc_ir") {
                    NfcIrScreen(viewModel = viewModel(factory = viewModelFactory))
                }
                composable("terminal") {
                    ConsoleScreen(viewModel = viewModel(factory = viewModelFactory))
                }
                composable("ai") {
                    AiAssistantScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        activeTab = currentRoute,
                        secureStoreAvailable = container.secureStore.isAvailable
                    )
                }
                composable("map_tools") {
                    MapToolsScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        connectionManager = container.connectionManager,
                        locationTracker = container.locationTracker,
                        flashCoordinator = container.firmwareFlashCoordinator,
                        macroRepository = container.macroRepository,
                        macroEngine = container.macroEngine,
                        profileRepository = container.profileRepository,
                        rfAutomationRepository = container.rfAutomationRepository,
                        mapTileCacheManager = container.mapTileCacheManager,
                        bruceStorageSync = container.bruceStorageSync,
                        signalRepository = container.signalRepository,
                        irRepository = container.irRepository,
                        nfcDumpRepository = container.nfcDumpRepository
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        preferences = container.appPreferences,
                        connectionManager = container.connectionManager,
                        onBack = { navController.popBackStack() },
                        onNavigateAbout = {
                            navController.navigate("about") { launchSingleTop = true }
                        },
                        onResetOnboarding = {
                            (context as? androidx.activity.ComponentActivity)?.recreate()
                        },
                        onLanguageChanged = {
                            (context as? androidx.activity.ComponentActivity)?.recreate()
                        },
                        onNavigateHardwareBringup = {
                            navController.navigate("hardware_bringup") { launchSingleTop = true }
                        }
                    )
                }
                composable("about") {
                    AboutScreen(
                        onBack = { navController.popBackStack() },
                        onOpenManual = {
                            navController.navigate("manual") { launchSingleTop = true }
                        }
                    )
                }
                composable("manual") {
                    ManualScreen(onBack = { navController.popBackStack() })
                }
                composable("hardware_bringup") {
                    HardwareBringupScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
