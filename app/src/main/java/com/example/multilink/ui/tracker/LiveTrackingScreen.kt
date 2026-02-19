package com.example.multilink.ui.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.BuildConfig
import com.example.multilink.R
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.repo.RouteRepository
import com.example.multilink.model.SessionParticipant
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.MapTopBar
import com.example.multilink.ui.components.MultiLinkMap
import com.example.multilink.ui.components.MyLocationFab
import com.example.multilink.ui.components.SessionControlBar
import com.example.multilink.ui.components.SessionMapContent
import com.example.multilink.ui.components.dialogs.DeleteSessionDialog
import com.example.multilink.ui.components.dialogs.PauseSessionDialog
import com.example.multilink.ui.components.dialogs.SessionInfoDialog
import com.example.multilink.ui.theme.MultiLinkTheme
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import com.example.multilink.utils.LocationUtils
import com.google.firebase.auth.FirebaseAuth

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import kotlinx.coroutines.launch

enum class SortType {
    A_Z,
    Z_A,
    JOINED_FIRST,
    JOINED_LATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    sessionId: String,
    onBackClick: () -> Unit,
    onUserDetailClick: (String) -> Unit,
    onStopSession: () -> Unit,
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = remember { auth.currentUser?.uid ?: "" }
    val repository = remember { RealtimeRepository() }
    val scope = rememberCoroutineScope()

    // --- ORIENTATION CHECK ---
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Dialog States
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // --- NAVIGATION GUARDS ---
    LaunchedEffect(uiState.isSessionActive) {
        if (!uiState.isSessionActive && !uiState.isLoading) {
            Toast.makeText(context, "Session Ended", Toast.LENGTH_SHORT)
                .show()
            onBackClick()
        }
    }

    // --- MAP & ROUTE ---
    val token = BuildConfig.MAPBOX_ACCESS_TOKEN
    val routeRepository = remember { RouteRepository(token) }
    var realRoutePoints by remember { mutableStateOf<List<Point>>(emptyList()) }

    LaunchedEffect(uiState.startPoint, uiState.endPoint) {
        if (uiState.startPoint != null && uiState.endPoint != null) {
            val path = routeRepository.getRoute(uiState.startPoint!!, uiState.endPoint!!)
            realRoutePoints = path.ifEmpty { listOf(uiState.startPoint!!, uiState.endPoint!!) }
        }
    }

    // Map Locations Logic
    val userLocations = remember(uiState.participants) {
        uiState.participants.mapNotNull { user ->
            if (user.lat != 0.0 && user.lng != 0.0) {
                Point.fromLngLat(user.lng, user.lat) to user
            } else null
        }
    }

