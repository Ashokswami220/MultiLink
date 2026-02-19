package com.example.multilink.ui.navigation

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
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
import com.example.multilink.ui.viewmodel.MultiLinkViewModel
import com.example.multilink.ui.main.RecentScreen
import com.example.multilink.ui.tracker.SeeAllScreen
import com.example.multilink.ui.main.UserProfileScreen
import com.example.multilink.ui.main.UserScreen
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

    val handleStopSession: (String) -> Unit = { sessionId ->
        scope.launch {
            realtimeRepository.stopSession(sessionId)

            // Stop Service
            val stopIntent = Intent(context, LocationService::class.java)
            stopIntent.action = LocationService.ACTION_STOP
            context.startService(stopIntent)

            Toast.makeText(context, "Session Stopped", Toast.LENGTH_SHORT)
                .show()

            // Navigate back if needed (check if we are not already on home)
            if (currentRoute != MultiLinkRoutes.HOME) {
                navController.popBackStack(MultiLinkRoutes.HOME, inclusive = false)
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

    val bottomBarRoutes = listOf(
        MultiLinkRoutes.HOME,
        MultiLinkRoutes.USER,
        MultiLinkRoutes.RECENT,
        MultiLinkRoutes.SETTINGS
    )
    val showBottomBar = currentRoute in bottomBarRoutes
    var visibleTab by rememberSaveable { mutableStateOf(BottomNavDest.Home) }

    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            MultiLinkRoutes.HOME -> visibleTab = BottomNavDest.Home
            MultiLinkRoutes.USER -> visibleTab = BottomNavDest.User
            MultiLinkRoutes.RECENT -> visibleTab = BottomNavDest.Recent
            MultiLinkRoutes.SETTINGS -> visibleTab = BottomNavDest.Settings
        }
    }

    val onBottomTabSelected: (BottomNavDest) -> Unit = { dest ->
        val route = when (dest) {
            BottomNavDest.Home -> MultiLinkRoutes.HOME
            BottomNavDest.User -> MultiLinkRoutes.USER
            BottomNavDest.Recent -> MultiLinkRoutes.RECENT
            BottomNavDest.Settings -> MultiLinkRoutes.SETTINGS
        }

        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(MultiLinkRoutes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
                modifier = Modifier.haze(hazeState)
            ) {

                // 1. HOME SCREEN
                composable(
                    route = MultiLinkRoutes.HOME,
                    enterTransition = { fadeIn(tween(TAB_FADE_DURATION)) },
                    exitTransition = { fadeOut(tween(TAB_FADE_DURATION)) }
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
                            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    Box(
                        modifier = Modifier.padding(
                            bottom = innerPadding.calculateBottomPadding()
                        )
                    ) {
                        HomeScreen(
                            uiState = uiState,
                            onCreateSession = { _, _ -> },
                            onSessionClick = { session ->
                                navController.navigate("${MultiLinkRoutes.SEE_ALL}/${session.id}")
                            },
                            onStopSession = { session ->
                                handleStopSession(session.id)
                            },
                            onShareSession = { session ->
                                val code = session.joinCode
                                val hostName = session.hostName
                                val link = "https://multilink-aa2228.web.app/join/$code"

                                val shareText = """
                                    🚗 Join my Trip: "${session.title}"
                                    👤 Host: $hostName
                                    🔢 Code: $code
                                    
                                    Click to join:
                                    $link
                                """.trimIndent()

                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent =
                                    Intent.createChooser(sendIntent, "Share Live Link")
                                context.startActivity(shareIntent)
                            },
                            onProfileClick = onProfileClick,
                            onDrawerClick = {},
                            onJoinSuccess = { sessionId ->
                                navController.navigate(
                                    "${MultiLinkRoutes.LIVE_TRACKING}/$sessionId"
                                )
                            },
                            onJoinCodeEntered = {
                            },
                            initialJoinCode = startJoinCode,
                            onRestrictedSessionClick = {
                                navController.navigate(MultiLinkRoutes.RESTRICTED)
                            },
                        )
                    }
                }

                // 2. USER
                composableWithTopBar(
                    route = MultiLinkRoutes.USER, globalPadding = innerPadding,
                    onProfileClick = onProfileClick, onDrawerClick = {}) {
                    UserScreen()
                }

                // 3. RECENT
                composableWithTopBar(
                    route = MultiLinkRoutes.RECENT, globalPadding = innerPadding,
                    onDrawerClick = onProfileClick, onProfileClick = {}) {
                    RecentScreen()
                }

                // 4. SETTINGS
                composableWithTopBar(
                    route = MultiLinkRoutes.SETTINGS, globalPadding = innerPadding,
                    onDrawerClick = onProfileClick, onProfileClick = {}) {
                    ScreenPlaceholder("Settings")
                }

                // SEE ALL
                composable(
                    route = "${MultiLinkRoutes.SEE_ALL}/{sessionId}",
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
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
                            navController.navigate("${MultiLinkRoutes.DETAIL}/$sessionId/$userId")
                        },
                        onTrackAllClick = { sid ->
                            navController.navigate("${MultiLinkRoutes.LIVE_TRACKING}/$sid")
                        }

                    )
                }

                // LIVE TRACKING SCREEN
                composable(
                    route = "${MultiLinkRoutes.LIVE_TRACKING}/{sessionId}",
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
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
                            navController.navigate("${MultiLinkRoutes.DETAIL}/$sessionId/$userId")
                        },
                        onStopSession = {
                            handleStopSession(sessionId)
                        }
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
                        onBackClick = { navController.popBackStack() }
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
                                val stopIntent = Intent(context, LocationService::class.java)
                                stopIntent.action = LocationService.ACTION_STOP
                                context.startService(stopIntent)

                                // 2. Sign out of Firebase
                                FirebaseAuth.getInstance()
                                    .signOut()

                                // 3. Sign out of Google
                                val gso =
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .build()
                                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                googleSignInClient.signOut()
                                    .addOnCompleteListener {
                                        // 4. Navigate to Login (Clear History)
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
                            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT)
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
                            initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)
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
                            initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)
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

// ... (Helper functions remain the same) ...
@Composable
fun ScreenPlaceholder(name: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = name)
    }
}

fun NavGraphBuilder.composableWithTopBar(
    route: String,
    globalPadding: PaddingValues,
    onDrawerClick: () -> Unit,
    onProfileClick: () -> Unit,
    content: @Composable () -> Unit
) {
    composable(
        route = route,
        enterTransition = { fadeIn(tween(TAB_FADE_DURATION)) },
        exitTransition = { fadeOut(tween(TAB_FADE_DURATION)) }
    ) {
        ScreenWithTopBar(
            onDrawerClick = onDrawerClick,
            onProfileClick = onProfileClick
        ) { topBarPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarPadding.calculateTopPadding())
                    .padding(bottom = globalPadding.calculateBottomPadding())
            ) {
                content()
            }
        }
    }
}