package frb.axeron.manager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry

import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExecutePluginActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsCategoryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TabsPlaceholderDestination
import com.ramcosta.composedestinations.generated.destinations.ToggleShortcutsScreenDestination
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.navigation.BottomBarDestination
import frb.axeron.manager.ui.navigation.LocalTabSelected
import frb.axeron.manager.ui.screen.DisableAppsScreen
import frb.axeron.manager.ui.screen.FlashIt
import frb.axeron.manager.ui.screen.HomeScreen
import frb.axeron.manager.ui.screen.PrivilegeScreen
import frb.axeron.manager.ui.screen.SettingsScreen
import frb.axeron.manager.ui.screen.plugin.PluginScreen
import frb.axeron.manager.ui.theme.AxManagerTheme
import frb.axeron.manager.ui.util.LocalBottomBarHidden
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.util.LocaleHelper
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.viewmodel.DisableAppsViewModel
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel
import frb.axeron.manager.ui.viewmodel.QuickShellViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.manager.ui.viewmodel.ToggleViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.server.PluginInstaller
import kotlinx.coroutines.launch

class AxActivity : ComponentActivity() {

    companion object {
        const val OPEN_QUICK_SHELL = "Axora.QUICK_SHELL"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    var zipUri by mutableStateOf<List<PluginInstaller>?>(emptyList())

    val shortcut by lazy {
        ShortcutInfoCompat.Builder(this, "quick-shell")
            .setShortLabel(getString(R.string.quick_shell_short))
            .setLongLabel(getString(R.string.quick_shell))
            .setDisabledMessage(getString(R.string.quick_shell_not_supported))
            .setIcon(IconCompat.createWithResource(this, R.drawable.terminal))
            .setIntent(
                Intent(this, AxActivity::class.java).apply {
                    action = OPEN_QUICK_SHELL
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            .build()
    }

    private var intentState: Intent? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ShortcutManagerCompat.pushDynamicShortcut(this@AxActivity, shortcut)

        intentState = intent

        setContent {
            AxManagerTheme {
                MainScreen(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentState = intent
        Log.i("AxActivity", "onNewIntent")
    }

    @Composable
    fun MainScreen(settingsViewModel: SettingsViewModel) {
        val snackBarHostState = remember { SnackbarHostState() }
        val navController = rememberNavController()
        val navigator = navController.rememberDestinationsNavigator()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination



        val activateViewModel: ActivateViewModel = viewModel<ActivateViewModel>()

        val appsViewModel: AppsViewModel = viewModel<AppsViewModel>()
        val disableAppsViewModel: DisableAppsViewModel = viewModel<DisableAppsViewModel>()
        val privilegeViewModel: PrivilegeViewModel = viewModel<PrivilegeViewModel>()
        val pluginViewModel: PluginViewModel = viewModel<PluginViewModel>()
        val quickShellViewModel: QuickShellViewModel = viewModel<QuickShellViewModel>()
        val toggleViewModel: ToggleViewModel = viewModel<ToggleViewModel>()

        val axeronInfo = activateViewModel.axeronInfo
        val isShizukuActive = activateViewModel.isShizukuActive

        val viewModelGlobal = remember {
            ViewModelGlobal(
                settingsViewModel = settingsViewModel,
                appsViewModel = appsViewModel,
                disableAppsViewModel = disableAppsViewModel,
                activateViewModel = activateViewModel,
                pluginViewModel = pluginViewModel,
                privilegeViewModel = privilegeViewModel,
                quickShellViewModel = quickShellViewModel,
                toggleViewModel = toggleViewModel
            )
        }

        val visibleTabs = remember(axeronInfo, isShizukuActive) {
            BottomBarDestination.entries.filter { dest ->
                (axeronInfo.isRunning() || !dest.needAxeron) &&
                    !(dest == BottomBarDestination.Privilege && !isShizukuActive)
            }
        }

        val pagerState = rememberPagerState(pageCount = { visibleTabs.size })
        val scope = rememberCoroutineScope()

        val onTabClick: (BottomBarDestination) -> Unit = { dest ->
            val index = visibleTabs.indexOf(dest)
            if (index >= 0) {
                if (currentDestination?.route != TabsPlaceholderDestination.route) {
                    navController.popBackStack(TabsPlaceholderDestination.route, inclusive = false)
                }
                scope.launch { pagerState.scrollToPage(index) }
            }
        }

        LaunchedEffect(visibleTabs.size) {
            if (pagerState.currentPage > visibleTabs.size - 1) {
                pagerState.scrollToPage(visibleTabs.size - 1)
            }
        }

        LaunchedEffect(intentState) {
            if (intentState == null) return@LaunchedEffect
            Log.i("AxActivity", "intent: $intentState")
            when (intentState!!.action) {
                OPEN_QUICK_SHELL -> {
                    if (axeronInfo.isRunning()) {
                        navController.popBackStack(
                            TabsPlaceholderDestination.route,
                            inclusive = false
                        )
                        scope.launch {
                            val homeIndex = visibleTabs.indexOf(BottomBarDestination.Home)
                            if (homeIndex >= 0) pagerState.scrollToPage(homeIndex)
                            navigator.navigate(QuickShellScreenDestination())
                        }
                    }
                }

                Intent.ACTION_VIEW -> {
                    if (intentState!!.data?.scheme == "content") {
                        zipUri =
                            intent.data?.let { uri ->
                                arrayListOf(PluginInstaller(uri))
                            } ?: run {
                                val uris: ArrayList<Uri>? =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        intent.getParcelableArrayListExtra("uris", Uri::class.java)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        intent.getParcelableArrayListExtra("uris")
                                    }

                                uris
                                    ?.map { PluginInstaller(it) }
                                    ?.toCollection(arrayListOf())
                                    ?: arrayListOf()
                            }
                        if (!zipUri.isNullOrEmpty()) {
                            navigator.navigate(
                                FlashScreenDestination(
                                    flashIt = FlashIt.FlashPlugins(zipUri!!)
                                )
                            )
                            zipUri = null
                        }
                    }
                }
            }

            intentState = null
        }

        LaunchedEffect(axeronInfo) {
            if (axeronInfo.isRunning()) {
                pluginViewModel.fetchModuleList()
                appsViewModel.loadInstalledApps()
                disableAppsViewModel.mergeNew()
            }
        }

        LaunchedEffect(axeronInfo, isShizukuActive) {
            if (axeronInfo.isRunning() && isShizukuActive) {
                privilegeViewModel.loadInstalledApps(refresh = false)
            }
        }

        val showBottomBar = when (currentDestination?.route) {
            ActivateScreenDestination.route -> false
            FlashScreenDestination.route -> false
            ExecutePluginActionScreenDestination.route -> false
            SettingsCategoryScreenDestination.route -> false
            ToggleShortcutsScreenDestination.route -> false
            else -> true
        }

        val bottomBarHidden = remember { mutableStateOf(false) }

        LaunchedEffect(currentDestination?.route) {
            bottomBarHidden.value = false
        }

        val selectedTab = visibleTabs.getOrElse(pagerState.currentPage) {
            BottomBarDestination.Home
        }

        val pluginUpdateCount by pluginViewModel.updateCount.collectAsState()
        Box {
            CompositionLocalProvider(
                LocalSnackbarHost provides snackBarHostState,
                LocalBottomBarHidden provides bottomBarHidden,
                LocalTabSelected provides onTabClick,
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            key = { page -> visibleTabs.getOrNull(page) ?: page },
                            userScrollEnabled = currentDestination?.route == TabsPlaceholderDestination.route
                        ) { page ->
                            when (visibleTabs.getOrNull(page)) {
                                BottomBarDestination.Home -> HomeScreen(navigator, viewModelGlobal)
                                BottomBarDestination.Apps -> DisableAppsScreen(navigator, viewModelGlobal)
                                BottomBarDestination.Privilege -> PrivilegeScreen(navigator, viewModelGlobal)
                                BottomBarDestination.Plugin -> PluginScreen(navigator, viewModelGlobal)
                                BottomBarDestination.Settings -> SettingsScreen(navigator, viewModelGlobal)
                                null -> Unit
                            }
                        }

                        DestinationsNavHost(
                            modifier = Modifier.fillMaxSize(),
                            navGraph = NavGraphs.root,
                            navController = navController,
                            dependenciesContainerBuilder = {
                                dependency(viewModelGlobal)
                            },
                            defaultTransitions = remember {
                                createPushTransitions()
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showBottomBar && !bottomBarHidden.value,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        BottomBar(
                            tabs = visibleTabs,
                            selected = selectedTab,
                            onTabClick = onTabClick,
                            moduleUpdateCount = pluginUpdateCount,
                            bottomBarScale = settingsViewModel.bottomBarScale
                        )
                    }
                }
            }
        }
    }

    private fun createPushTransitions(): NavHostAnimatedDestinationStyle {
        return object : NavHostAnimatedDestinationStyle() {
            override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                slideInHorizontally(initialOffsetX = { it })
            }

            override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
            }

            override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
            }

            override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        }
    }

