package com.example.multilink.ui.main

import EmptySessionState
import HomeBanner
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.multilink.utils.HapticHelper

@Composable
fun HomeScreen(
    uiState: MultiLinkUiState,
    onSessionClick: (SessionData) -> Unit,
    onShareSession: (SessionData) -> Unit,
    onDrawerClick: () -> Unit,
    onProfileClick: () -> Unit,
    initialJoinCode: String? = null,
    onRestrictedSessionClick: () -> Unit,
    onNavigateSession: (SessionData) -> Unit
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

    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val repository = remember { RealtimeRepository() }
    val view = LocalView.current

    var networkErrorTrigger by remember { mutableIntStateOf(0) }

    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            if (!isOnline) {
                window.statusBarColor = Color(0xFFD32F2F).toArgb()
            } else {
                window.statusBarColor = Color.Transparent.toArgb()
            }
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues()
        .calculateTopPadding()

    val animatedTopPadding by animateDpAsState(
        targetValue = if (isOnline) topInset else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "TopBarPadding"
    )

    var sessionToPause by remember { mutableStateOf<SessionData?>(null) }
    var sessionToEdit by remember { mutableStateOf<SessionData?>(null) }
    var sessionToInfo by remember { mutableStateOf<SessionData?>(null) }

    var currentSortOption by rememberSaveable { mutableStateOf("Newest") }

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
                putExtra(LocationService.EXTRA_STOP_MODE, LocationService.MODE_REMOVE)
            }
        }
        context.startService(serviceIntent)
    }

    val sortedSessions = remember(uiState.sessions, currentSortOption) {
        when (currentSortOption) {
            "Newest" -> uiState.sessions.sortedByDescending { it.createdTimestamp }
            "Oldest" -> uiState.sessions.sortedBy { it.createdTimestamp }
            "A-Z" -> uiState.sessions.sortedBy { it.title.lowercase() }
            "Z-A" -> uiState.sessions.sortedByDescending { it.title.lowercase() }
            else -> uiState.sessions.reversed()
        }
    }
    val bannerHeight = 290.dp
    val bannerHeightPx = with(density) { bannerHeight.toPx() }

    val scrollFraction by remember {
        derivedStateOf {
            (scrollState.value / (bannerHeightPx * 0.5f)).coerceIn(0f, 1f)
        }
    }
    val currentScrollOffset by remember {
        derivedStateOf { scrollState.value }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NoInternetBanner(isVisible = !isOnline, errorTrigger = networkErrorTrigger)

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val minScrollHeight = this.maxHeight + 1.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minScrollHeight)
                        .padding(bottom = 120.dp)
                ) {
                    HomeBanner(
                        height = bannerHeight,
                        scrollOffset = currentScrollOffset
                    )

                    HomeTabSwitcher(
                        selectedTab = pagerState.currentPage,
                        onTabSelected = { newPage ->
                            scope.launch { pagerState.animateScrollToPage(newPage) }
                        },
                        currentSort = currentSortOption,
                        onSortChanged = { currentSortOption = it }
                    )

                    // The Horizontal Pager wrapping the list
                    HorizontalPager(
                        state = pagerState,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) { page ->
                        if (page == 0) {
                            // --- PAGE 0: ACTIVE SESSIONS ---
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (uiState.isLoading && sortedSessions.isEmpty()) {
                                    repeat(3) {
                                        SkeletonSessionCard(
                                            modifier = Modifier.padding(
                                                horizontal = dimensionResource(
                                                    id = R.dimen.padding_standard
                                                ),
                                                vertical = 8.dp
                                            )
                                        )
                                    }
                                } else if (sortedSessions.isEmpty()) {
                                    EmptySessionState(
                                        onCreateClick = { showCreateDialog = true },
                                        onJoinClick = {
                                            joinDialogInitCode = null; showJoinDialog = true
                                        }
                                    )
                                } else {
                                    sortedSessions.forEach { session ->
                                        val auth =
                                            com.google.firebase.auth.FirebaseAuth.getInstance()
                                        val currentUserId = auth.currentUser?.uid ?: ""
                                        val isHost = session.hostId == currentUserId

                                        val singleClick = rememberSingleClick {
                                            val hasDest =
                                                session.endLat != null && session.endLat != 0.0

                                            if (isHost) {
                                                onSessionClick(
                                                    session
                                                ) // Host always goes to SeeAll
                                            } else if (!session.isUsersVisible) {
                                                // User + Tracking OFF = Go to Solo Navigation
                                                if (hasDest) onNavigateSession(session)
                                                else Toast.makeText(
                                                    context, "Waiting for host to set destination",
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                            } else {
                                                onSessionClick(
                                                    session
                                                ) // User + Tracking ON = Go to SeeAll
                                            }
                                        }

                                        SessionCard(
                                            data = session,
                                            onClick = singleClick,
                                            onNavigateClick = {
                                                val hasDest =
                                                    session.endLat != null && session.endLat != 0.0
                                                if (hasDest) onNavigateSession(session)
                                            },
                                            onStopClick = {
                                                scope.launch {
                                                    if (isHost) {
                                                        repository.stopSession(session.id)
                                                        stopTrackingService(isRemoval = true)
                                                        Toast.makeText(
                                                            context, "Session Stopped",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                            .show()
                                                    } else {
                                                        repository.leaveSession(session.id)
                                                        stopTrackingService(isRemoval = true)
                                                        Toast.makeText(
                                                            context, "Left Session",
                                                            Toast.LENGTH_SHORT
                                                        )
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
                                                    HapticHelper.trigger(
                                                        context, HapticHelper.Type.ERROR
                                                    )
                                                    networkErrorTrigger++
                                                }
                                            },
                                            onInfoClick = { sessionToInfo = session },
                                            onArrivedClick = {
                                                scope.launch {
                                                    repository.markUserAsArrived(
                                                        session.id, currentUserId
                                                    )
                                                    Toast.makeText(
                                                        context, "Marked as Arrived!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                        .show()
                                                }
                                            },
                                            modifier = Modifier.padding(
                                                bottom = dimensionResource(
                                                    id = R.dimen.padding_standard
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // --- PAGE 1: PARENTAL CONTROL (Empty State) ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Parental Control",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Advanced safety features and monitoring controls are coming soon.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // B. The Top Bar (Overlay)
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = dimensionResource(id = R.dimen.padding_large), bottom = 16.dp)
            ) {
                AnimatedFab(
                    isVisible = uiState.sessions.isNotEmpty() && pagerState.currentPage == 0,
                    isOnline = isOnline,
                    onCreateClick = { showCreateDialog = true },
                    onJoinClick = { joinDialogInitCode = null; showJoinDialog = true },
                    onErrorTrigger = { networkErrorTrigger++ }
                )
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
                            repository.updateSessionStatus(sessionToPause!!.id, isPaused = true)
                            sessionToPause = null
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
fun AnimatedFab(
    isVisible: Boolean,
    isOnline: Boolean,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onErrorTrigger: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ExpandableActionFab(
            isOnline = isOnline,
            onCreateClick = onCreateClick,
            onJoinClick = onJoinClick,
            onErrorTrigger = onErrorTrigger
        )
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
    val context = LocalContext.current

    val contentAlpha = if (isOnline) 1f else 0.38f
    val buttonContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha)

    Surface(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        onClick = {
            if (!isExpanded) {
                HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                isExpanded = true
            }
        }
    ) {
        if (!isExpanded) {
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add, "Actions", modifier = Modifier.size(24.dp),
                    tint = buttonContentColor
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (isOnline) {
                            HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                            isExpanded = false
                            onJoinClick()
                        } else {
                            HapticHelper.trigger(context, HapticHelper.Type.ERROR)
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

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                )

                TextButton(
                    onClick = {
                        if (isOnline) {
                            HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                            isExpanded = false
                            onCreateClick()
                        } else {
                            HapticHelper.trigger(context, HapticHelper.Type.ERROR)
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

                IconButton(
                    onClick = {
                        HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                        isExpanded = false
                    }
                ) {
                    Icon(Icons.Default.Close, "Close", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun HomeTabSwitcher(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    currentSort: String,
    onSortChanged: (String) -> Unit
) {
    val tabs = listOf("Active Sessions", "Parental Control")
    val subtitles = listOf(
        "Track your family and friends in real-time",
        "Track your family and friends in real-time"
    )
    val context = LocalContext.current

    // State for the dropdown menu
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabWidth = maxWidth / tabs.size

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedTab,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                label = "indicator"
            )

            // The full-width top indicator line
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                    onTabSelected(index)
                                }
                            }
                            .padding(top = 16.dp, bottom = 6.dp, start = 24.dp, end = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "SubtitleAnimation",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                Text(
                    text = subtitles[targetTab],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(visible = selectedTab == 0) {
                Box {
                    Surface(
                        onClick = {
                            HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                            showSortMenu = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentSort,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // The Dropdown Menu
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val options = listOf("Newest", "Oldest", "A-Z", "Z-A")
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                trailingIcon = {
                                    if (currentSort == option) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                    onSortChanged(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}