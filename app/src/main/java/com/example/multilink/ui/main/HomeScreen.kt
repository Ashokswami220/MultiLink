package com.example.multilink.ui.main

import EmptySessionState
import HomeBanner
import HomeSectionHeader
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.multilink.R
import com.example.multilink.model.MultiLinkUiState
import com.example.multilink.model.SessionData
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.NoInternetBanner
import com.example.multilink.ui.components.PauseWarningDialog
import com.example.multilink.ui.components.dialogs.CreateSessionDialog
import com.example.multilink.ui.components.dialogs.SessionInfoDialog
import com.example.multilink.ui.components.dialogs.UnifiedJoinDialog
import com.example.multilink.ui.components.session.SessionCard
import com.example.multilink.ui.components.session.SkeletonSessionCard
import com.example.multilink.ui.navigation.MultiLinkTopBar
import com.example.multilink.ui.navigation.rememberSingleClick
import com.example.multilink.utils.NetworkMonitor
import kotlinx.coroutines.launch
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight

@Composable
fun HomeScreen(
    uiState: MultiLinkUiState,
    onSessionClick: (SessionData) -> Unit,
    onShareSession: (SessionData) -> Unit,
    onDrawerClick: () -> Unit,
    onProfileClick: () -> Unit,
    initialJoinCode: String? = null,
    onRestrictedSessionClick: () -> Unit
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }
    var joinDialogInitCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialJoinCode) {
        if (initialJoinCode != null) {
            joinDialogInitCode = initialJoinCode
            showJoinDialog = true
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val repository = remember { RealtimeRepository() }
    val view = LocalView.current

    var networkErrorTrigger by remember { mutableIntStateOf(0) }

    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    // FORCE STATUS BAR COLOR LOGIC
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (!isOnline) {
                // Offline: Red status bar matches the banner
                window.statusBarColor = Color(0xFFD32F2F).toArgb()
            } else {
                // Online: Transparent
                window.statusBarColor = Color.Transparent.toArgb()
            }
        }
    }

    // 1. Calculate the Status Bar Height dynamically
    val topInset = WindowInsets.statusBars.asPaddingValues()
        .calculateTopPadding()

    // Animate the top padding so it slides up smoothly
    val animatedTopPadding by animateDpAsState(
        targetValue = if (isOnline) topInset else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "TopBarPadding"
    )

    var sessionToPause by remember { mutableStateOf<SessionData?>(null) }
    var sessionToEdit by remember { mutableStateOf<SessionData?>(null) }
    var sessionToInfo by remember { mutableStateOf<SessionData?>(null) }

    fun startTrackingService(sessionId: String) {
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra(LocationService.EXTRA_SESSION_ID, sessionId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopTrackingService(isRemoval: Boolean = false) {
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            if (isRemoval) {
                // Tell service NOT to write "Offline" to DB
                putExtra(LocationService.EXTRA_STOP_MODE, LocationService.MODE_REMOVE)
            }
        }
        context.startService(serviceIntent)
    }

    val sortedSessions = remember(uiState.sessions) { uiState.sessions.reversed() }
    val bannerHeight = 290.dp
    val bannerHeightPx = with(density) { bannerHeight.toPx() }

    val scrollFraction by remember {
        derivedStateOf {
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            val firstItemIndex = listState.firstVisibleItemIndex
            if (firstItemIndex == 0) (firstItemOffset / (bannerHeightPx * 0.5f)).coerceIn(
                0f, 1f
            ) else 1f
        }
    }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val navBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(this)
            .toDp()
    }
    val isThreeButtonNav = navBarHeight > 30.dp

    val baseBottomPadding = dimensionResource(id = R.dimen.padding_list_bottom)

    val dynamicBottomPadding = remember(
        isPortrait,
        sortedSessions.size,
        uiState.isLoading,
        isThreeButtonNav,
        baseBottomPadding
    ) {
        if (isPortrait) {
            val extraBouncePadding = when {
                uiState.isLoading -> 0.dp
                sortedSessions.isEmpty() -> 110.dp
                sortedSessions.size == 1 -> 80.dp
                else -> 0.dp
            }
            val adjustment = if (isThreeButtonNav && extraBouncePadding > 0.dp) 50.dp else 0.dp
            baseBottomPadding + (extraBouncePadding - adjustment).coerceAtLeast(0.dp)
        } else {
            baseBottomPadding
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. ISOLATED BANNER COMPONENT
        NoInternetBanner(
            isVisible = !isOnline,
            errorTrigger = networkErrorTrigger
        )

        // 2. REST OF THE APP (Pushed down when banner appears)
        Box(modifier = Modifier.weight(1f)) {

            // A. The List Content
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = dynamicBottomPadding),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    HomeBanner(
                        height = bannerHeight,
                        scrollOffset = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 0
                    )
                }
                item { HomeSectionHeader() }

                if (uiState.isLoading && sortedSessions.isEmpty()) {
                    items(3) {
                        SkeletonSessionCard(
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(id = R.dimen.padding_standard),
                                vertical = 8.dp
                            )
                        )
                    }
                } else if (sortedSessions.isEmpty()) {
                    item {
                        EmptySessionState(
                            onCreateClick = { showCreateDialog = true },
                            onJoinClick = { joinDialogInitCode = null; showJoinDialog = true }
                        )
                    }
                } else {
                    items(sortedSessions) { session ->
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUserId = auth.currentUser?.uid ?: ""
                        val isHost = session.hostId == currentUserId

                        val singleClick = rememberSingleClick {
                            if (!isHost && !session.isUsersVisible) {
                                onRestrictedSessionClick()
                            } else {
                                onSessionClick(session)
                            }
                        }

                        SessionCard(
                            data = session,
                            onClick = singleClick,
                            onStopClick = {
                                scope.launch {
                                    if (isHost) {
                                        repository.stopSession(session.id)
                                        stopTrackingService(isRemoval = true)
                                        Toast.makeText(
                                            context, "Session Stopped", Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    } else {
                                        repository.leaveSession(session.id)
                                        stopTrackingService(isRemoval = true)
                                        Toast.makeText(context, "Left Session", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            },
                            onShareClick = { onShareSession(session) },
                            onPauseClick = { sessionToPause = session },
                            onResumeClick = {
                                scope.launch {
                                    repository.updateSessionStatus(
                                        session.id, isPaused = false
                                    )
                                }
                            },
                            onEditClick = {
                                if (isOnline) {
                                    sessionToEdit = session
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    networkErrorTrigger++
                                }
                            },
                            onInfoClick = { sessionToInfo = session },
                            modifier = Modifier.padding(
                                bottom = dimensionResource(id = R.dimen.padding_standard)
                            )
                        )
                    }
                }
            }

            // B. The Top Bar (Overlay)
            // Logic to fade the top bar background as you scroll
            val targetColor = MaterialTheme.colorScheme.surface
            val profileTargetColor = MaterialTheme.colorScheme.primary
            val profileColor = remember(scrollFraction) {
                Color(
                    ColorUtils.blendARGB(
                        Color.White.toArgb(), profileTargetColor.toArgb(), scrollFraction
                    )
                )
            }
            val contentTargetColor = MaterialTheme.colorScheme.onSurface
            val contentColor = remember(scrollFraction) {
                Color(
                    ColorUtils.blendARGB(
                        Color.White.toArgb(), contentTargetColor.toArgb(), scrollFraction
                    )
                )
            }

            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (isOnline) WindowInsets.statusBars.asPaddingValues()
                                .calculateTopPadding() + 64.dp else 64.dp
                        )
                        .alpha(scrollFraction),
                    color = targetColor,
                    shadowElevation = if (scrollFraction > 0.9f) 4.dp else 0.dp
                ) {}

                MultiLinkTopBar(
                    onDrawerClick = onDrawerClick,
                    onProfileClick = onProfileClick,
                    containerColor = Color.Transparent,
                    profileColor = profileColor,
                    contentColor = contentColor,
                    titleAlpha = scrollFraction,
                    elevation = 0.dp,
                    modifier = Modifier.padding(top = animatedTopPadding),
                    windowInsets = WindowInsets(0.dp)
                )
            }

            // C. The FABs
            if (uiState.sessions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = dimensionResource(id = R.dimen.padding_large),
                            bottom = 16.dp
                        )
                ) {
                    ExpandableActionFab(
                        isOnline = isOnline,
                        onCreateClick = { showCreateDialog = true },
                        onJoinClick = { joinDialogInitCode = null; showJoinDialog = true },
                        onErrorTrigger = { networkErrorTrigger++ }
                    )
                }
            }

            // D. Dialogs & Overlays
            if (showCreateDialog) {
                CreateSessionDialog(
                    onDismiss = { showCreateDialog = false },
                    onSuccess = { newSession, isSharing ->
                        showCreateDialog = false
                        Toast.makeText(context, "Creating session...", Toast.LENGTH_SHORT)
                            .show()
                        scope.launch {
                            val sessionId = repository.createSession(newSession, isSharing)
                            if (sessionId != null && isSharing) startTrackingService(sessionId)
                        }
                    }
                )
            }

            if (showJoinDialog) {
                UnifiedJoinDialog(
                    initialCode = joinDialogInitCode,
                    onDismiss = { showJoinDialog = false },
                    onJoinConfirmed = { realSessionId ->
                        showJoinDialog = false
                        Toast.makeText(context, "Joining session...", Toast.LENGTH_SHORT)
                            .show()
                        scope.launch {
                            val success = repository.joinSession(realSessionId)
                            if (success) {
                                startTrackingService(realSessionId)
                                Toast.makeText(context, "Joined Successfully", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(context, "Failed to join", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                )
            }

            if (sessionToPause != null) {
                PauseWarningDialog(
                    onConfirm = {
                        scope.launch {
                            repository.updateSessionStatus(
                                sessionToPause!!.id, isPaused = true
                            ); sessionToPause = null
                        }
                    },
                    onDismiss = { sessionToPause = null }
                )
            }

            if (sessionToEdit != null) {
                CreateSessionDialog(
                    existingSession = sessionToEdit,
                    onDismiss = { sessionToEdit = null },
                    onSuccess = { updatedSession, isSharing ->
                        scope.launch {
                            val finalSession = updatedSession.copy(
                                id = sessionToEdit!!.id, hostId = sessionToEdit!!.hostId,
                                status = sessionToEdit!!.status, isHostSharing = isSharing
                            )
                            repository.updateSession(finalSession)

                            if (isSharing) {
                                startTrackingService(finalSession.id)
                            } else {
                                stopTrackingService(isRemoval = true)
                            }

                            sessionToEdit = null
                            Toast.makeText(context, "Session Updated", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                )
            }

            if (sessionToInfo != null) {
                SessionInfoDialog(session = sessionToInfo!!, onDismiss = { sessionToInfo = null })
            }
        }
    }
}


@Composable
fun ExpandableActionFab(
    isOnline: Boolean,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onErrorTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    val contentAlpha = if (isOnline) 1f else 0.38f
    val buttonContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha)

    Surface(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        shape = RoundedCornerShape(16.dp), // M3 Standard FAB shape
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        onClick = {
            if (!isExpanded) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                isExpanded = true
            }
        }
    ) {
        if (!isExpanded) {
            // --- COLLAPSED STATE (Standard + FAB) ---
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Actions",
                    modifier = Modifier.size(24.dp),
                    tint = buttonContentColor
                )
            }
        } else {
            // --- EXPANDED STATE (Horizontal Pill) ---
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // JOIN BUTTON
                TextButton(
                    onClick = {
                        if (isOnline) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            isExpanded = false
                            onJoinClick()
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            isExpanded = false
                            onErrorTrigger()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = buttonContentColor)
                ) {
                    Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Join", fontWeight = FontWeight.Bold)
                }

                // VERTICAL DIVIDER
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                )

                // CREATE BUTTON
                TextButton(
                    onClick = {
                        if (isOnline) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            isExpanded = false
                            onCreateClick()
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            isExpanded = false
                            onErrorTrigger()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = buttonContentColor)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create", fontWeight = FontWeight.Bold)
                }

                // CLOSE BUTTON
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        isExpanded = false
                    }
                ) {
                    Icon(Icons.Default.Close, "Close", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}