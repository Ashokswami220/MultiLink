package com.example.multilink.ui.tracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.BuildConfig
import com.example.multilink.model.SessionParticipant
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.repo.RouteRepository
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.MapTopBar
import com.example.multilink.ui.components.MultiLinkMap
import com.example.multilink.ui.components.MyLocationFab
import com.example.multilink.ui.components.SessionActionHandler
import com.example.multilink.ui.components.SessionControlBar
import com.example.multilink.ui.components.SessionMapContent
import com.example.multilink.ui.components.dialogs.SessionInfoDialog
import com.example.multilink.ui.theme.MultiLinkTheme
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import com.example.multilink.utils.LocationUtils.calculateDistance
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sessionId: String, userId: String, onBackClick: () -> Unit,
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()

    val repository = remember { RealtimeRepository() }
    val context = LocalContext.current
    val token = BuildConfig.MAPBOX_ACCESS_TOKEN
    val routeRepository = remember { RouteRepository(token) }
    val scope = rememberCoroutineScope()

    var showInfoDialog by remember { mutableStateOf(false) }
    if (showInfoDialog && uiState.sessionData != null) {
        SessionInfoDialog(
            session = uiState.sessionData!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    val actionHandler = remember { SessionActionHandler(context, repository, scope) }

    val user = remember(uiState.participants, userId) {
        uiState.participants.find { it.id == userId }
            ?: SessionParticipant(id = userId, name = "Loading...")
    }

    val isUserInSession = remember(uiState.participants, userId) {
        uiState.participants.any { it.id == userId }
    }

    var isRemoving by remember { mutableStateOf(false) }

    var isFullScreen by remember { mutableStateOf(false) }
    BackHandler(enabled = isFullScreen) { isFullScreen = false }

    // Static Data (Phone/Photo) - Fetched once
    var userPhone by remember { mutableStateOf("") }
    var userPhoto by remember { mutableStateOf<String?>(null) }
    var realRoutePoints by remember { mutableStateOf<List<Point>>(emptyList()) }


    LaunchedEffect(userId) {
        val profile = repository.getGlobalUserProfile(userId)
        if (profile != null) {
            userPhone = profile["phone"] ?: ""
            userPhoto = profile["photoUrl"]?.takeIf { it.isNotEmpty() }
        }
    }

    // --- NAVIGATION LOGIC ---

    // A. Session Ended
    LaunchedEffect(uiState.isSessionActive) {
        if (!uiState.isSessionActive && !uiState.isLoading) {
            Toast.makeText(context, "Session Ended", Toast.LENGTH_SHORT)
                .show()
            onBackClick()
        }
    }

    // B. User Left (Automatic) vs Removed (Manual)
    LaunchedEffect(uiState.isLoading, isUserInSession) {
        // Only pop if user is gone AND we didn't initiate the removal ourselves
        if (!uiState.isLoading && !isUserInSession && !isRemoving) {
            Toast.makeText(context, "User left the session", Toast.LENGTH_SHORT)
                .show()
            onBackClick()
        }
    }


    LaunchedEffect(uiState.startPoint, uiState.endPoint) {
        if (uiState.startPoint != null && uiState.endPoint != null) {
            val path = routeRepository.getRoute(uiState.startPoint!!, uiState.endPoint!!)
            realRoutePoints = path.ifEmpty { listOf(uiState.startPoint!!, uiState.endPoint!!) }
        }
    }

    val targetUserLocation = if (user.lat != 0.0) Point.fromLngLat(user.lng, user.lat) else null

    val serviceLocationState by LocationService.currentLocation.collectAsState()
    var localLocationState by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc -> localLocationState = loc }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val myRealPoint = remember(serviceLocationState, localLocationState) {
        val loc = serviceLocationState ?: localLocationState
        loc?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(
            75.7950, 26.9190
        )
    }

    val mapViewportState =
        rememberMapViewportState {
            setCameraOptions {
                center(myRealPoint); zoom(14.0); pitch(
                0.0
            )
            }
        }
    LaunchedEffect(targetUserLocation) {
        targetUserLocation?.let {
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(it)
                    .zoom(15.0)
                    .build()
            )
        }
    }

    val distFromMe = remember(myRealPoint, targetUserLocation) {
        calculateDistance(
            myRealPoint, targetUserLocation
        )
    }
    val distToEnd = remember(uiState.endPoint, targetUserLocation) {
        calculateDistance(
            targetUserLocation, uiState.endPoint
        )
    }

    val userSpeed = getattr(user, "speed", 0f)
    val speedKmh = (userSpeed * 3.6).toInt()

    val timeLeft = remember(distToEnd, speedKmh) {
        if (speedKmh > 1 && distToEnd != "...") {
            try {
                val distVal = distToEnd.split(" ")[0].toDoubleOrNull() ?: 0.0
                val hours = distVal / speedKmh
                val minutes = (hours * 60).toInt()
                if (minutes > 60) "${minutes / 60} hr ${minutes % 60} min" else "$minutes min"
            } catch (_: Exception) {
                "--"
            }
        } else {
            "--"
        }
    }

    val configuration = LocalConfiguration.current
    val basePeekHeight =
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 110.dp else 270.dp

    val animatedPeekHeight by animateDpAsState(
        targetValue = if (isFullScreen) 0.dp else basePeekHeight,
        animationSpec = tween(durationMillis = 300),
        label = "peekHeight"
    )

    val scaffoldState = rememberBottomSheetScaffoldState()
    val showMapChips =
        scaffoldState.bottomSheetState.targetValue != SheetValue.Expanded && !isFullScreen

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = animatedPeekHeight,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            containerColor = Color.Transparent,
            sheetDragHandle = null,
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Header Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(70.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(35.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (user.status == "Online") Color.Green else Color.Gray
                                    )
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                user.name, style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                userPhone.ifEmpty { "Private / No Contact" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActionItem(
                            Icons.Default.Call, "Call",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            if (userPhone.isNotEmpty()) context.startActivity(
                                Intent(Intent.ACTION_DIAL, "tel:$userPhone".toUri())
                            )
                            else Toast.makeText(
                                context, "No phone number available", Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        ActionItem(
                            Icons.AutoMirrored.Filled.Message, "Message",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            if (userPhone.isNotEmpty()) context.startActivity(
                                Intent(Intent.ACTION_VIEW, "sms:$userPhone".toUri())
                            )
                            else Toast.makeText(
                                context, "No phone number available", Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        ActionItem(
                            Icons.Default.Whatsapp, "WhatsApp", Color(0xFFE0F2F1), Color(0xFF00695C)
                        ) {
                            openWhatsApp(context, userPhone)
                        }
                        ActionItem(
                            Icons.Default.Directions, "Navigate", MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary
                        ) {
                            targetUserLocation?.let {
                                val uri = "google.navigation:q=${it.latitude()},${it.longitude()}"
                                val intent = Intent(Intent.ACTION_VIEW, uri.toUri()).setPackage(
                                    "com.google.android.apps.maps"
                                )
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context, "Maps not installed", Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.2f
                        )
                    )

                    // Info Grid
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f), Icons.Default.NearMe, "Distance", distFromMe,
                                "From You", MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            if (uiState.endPoint != null) {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.SportsScore,
                                    "To Destination", distToEnd, "Remaining",
                                    MaterialTheme.colorScheme.error
                                )
                            } else {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.LocationOff, "Destination",
                                    "Not Selected", "By Host", MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        // Row 3: Speed | Time (If moving/dest set)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f), Icons.Outlined.Speed, "Speed",
                                "$speedKmh km/h", if (speedKmh > 0) "Moving" else "Stopped",
                                MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            if (uiState.endPoint != null) {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Outlined.Timer, "Est. Time To Dest",
                                    timeLeft, "To Arrival", MaterialTheme.colorScheme.primary
                                )
                            } else {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.LocationOff, "Destination",
                                    "No Time", "End Loc not selected",
                                    MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        // Row 3: Battery | Status (Re-added)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f),
                                if (user.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                "Battery", "${user.batteryLevel}%",
                                if (user.isCharging) "Charging" else "Normal",
                                if (user.batteryLevel < 20) Color.Red else MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            InfoCard(
                                Modifier.weight(1f), Icons.Default.Speed, "Status", user.status,
                                "Activity", MaterialTheme.colorScheme.tertiary
                            )
                        }

                    }
                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                MultiLinkMap(
                    viewportState = mapViewportState,
                    hasLocationPermission = true,
                    enableLocationPuck = true,
                    routePoints = realRoutePoints
                ) {
                    SessionMapContent(
                        sessionStartPoint = uiState.startPoint,
                        startLocName = uiState.startName,
                        destinationPoint = uiState.endPoint,
                        endLocName = uiState.endName,
                        userLocations = if (targetUserLocation != null) listOf(
                            targetUserLocation to user
                        ) else emptyList(),
                        currentUserId = null
                    )
                }

                AnimatedVisibility(
                    visible = !isFullScreen,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues()
                                    .calculateTopPadding() + 8.dp
                            )
                    ) {
                        MapTopBar(
                            startName = uiState.startName,
                            endName = uiState.endName,
                            isViewerAdmin = uiState.isCurrentUserAdmin,
                            isTargetUserSelf = (uiState.currentUserId == userId),
                            onBackClick = onBackClick,
                            onStartClick = {
                                if (uiState.startPoint != null) mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(uiState.startPoint)
                                        .zoom(16.0)
                                        .build()
                                ) else Toast.makeText(
                                    context, "Start location not set", Toast.LENGTH_SHORT
                                )
                                    .show()
                            },
                            onEndClick = {
                                if (uiState.endPoint != null) mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(uiState.endPoint)
                                        .zoom(16.0)
                                        .build()
                                ) else Toast.makeText(
                                    context, "Destination not set", Toast.LENGTH_SHORT
                                )
                                    .show()
                            },
                            onCallClick = { actionHandler.onCall(userPhone) },
                            onRemoveClick = {
                                isRemoving = true // 1. Flag ON
                                actionHandler.onRemoveUser(sessionId, userId, user.name) {
                                    onBackClick() // 2. Manual Pop
                                }
                            },
                            onInfoClick = { showInfoDialog = true }

                        )

                        AnimatedVisibility(
                            visible = showMapChips && uiState.endPoint != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp) // Vertical spacing
                            ) {
                                StatusChip(
                                    Icons.Default.SportsScore, distToEnd,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                                StatusChip(
                                    Icons.Outlined.Timer, timeLeft,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                                if (speedKmh > 0) {
                                    StatusChip(
                                        Icons.Outlined.Speed, "$speedKmh km/h",
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = animatedPeekHeight + 16.dp, end = 16.dp)
                        .padding(bottom = if (isFullScreen) 32.dp else 0.dp)
                        .wrapContentWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. The Pill
                        SessionControlBar(
                            onStartClick = {
                                if (uiState.startPoint != null) mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(uiState.startPoint)
                                        .zoom(16.0)
                                        .build()
                                )
                                else Toast.makeText(
                                    context, "Start location not set", Toast.LENGTH_SHORT
                                )
                                    .show()
                            },
                            onUserClick = {
                                if (targetUserLocation != null) mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(targetUserLocation)
                                        .zoom(16.0)
                                        .build()
                                )
                                else Toast.makeText(
                                    context, "User location not available", Toast.LENGTH_SHORT
                                )
                                    .show()
                            },
                            onEndClick = {
                                if (uiState.endPoint != null) mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(uiState.endPoint)
                                        .zoom(16.0)
                                        .build()
                                )
                                else Toast.makeText(
                                    context, "Destination not set", Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        )

                        // 2. Full Screen Button
                        FloatingActionButton(
                            onClick = { isFullScreen = !isFullScreen },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Full Screen"
                            )
                        }

                        // 3. My Location Button
                        MyLocationFab(
                            onClick = {
                                mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(myRealPoint)
                                        .zoom(16.0)
                                        .build()
                                )
                                if (serviceLocationState == null && ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    LocationServices.getFusedLocationProviderClient(context)
                                        .getCurrentLocation(
                                            Priority.PRIORITY_HIGH_ACCURACY, null
                                        )
                                        .addOnSuccessListener { loc ->
                                            localLocationState = loc
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

fun getattr(obj: Any, prop: String, default: Float): Float {
    return try {
        val field = obj::class.java.getDeclaredField(prop)
        field.isAccessible = true
        field.getFloat(obj)
    } catch (_: Exception) {
        default
    }
}

@Composable
fun StatusChip(icon: ImageVector, text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector, label: String, color: Color, iconColor: Color, onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = color,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subValue: String,
    tint: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}


fun openWhatsApp(context: Context, number: String) {
    try {
        val cleanNumber = number.replace(Regex("[^0-9]"), "")
        val url = "https://api.whatsapp.com/send?phone=$cleanNumber"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = url.toUri()
        context.startActivity(i)
    } catch (_: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT)
            .show()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDetailRedesign() {
    MultiLinkTheme {
        DetailScreen(sessionId = "123", userId = "1", onBackClick = {})
    }
}