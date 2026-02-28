package com.example.multilink.ui.navigation

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.multilink.repo.AuthRepository
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.service.LocationService
import com.example.multilink.ui.auth.InfoInputScreen
import com.example.multilink.ui.tracker.DetailScreen
import com.example.multilink.ui.main.HomeScreen
import com.example.multilink.ui.tracker.LiveTrackingScreen
import com.example.multilink.ui.auth.LoginScreen
import com.example.multilink.ui.components.GlobalTrackingBlocker
import com.example.multilink.ui.main.ActivityScreen
import com.example.multilink.ui.viewmodel.MultiLinkViewModel
import com.example.multilink.ui.main.RecentScreen
import com.example.multilink.ui.tracker.SeeAllScreen
import com.example.multilink.ui.main.UserProfileScreen
import com.example.multilink.ui.tracker.RestrictedScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

const val ANIM_DURATION = 400
val ANIM_EASING = FastOutSlowInEasing
const val TAB_FADE_DURATION = 300

@Composable
fun MultiLinkNavApp(startJoinCode: String? = null) {
    val navController = rememberNavController()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Repositories & ViewModels
    val viewModel: MultiLinkViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val authRepository = remember { AuthRepository() }
    val realtimeRepository = remember { RealtimeRepository() }

    val auth = remember { FirebaseAuth.getInstance() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var isCheckingAuth by rememberSaveable { mutableStateOf(true) }
    var startDest by rememberSaveable { mutableStateOf(MultiLinkRoutes.LOGIN) }
    val pagerState = rememberPagerState(pageCount = { 4 })


    LaunchedEffect(Unit) {
        if (isCheckingAuth) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val hasProfile = authRepository.checkUserExists()
                startDest =
                    if (hasProfile) MultiLinkRoutes.HOME else MultiLinkRoutes.COMPLETE_PROFILE
            } else {
                startDest = MultiLinkRoutes.LOGIN
            }
            isCheckingAuth = false
        }
    }

    fun checkProfileAndNavigate() {
        scope.launch {
            val exists = authRepository.checkUserExists()
            if (exists) {
                navController.navigate(MultiLinkRoutes.HOME) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                navController.navigate(MultiLinkRoutes.COMPLETE_PROFILE) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // --- CENTRAL ACTIONS ---

    // UPDATED: Safely clears the stack and returns to Home
    val handleStopSession: (String) -> Unit = { sessionId ->
        scope.launch {
            realtimeRepository.stopSession(sessionId)

            val stopIntent = Intent(context, LocationService::class.java)
            stopIntent.action = LocationService.ACTION_STOP
            context.startService(stopIntent)

            Toast.makeText(context, "Session Stopped", Toast.LENGTH_SHORT)
                .show()

            if (currentRoute != MultiLinkRoutes.HOME) {
                navController.navigate(MultiLinkRoutes.HOME) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val handleSessionTerminated: () -> Unit = {
        val stopIntent = Intent(context, LocationService::class.java)
        stopIntent.action = LocationService.ACTION_STOP
        context.startService(stopIntent)

        if (currentRoute != MultiLinkRoutes.HOME) {
            navController.navigate(MultiLinkRoutes.HOME) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val handleSessionPausedKick: () -> Unit = {
        if (currentRoute != MultiLinkRoutes.HOME) {
            navController.navigate(MultiLinkRoutes.HOME) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val onProfileClick: () -> Unit = {
        if (currentRoute != MultiLinkRoutes.PROFILE) {
            navController.navigate(MultiLinkRoutes.PROFILE) {
                popUpTo(MultiLinkRoutes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // --- NAVIGATION SETUP ---

    val showBottomBar = currentRoute == MultiLinkRoutes.HOME

    val visibleTab = when (pagerState.currentPage) {
        0 -> BottomNavDest.Home
        1 -> BottomNavDest.Activity
        2 -> BottomNavDest.Recent
        3 -> BottomNavDest.Settings
        else -> BottomNavDest.Home
    }

    val onBottomTabSelected: (BottomNavDest) -> Unit = { dest ->
        val targetPage = when (dest) {
            BottomNavDest.Home -> 0
            BottomNavDest.Activity -> 1
            BottomNavDest.Recent -> 2
            BottomNavDest.Settings -> 3
        }
        scope.launch {
            pagerState.scrollToPage(targetPage)
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Place, // Or your app logo
                    contentDescription = "Logo",
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MultiLink",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    GlobalTrackingBlocker(currentRoute = currentRoute) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                    )
            ) {

                if (isLandscape && showBottomBar) {
                    MultiLinkNavigationRail(
                        currentDestination = visibleTab,
                        onDestinationSelected = onBottomTabSelected
                    )
                }

                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentWindowInsets = WindowInsets(0.dp),

                    bottomBar = {
                        if (!isLandscape && showBottomBar) {
                            MultiLinkNavigationBar(
                                currentDestination = visibleTab,
                                onDestinationSelected = onBottomTabSelected
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = startDest,
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(hazeState)
                        ) {

                            // 1. DASHBOARD SCREEN
                            composable(
                                route = MultiLinkRoutes.HOME,
                                enterTransition = { fadeIn(tween(TAB_FADE_DURATION)) },
                                exitTransition = { fadeOut(tween(TAB_FADE_DURATION)) }
                            ) {
                                val activity = (context as? Activity)
                                var backPressedTime by remember { mutableLongStateOf(0L) }

                                BackHandler(enabled = true) {
                                    if (pagerState.currentPage != 0) {
                                        scope.launch { pagerState.animateScrollToPage(0) }
                                    } else {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - backPressedTime < 2000) {
                                            activity?.finish()
                                        } else {
                                            backPressedTime = currentTime
                                            Toast.makeText(
                                                context, "Press back again to exit",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }
                                }

                                // We wrap the pager and the overlay in a Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = innerPadding.calculateBottomPadding())
                                ) {

                                    // A. The Swipeable Content
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        when (page) {
                                            0 -> {
                                                // Home screen keeps its perfectly isolated transparent TopBar logic!
                                                HomeScreen(
                                                    uiState = uiState,
                                                    onCreateSession = { _, _ -> },
                                                    onSessionClick = { session ->
                                                        navController.navigate(
                                                            "${MultiLinkRoutes.SEE_ALL}/${session.id}"
                                                        )
                                                    },
                                                    onStopSession = { session ->
                                                        handleStopSession(session.id)
                                                    },
                                                    onShareSession = { session ->
                                                        val code = session.joinCode
                                                        val hostName = session.hostName
                                                        val encodedCode =
                                                            android.util.Base64.encodeToString(
                                                                code.toByteArray(),
                                                                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                                                            )
                                                        val link =
                                                            "https://multilink-aa2228.web.app/invite/$encodedCode"
                                                        val shareText =
                                                            if (session.isSharingAllowed) {
                                                                "Title: \"${session.title}\"\nHost: $hostName\nJoin Code: $code\n\nClick to join:\n$link"
                                                            } else {
                                                                "Title: \"${session.title}\"\nHost: $hostName\n\nClick the link to join:\n$link"
                                                            }
                                                        val sendIntent =
                                                            Intent(Intent.ACTION_SEND).apply {
                                                                putExtra(
                                                                    Intent.EXTRA_TEXT, shareText
                                                                )
                                                                type = "text/plain"
                                                            }
                                                        context.startActivity(
                                                            Intent.createChooser(
                                                                sendIntent, "Share Live Link"
                                                            )
                                                        )
                                                    },
                                                    onProfileClick = onProfileClick,
                                                    onDrawerClick = {},
                                                    onJoinSuccess = { sessionId ->
                                                        navController.navigate(
                                                            "${MultiLinkRoutes.LIVE_TRACKING}/$sessionId"
                                                        )
                                                    },
                                                    onJoinCodeEntered = {},
                                                    initialJoinCode = startJoinCode,
                                                    onRestrictedSessionClick = {
                                                        navController.navigate(
                                                            MultiLinkRoutes.RESTRICTED
                                                        )
                                                    },
                                                )
                                            }

                                            1 -> {
                                                val topBarHeight =
                                                    WindowInsets.statusBars.asPaddingValues()
                                                        .calculateTopPadding() + 64.dp
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(top = topBarHeight)
                                                ) {
                                                    ActivityScreen(
                                                        onNavigateToLiveTracking = { sessionId ->
                                                            navController.navigate(
                                                                "${MultiLinkRoutes.LIVE_TRACKING}/$sessionId"
                                                            )
                                                        }
                                                    )
                                                }
                                            }

                                            2 -> {
                                                val topBarHeight =
                                                    WindowInsets.statusBars.asPaddingValues()
                                                        .calculateTopPadding() + 64.dp
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(top = topBarHeight)
                                                ) {
                                                    RecentScreen()
                                                }
                                            }

                                            3 -> {
                                                val topBarHeight =
                                                    WindowInsets.statusBars.asPaddingValues()
                                                        .calculateTopPadding() + 64.dp
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(top = topBarHeight)
                                                ) {
                                                    ScreenPlaceholder("Settings")
                                                }
                                            }
                                        }
                                    }

                                    val globalTopBarAlpha by remember {
                                        derivedStateOf {
                                            val continuousPage =
                                                pagerState.currentPage + pagerState.currentPageOffsetFraction
                                            continuousPage.coerceIn(0f, 1f)
                                        }
                                    }

                                    if (globalTopBarAlpha > 0f) {
                                        MultiLinkTopBar(
                                            modifier = Modifier.alpha(globalTopBarAlpha),
                                            onDrawerClick = {},
                                            onProfileClick = onProfileClick
                                        )
                                    }
                                }
                            }

                            // SEE ALL
                            composable(
                                route = "${MultiLinkRoutes.SEE_ALL}/{sessionId}",
                                arguments = listOf(
                                    navArgument("sessionId") { type = NavType.StringType }),
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { -it / 3 }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { -it / 3 }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                }
                            ) { entry ->
                                val sessionId = entry.arguments?.getString("sessionId") ?: ""

                                SeeAllScreen(
                                    sessionId = sessionId,
                                    onBackClick = { navController.popBackStack() },
                                    onUserClick = { userId ->
                                        navController.navigate(
                                            "${MultiLinkRoutes.DETAIL}/$sessionId/$userId"
                                        )
                                    },
                                    onTrackAllClick = { sid ->
                                        navController.navigate(
                                            "${MultiLinkRoutes.LIVE_TRACKING}/$sid"
                                        )
                                    },
                                    onSessionEnded = handleSessionTerminated,
                                    onSessionPaused = handleSessionPausedKick

                                )
                            }

                            // LIVE TRACKING SCREEN
                            composable(
                                route = "${MultiLinkRoutes.LIVE_TRACKING}/{sessionId}",
                                arguments = listOf(
                                    navArgument("sessionId") { type = NavType.StringType }),
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { -it / 3 }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { -it / 3 }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                }
                            ) { entry ->
                                val sessionId = entry.arguments?.getString("sessionId") ?: ""

                                LiveTrackingScreen(
                                    sessionId = sessionId,
                                    onBackClick = { navController.popBackStack() },
                                    onUserDetailClick = { userId ->
                                        navController.navigate(
                                            "${MultiLinkRoutes.DETAIL}/$sessionId/$userId"
                                        )
                                    },
                                    onStopSession = {
                                        handleStopSession(sessionId)
                                    },
                                    onSessionEnded = handleSessionTerminated,
                                    onSessionPaused = handleSessionPausedKick
                                )
                            }

                            // DETAIL SCREEN
                            composable(
                                route = "${MultiLinkRoutes.DETAIL}/{sessionId}/{userId}",
                                arguments = listOf(
                                    navArgument("sessionId") { type = NavType.StringType },
                                    navArgument("userId") { type = NavType.StringType }
                                ),
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { -it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { -it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                }
                            ) { entry ->
                                val sessionId = entry.arguments?.getString("sessionId") ?: ""
                                val userId = entry.arguments?.getString("userId") ?: ""

                                DetailScreen(
                                    sessionId = sessionId,
                                    userId = userId,
                                    onBackClick = { navController.popBackStack() },
                                    onSessionEnded = handleSessionTerminated,
                                    onSessionPaused = handleSessionPausedKick
                                )
                            }

                            // Profile screen
                            composable(
                                route = MultiLinkRoutes.PROFILE,
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { -it / 3 }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { -it / 3 }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                }
                            ) {
                                Box(
                                    modifier = Modifier.padding(
                                        bottom = innerPadding.calculateBottomPadding()
                                    )
                                ) {

                                    val context = LocalContext.current

                                    UserProfileScreen(
                                        onBackClick = { navController.popBackStack() },
                                        onLogout = {
                                            // 1. KILL THE SERVICE!
                                            val stopIntent =
                                                Intent(context, LocationService::class.java)
                                            stopIntent.action = LocationService.ACTION_STOP
                                            context.startService(stopIntent)

                                            // 2. Sign out of Firebase
                                            FirebaseAuth.getInstance()
                                                .signOut()

                                            // 3. Sign out of Google
                                            val gso =
                                                GoogleSignInOptions.Builder(
                                                    GoogleSignInOptions.DEFAULT_SIGN_IN
                                                )
                                                    .build()
                                            val googleSignInClient =
                                                GoogleSignIn.getClient(context, gso)
                                            googleSignInClient.signOut()
                                                .addOnCompleteListener {
                                                    navController.navigate(MultiLinkRoutes.LOGIN) {
                                                        popUpTo(0) { inclusive = true }
                                                    }
                                                }
                                        }
                                    )
                                }
                            }

                            // INFO INPUT SCREEN
                            composable(
                                route = MultiLinkRoutes.COMPLETE_PROFILE,
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
                            ) {
                                InfoInputScreen(
                                    onInfoSubmitted = {
                                        // Info Saved -> Now Go Home
                                        navController.navigate(MultiLinkRoutes.HOME) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // LOGIN
                            composable(
                                route = MultiLinkRoutes.LOGIN,
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { -it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { -it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { it }, animationSpec = tween(
                                            ANIM_DURATION, easing = ANIM_EASING
                                        )
                                    )
                                }
                            ) {
                                val context = LocalContext.current
                                val activity = (context as? Activity)
                                var backPressedTime by remember { mutableLongStateOf(0L) }

                                BackHandler(enabled = true) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - backPressedTime < 2000) {
                                        activity?.finish()
                                    } else {
                                        backPressedTime = currentTime
                                        Toast.makeText(
                                            context, "Press back again to exit", Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }

                                LoginScreen(
                                    onLoginSuccess = {
                                        checkProfileAndNavigate()
                                    }
                                )
                            }

                            composable(
                                route = MultiLinkRoutes.RESTRICTED,
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(ANIM_DURATION)
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)
                                    )
                                }
                            ) {
                                RestrictedScreen(onBackClick = { navController.popBackStack() })
                            }

                            // ABOUT
                            composable(
                                route = MultiLinkRoutes.ABOUT,
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(ANIM_DURATION)
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)
                                    )
                                }
                            ) {
                                ScreenPlaceholder("About Screen")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenPlaceholder(name: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = name)
    }
}