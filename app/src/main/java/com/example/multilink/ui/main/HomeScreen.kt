package com.example.multilink.ui.main

import com.example.multilink.ui.components.home.EmptySessionState
import com.example.multilink.ui.components.home.HomeBanner
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
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.NoInternetBanner
import com.example.multilink.ui.components.PauseWarningDialog
import com.example.multilink.ui.components.dialogs.CreateSessionDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.ui.components.dialogs.ArrivedToggleDialog
import com.example.multilink.ui.components.dialogs.TooFarDialog
import com.example.multilink.ui.viewmodel.HomeUiEvent
import com.example.multilink.ui.viewmodel.HomeViewModel
import com.example.multilink.utils.HapticHelper
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    uiState: MultiLinkUiState,
    onSessionClick: (SessionData) -> Unit,
    onShareSession: (SessionData) -> Unit,
    onProfileClick: () -> Unit,
    initialJoinCode: String? = null,
    onNavigateSession: (SessionData) -> Unit,
    onSessionInfoClick: (String) -> Unit
) {
    val homeViewModel: HomeViewModel = viewModel()
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var createDialogIsParental by rememberSaveable { mutableStateOf(false) }
    val (showCreateDialog, setShowCreateDialog) = rememberSaveable { mutableStateOf(false) }
    val (showJoinDialog, setShowJoinDialog) = rememberSaveable { mutableStateOf(false) }
    val (joinDialogInitCode, setJoinDialogInitCode) = remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState(pageCount = { 2 })

    val (networkErrorTrigger, setNetworkErrorTrigger) = remember { mutableIntStateOf(0) }
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
    val realLocationState by LocationService.currentLocation.collectAsState()

    val (showTooFarDialog, setShowTooFarDialog) = remember { mutableStateOf(false) }
    val (pendingArrivalState, setPendingArrivalState) = remember {
        mutableStateOf<Pair<SessionData, Boolean>?>(
            null
        )
    }
    val (distanceRemaining, setDistanceRemaining) = remember { mutableIntStateOf(0) }

    val (sessionToPause, setSessionToPause) = remember { mutableStateOf<SessionData?>(null) }
    val (sessionToEdit, setSessionToEdit) = remember { mutableStateOf<SessionData?>(null) }
    val currentSortOption by homeViewModel.sortOption.collectAsState()

    val errorColor = MaterialTheme.colorScheme.error
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            if (!isOnline) {
                window.statusBarColor = errorColor.toArgb()
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

    // Handle incoming deep links / parameters
    LaunchedEffect(initialJoinCode) {
        if (initialJoinCode != null) {
            setJoinDialogInitCode(initialJoinCode)
            setShowJoinDialog(true)
        }
    }

    // Trigger App Restart Tracking via ViewModel
    LaunchedEffect(uiState.sessions) {
        homeViewModel.checkAppRestartTracking(uiState.sessions)
    }

    //Centralized UI Events listener via MVVM
    LaunchedEffect(Unit) {
        homeViewModel.uiEvents.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowToast -> Toast.makeText(
                    context, event.message, Toast.LENGTH_SHORT
                )
                    .show()

                is HomeUiEvent.StartTrackingService -> {
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_START
                        putExtra(LocationService.EXTRA_SESSION_ID, event.sessionId)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(
                        serviceIntent
                    )
                    else context.startService(serviceIntent)
                }

                is HomeUiEvent.StopTrackingService -> {
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_STOP
                        if (event.isRemoval) putExtra(
                            LocationService.EXTRA_STOP_MODE, LocationService.MODE_REMOVE
                        )
                    }
                    context.startService(serviceIntent)
                }

                is HomeUiEvent.ShowTooFarDialog -> {
                    setDistanceRemaining(event.distanceMeters)
                    setShowTooFarDialog(true)
                }

                is HomeUiEvent.ShowArrivalConfirmDialog -> {
                    setPendingArrivalState(Pair(event.session, event.isArriving))
                }
            }
        }
    }

    val sortedSessions = remember(uiState.sessions, currentSortOption) {
        homeViewModel.getSortedSessions(uiState.sessions)
    }

    val activeSessions =
        remember(sortedSessions) { sortedSessions.filter { it.sessionType != "Parental" } }
    val parentalSessions =
        remember(sortedSessions) { sortedSessions.filter { it.sessionType == "Parental" } }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NoInternetBanner(isVisible = !isOnline, errorTrigger = networkErrorTrigger)

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val minScrollHeight = this.maxHeight + 1.dp

            val bannerHeight = (this.maxHeight * 0.3f).coerceAtLeast(200.dp)
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
                        onSortChanged = { homeViewModel.updateSortOption(it) }
                    )

                    // The Horizontal Pager wrapping the list
                    HorizontalPager(
                        state = pagerState, verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) { page ->
                        val listToRender = if (page == 0) activeSessions else parentalSessions

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isLoading && listToRender.isEmpty()) {
                                repeat(3) {
                                    SkeletonSessionCard(
                                        modifier = Modifier.padding(
                                            horizontal = dimensionResource(
                                                id = R.dimen.padding_standard
                                            ), vertical = 8.dp
                                        )
                                    )
                                }
                            } else if (listToRender.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 64.dp, bottom = 32.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    EmptySessionState(
                                        isParental = (page == 1),
                                        onCreateClick = {
                                            createDialogIsParental = (page == 1)
                                            setShowCreateDialog(true)
                                        },
                                        onJoinClick = {
                                            setJoinDialogInitCode(null)
                                            setShowJoinDialog(true)
                                        }
                                    )
                                }
                            } else {
                                listToRender.forEach { session ->
                                    val isHost = session.hostId == homeViewModel.currentUserId

                                    val singleClick = rememberSingleClick {
                                        val hasDest =
                                            session.endLat != null && session.endLat != 0.0
                                        homeViewModel.verifyTrackingActive(session)

                                        if (isHost || session.isUsersVisible) {
                                            onSessionClick(session)
                                        } else if (!session.isUsersVisible) {
                                            if (hasDest) onNavigateSession(session)
                                            else Toast.makeText(
                                                context, "Waiting for host to set destination",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }

                                    SessionCard(
                                        data = session,
                                        onClick = singleClick,
                                        onNavigateClick = {
                                            if (session.endLat != null && session.endLat != 0.0) onNavigateSession(
                                                session
                                            )
                                        },
                                        onStopClick = {
                                            if (isHost) homeViewModel.stopSession(session.id)
                                            else homeViewModel.leaveSession(session.id)
                                        },
                                        onShareClick = { onShareSession(session) },
                                        onPauseClick = { setSessionToPause(session) },
                                        onResumeClick = { homeViewModel.resumeSession(session.id) },
                                        onEditClick = {
                                            if (isOnline) {
                                                createDialogIsParental =
                                                    (session.sessionType == "Parental")
                                                setSessionToEdit(session)
                                            } else {
                                                HapticHelper.trigger(
                                                    context, HapticHelper.Type.ERROR
                                                )
                                                setNetworkErrorTrigger(networkErrorTrigger + 1)
                                            }
                                        },
                                        onInfoClick = { onSessionInfoClick(session.id) },
                                        onArrivedClick = { isArriving ->
                                            homeViewModel.attemptArrival(
                                                session, realLocationState, isArriving
                                            )
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
                    title = stringResource(id = R.string.app_name),
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
            val isCurrentTabEmpty =
                if (pagerState.currentPage == 0) activeSessions.isEmpty() else parentalSessions.isEmpty()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = dimensionResource(id = R.dimen.padding_large), bottom = 16.dp)
            ) {
                AnimatedFab(
                    isVisible = !isCurrentTabEmpty && !uiState.isLoading,
                    isOnline = isOnline,
                    onCreateClick = {
                        createDialogIsParental = (pagerState.currentPage == 1)
                        setShowCreateDialog(true)
                    },
                    onJoinClick = { setJoinDialogInitCode(null); setShowJoinDialog(true) },
                    onErrorTrigger = { setNetworkErrorTrigger(networkErrorTrigger + 1) }
                )
            }

            // D. Dialogs & Overlays
            if (showTooFarDialog) {
                TooFarDialog(
                    distanceMeters = distanceRemaining,
                    onDismiss = { setShowTooFarDialog(false) }
                )
            }

            pendingArrivalState?.let { (session, isArriving) ->
                ArrivedToggleDialog(
                    isArriving = isArriving,
                    onConfirm = {
                        setPendingArrivalState(null)
                        homeViewModel.confirmArrival(session, isArriving)
                    },
                    onDismiss = { setPendingArrivalState(null) }
                )
            }

            if (showCreateDialog) {
                CreateSessionDialog(
                    isParental = createDialogIsParental,
                    onDismiss = { setShowCreateDialog(false) },
                    onSuccess = { newSession, isSharing ->
                        setShowCreateDialog(false)
                        homeViewModel.createSession(newSession, isSharing)
                    }
                )
            }

            if (showJoinDialog) {
                UnifiedJoinDialog(
                    initialCode = joinDialogInitCode,
                    onDismiss = { setShowJoinDialog(false) },
                    onJoinConfirmed = { realSessionId ->
                        setShowJoinDialog(false)
                        homeViewModel.joinSession(realSessionId)
                    }
                )
            }

            if (sessionToPause != null) {
                PauseWarningDialog(
                    onConfirm = {
                        homeViewModel.pauseSession(sessionToPause.id)
                        setSessionToPause(null)
                    },
                    onDismiss = { setSessionToPause(null) }
                )
            }

            if (sessionToEdit != null) {
                CreateSessionDialog(
                    isParental = createDialogIsParental,
                    existingSession = sessionToEdit,
                    onDismiss = { setSessionToEdit(null) },
                    onSuccess = { updatedSession, isSharing ->
                        homeViewModel.editSession(sessionToEdit, updatedSession, isSharing)
                        setSessionToEdit(null)
                    }
                )
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
    val (isExpanded, setExpanded) = remember { mutableStateOf(false) }
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
                setExpanded(true)
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
                            setExpanded(false)
                            onJoinClick()
                        } else {
                            HapticHelper.trigger(context, HapticHelper.Type.ERROR)
                            setExpanded(false)
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
                            setExpanded(false)
                            onCreateClick()
                        } else {
                            HapticHelper.trigger(context, HapticHelper.Type.ERROR)
                            setExpanded(false)
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
                        setExpanded(false)
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
        "Track deliveries, vehicles, trips on one screen",
        "Track your family and friends in real-time"
    )
    val context = LocalContext.current

    // State for the dropdown menu
    val (showSortMenu, setShowSortMenu) = remember { mutableStateOf(false) }

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
                .padding(start = 24.dp, end = 12.dp)
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

            AnimatedVisibility(visible = selectedTab == 0 || selectedTab == 1) {
                Box {
                    Surface(
                        onClick = {
                            HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                            setShowSortMenu(true)
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
                        onDismissRequest = { setShowSortMenu(false) },
                        shape = RoundedCornerShape(
                            dimensionResource(id = R.dimen.corner_menu_sheet)
                        )
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
                                    setShowSortMenu(false)
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}