    val realLocationState by LocationService.currentLocation.collectAsState()
    val myRealPoint = remember(realLocationState) {
        realLocationState?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(
            75.7950, 26.9190
        )
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(myRealPoint); zoom(13.5); pitch(
            0.0
        )
        }
    }
    var followUserLocation by remember { mutableStateOf(false) }
    var focusTarget by remember { mutableStateOf<Point?>(null) }

    LaunchedEffect(focusTarget) {
        focusTarget?.let {
            followUserLocation = false
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(it)
                    .zoom(16.5)
                    .build()
            )
            focusTarget = null
        }
    }

    // Permissions
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasLocationPermission = it }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    if (showDeleteDialog) {
        DeleteSessionDialog(
            onConfirm = { showDeleteDialog = false; onStopSession() },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showPauseDialog) {
        val isPaused = false
        PauseSessionDialog(
            isPaused = isPaused,
            onConfirm = {
                showPauseDialog = false
                scope.launch { repository.updateSessionStatus(sessionId, !isPaused) }
            },
            onDismiss = { showPauseDialog = false }
        )
    }

    if (showInfoDialog && uiState.sessionData != null) {
        SessionInfoDialog(
            session = uiState.sessionData!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    // --- UI ---
    Scaffold(
        containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MultiLinkMap(
                viewportState = mapViewportState,
                hasLocationPermission = hasLocationPermission,
                enableLocationPuck = true,
                followUser = followUserLocation,
                topContentPadding = WindowInsets.statusBars.asPaddingValues()
                    .calculateTopPadding() + 80.dp,
                routePoints = realRoutePoints
            ) {
                SessionMapContent(
                    sessionStartPoint = uiState.startPoint,
                    startLocName = uiState.startName,
                    destinationPoint = uiState.endPoint,
                    endLocName = uiState.endName,
                    userLocations = userLocations,
                    currentUserId = currentUserId
                )
            }

            Column(
                modifier = Modifier
                    .align(if (isPortrait) Alignment.TopCenter else Alignment.TopStart)
                    .fillMaxWidth(if (isPortrait) 1f else 0.5f)
            ) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(12.dp))

                MapTopBar(
                    startName = uiState.startName,
                    endName = uiState.endName,
                    isViewerAdmin = uiState.isCurrentUserAdmin,
                    isSessionAdminMode = true,
                    sessionStatus = "Live",
                    onBackClick = onBackClick,
                    onStartClick = {
                        if (uiState.startPoint != null) focusTarget =
                            uiState.startPoint else Toast.makeText(
                            context, "No Start Point", Toast.LENGTH_SHORT
                        )
                            .show()
                    },
                    onEndClick = {
                        if (uiState.endPoint != null) focusTarget =
                            uiState.endPoint else Toast.makeText(
                            context, "No End Point", Toast.LENGTH_SHORT
                        )
                            .show()
                    },
                    onDeleteClick = { showDeleteDialog = true },
                    onPauseClick = { showPauseDialog = true },
                    onInfoClick = { showInfoDialog = true },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            val controlsAlignment = if (isPortrait) Alignment.BottomCenter else Alignment.BottomEnd
            Column(
                modifier = Modifier
                    .align(controlsAlignment)
                    .navigationBarsPadding()
                    .padding(
                        start = dimensionResource(id = R.dimen.padding_standard),
                        end = dimensionResource(id = R.dimen.padding_standard),
                        top = dimensionResource(id = R.dimen.padding_standard),
                        bottom = 8.dp
                    )
                    .fillMaxWidth(
                        if (isPortrait) 1f else 0.5f
                    ),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SessionControlBar(
                        onStartClick = {
                            if (uiState.startPoint != null) focusTarget = uiState.startPoint
                            else Toast.makeText(context, "No Start Point", Toast.LENGTH_SHORT)
                                .show()
                        },
                        onUserClick = {
                            val firstOtherUser =
                                userLocations.firstOrNull { it.second.id != currentUserId }
                            if (firstOtherUser != null) {
                                focusTarget = firstOtherUser.first
                            } else {
                                Toast.makeText(
                                    context,
                                    "No other users active",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        },
                        onEndClick = {
                            if (uiState.endPoint != null) focusTarget = uiState.endPoint
                            else Toast.makeText(context, "No End Point", Toast.LENGTH_SHORT)
                                .show()
                        },
                        showUserButton = false
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    MyLocationFab(
                        onClick = {
                            mapViewportState.flyTo(
                                CameraOptions.Builder()
                                    .center(myRealPoint)
                                    .zoom(16.0)
                                    .build()
                            )
                        }
                    )
                }

                if (isPortrait) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LiveBottomSummary(
                        userLocations = userLocations.map { it.second to it.first },
                        destination = uiState.endPoint,
                        isPortrait = true,
                        onFocusUser = { focusTarget = it },
                        onOpenDetails = onUserDetailClick
                    )
                }
            }

            if (!isPortrait) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.5f) // Matches Top Bar Width
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    LiveBottomSummary(
                        modifier = Modifier, // Internal width logic handles filling this Box
                        userLocations = userLocations.map { it.second to it.first },
                        destination = uiState.endPoint,
                        isPortrait = false,
                        onFocusUser = { focusTarget = it },
                        onOpenDetails = onUserDetailClick
                    )
                }
            }
        }
    }
}