    @Composable
    fun BottomBar(
        tabs: List<BottomBarDestination>,
        selected: BottomBarDestination,
        onTabClick: (BottomBarDestination) -> Unit,
        moduleUpdateCount: Int,
        bottomBarScale: Float
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(
                    bottom = 48.dp * bottomBarScale,
                    start = 16.dp * bottomBarScale,
                    end = 16.dp * bottomBarScale
                )
        ) {
            // A real pill: percent = 50 keeps every corner fully rounded regardless of the bar's
            // height or the scale animation. The previous fixed 20dp radius was smaller than half
            // the row's height, so the ends only ever looked mildly rounded - a rounded rectangle
            // rather than a capsule. Surface also gives the bar real tonal elevation so it sits
            // above the content instead of being a flat painted rectangle.
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 8.dp * bottomBarScale,
                        vertical = 6.dp * bottomBarScale
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp * bottomBarScale)
                ) {
                    tabs.forEach { destination ->
                        val isSelected = destination == selected
                        val label = stringResource(id = destination.labelId)

                        // The item is a fixed square (icon + uniform padding), so a circle
                        // indicator always stays a circle at any scale. A 12dp rounded rect
                        // read as a lopsided blob against the pill's own curvature.
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                                .clickable { onTabClick(destination) }
                                .padding(12.dp * bottomBarScale),
                            contentAlignment = Alignment.Center
                        ) {
                            if (destination == BottomBarDestination.Plugin && moduleUpdateCount > 0) {
                                BadgedBox(badge = { Badge { Text(moduleUpdateCount.toString()) } }) {
                                    Icon(
                                        imageVector = if (isSelected) destination.iconSelected else destination.iconNotSelected,
                                        contentDescription = label,
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp * bottomBarScale)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) destination.iconSelected else destination.iconNotSelected,
                                    contentDescription = label,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp * bottomBarScale)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