@Composable
fun LiveBottomSummary(
    modifier: Modifier = Modifier,
    userLocations: List<Pair<SessionParticipant, Point>>,
    destination: Point?,
    isPortrait: Boolean,
    onFocusUser: (Point) -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    var isExpandedLocal by remember { mutableStateOf(true) }
    val isExpanded = if (isPortrait) true else isExpandedLocal

    var showMenu by remember { mutableStateOf(false) }

    var currentSortType by remember { mutableStateOf(SortType.JOINED_FIRST) }

    var showSortOptions by remember { mutableStateOf(false) }

    val sortedUserLocations = remember(userLocations, currentSortType) {
        when (currentSortType) {
            SortType.A_Z -> userLocations.sortedBy { it.first.name.lowercase() }
            SortType.Z_A -> userLocations.sortedByDescending { it.first.name.lowercase() }
            SortType.JOINED_FIRST -> userLocations
            SortType.JOINED_LATE -> userLocations.reversed()
        }
    }

    val widthFraction = if (isPortrait) 1f else (if (isExpanded) 1f else 0.4f)
    Card(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp, bottom = 4.dp, top = 8.dp)
                .fillMaxWidth()
        ) {
            if (isExpanded) {
                // --- HEADER ROW (Active Chip Left | Collapse & Menu Right) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. ACTIVE CHIP (Top Left)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = Color.Green
                            )

                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Box(
                                modifier = Modifier
                                    .size(width = 1.dp, height = 10.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                            alpha = 0.2f
                                        )
                                    )
                            )

                            Text(
                                text = "${userLocations.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // 2. TOP RIGHT CONTROLS (Collapse + Menu)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Collapse Button (Only in Landscape, Left of Menu)
                        if (!isPortrait) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .width(52.dp) // Horizontal Pill Shape
                                    .height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = { isExpandedLocal = !isExpandedLocal },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Collapse",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Menu Button
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(32.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        showMenu = true
                                        showSortOptions = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        "Menu",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(
                                    dimensionResource(id = R.dimen.corner_menu_sheet)
                                )
                            ) {
                                if (!showSortOptions) {
                                    // STATE 1: Main Menu (Only "Sort by >")
                                    DropdownMenuItem(
                                        text = { Text("Sort by") },
                                        trailingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = { showSortOptions = true }
                                    )
                                } else {
                                    // STATE 2: Sort Options (Sub-menu)

                                    // Back Button to go up one level
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Back",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                                        },
                                        onClick = { showSortOptions = false }
                                    )
                                    HorizontalDivider()

                                    // Option 1: A-Z
                                    DropdownMenuItem(
                                        text = { Text("Name: A - Z") },
                                        leadingIcon = {
                                            if (currentSortType == SortType.A_Z)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SortType.A_Z
                                            showMenu = false
                                        }
                                    )

                                    // Option 2: Z-A
                                    DropdownMenuItem(
                                        text = { Text("Name: Z - A") },
                                        leadingIcon = {
                                            if (currentSortType == SortType.Z_A)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SortType.Z_A
                                            showMenu = false
                                        }
                                    )

                                    // Option 3: Joined First
                                    DropdownMenuItem(
                                        text = { Text("Joined First") },
                                        leadingIcon = {
                                            if (currentSortType == SortType.JOINED_FIRST)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SortType.JOINED_FIRST
                                            showMenu = false
                                        }
                                    )

                                    // Option 4: Joined Late
                                    DropdownMenuItem(
                                        text = { Text("Joined Late") },
                                        leadingIcon = {
                                            if (currentSortType == SortType.JOINED_LATE)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SortType.JOINED_LATE
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- CONTENT ROW (Users Only) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Users List
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        itemsIndexed(sortedUserLocations) { index, (user, point) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { onFocusUser(point) },
                                                onDoubleTap = { onOpenDetails(user.id) }
                                            )
                                        }
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Box {
                                        Surface(
                                            shape = CircleShape,
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Person, null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        user.name.split(" ")
                                            .first(),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )

                                    val distText = if (destination != null) {
                                        LocationUtils.calculateDistance(point, destination)
                                    } else "--"

                                    Text(
                                        text = distText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        ),
                                        maxLines = 1
                                    )
                                }

                                if (index < sortedUserLocations.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .height(60.dp)
                                            .width(1.dp)
                                            .background(
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // COLLAPSED STATE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Live", style = MaterialTheme.typography.labelSmall,
                            color = Color.Green.copy(alpha = 0.8f), fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userLocations.size} Users",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = { isExpandedLocal = !isExpandedLocal },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


fun Modifier.rotate(degrees: Float) = this.then(Modifier.graphicsLayer(rotationZ = degrees))

@Composable
fun UserMapMarker(user: SessionParticipant) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(
            animation = tween(1000), repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = user.name.substringBefore(" "),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Box(contentAlignment = Alignment.Center) {
            if (user.status == "Online") {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape
                        )
                )
            }
            Surface(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.White),
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Icon(
            Icons.Default.Navigation,
            null,
            tint = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
            modifier = Modifier
                .size(12.dp)
                .rotate(180f)
        )
    }
}


@Preview
@Composable
fun PreviewLiveTracking() {
    MultiLinkTheme {
        LiveTrackingScreen(
            sessionId = "Goa Trip", onBackClick = {}, onUserDetailClick = {}, onStopSession = {})
    }
}