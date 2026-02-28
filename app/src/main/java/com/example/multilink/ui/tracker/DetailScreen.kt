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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.BuildConfig
import com.example.multilink.R
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
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import com.example.multilink.utils.LocationUtils.calculateDistance
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sessionId: String,
    userId: String,
    onBackClick: () -> Unit,
    onSessionEnded: () -> Unit,
    onSessionPaused: () -> Unit
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()

    val repository = remember { RealtimeRepository() }
    val context = LocalContext.current
    val token = BuildConfig.MAPBOX_ACCESS_TOKEN
    val routeRepository = remember { RouteRepository(token) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionId, userId) {
        var isWatching = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Screen is visible/foregrounded
                if (!isWatching) {
                    repository.incrementUserWatchers(sessionId, userId)
                    isWatching = true
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Screen is hidden/backgrounded
                if (isWatching) {
                    repository.decrementUserWatchers(sessionId, userId)
                    isWatching = false
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Safety cleanup when composable is destroyed
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isWatching) {
                repository.decrementUserWatchers(sessionId, userId)
                isWatching = false
            }
        }
    }

    val (showInfoDialog, setShowInfoDialog) = remember { mutableStateOf(false) }
    if (showInfoDialog && uiState.sessionData != null) {
        SessionInfoDialog(
            session = uiState.sessionData!!,
            onDismiss = { setShowInfoDialog(false) }
        )
    }

    val actionHandler = remember { SessionActionHandler(context, repository, scope) }

    val user = remember(uiState.participants, userId) {
        uiState.participants.find { it.id == userId }
            ?: SessionParticipant(id = userId, name = context.getString(R.string.state_loading))
    }

    val isUserInSession = remember(uiState.participants, userId) {
        uiState.participants.any { it.id == userId }
    }

    var isRemoving by remember { mutableStateOf(false) }

    var isFullScreen by remember { mutableStateOf(false) }
    BackHandler(enabled = isFullScreen) { isFullScreen = false }

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
    var isNavigatingOut by remember { mutableStateOf(false) }

    LaunchedEffect(
        uiState.isSessionActive, uiState.isRemoved, isUserInSession, uiState.isLoading,
        uiState.sessionData?.status
    ) {
        if (uiState.isLoading || isNavigatingOut) return@LaunchedEffect

        val isPaused = uiState.sessionData?.status == "Paused"
        val isAdmin = uiState.isCurrentUserAdmin

        if (!uiState.isSessionActive) {
            isNavigatingOut = true
            Toast.makeText(
                context, context.getString(R.string.msg_session_ended), Toast.LENGTH_SHORT
            )
                .show()
            onSessionEnded()
        } else if (uiState.isRemoved) {
            kotlinx.coroutines.delay(100)
            isNavigatingOut = true
            Toast.makeText(context, "You were removed by the host", Toast.LENGTH_LONG)
                .show()
            onSessionEnded()
        } else if (isPaused && !isAdmin) {
            // Kick non-admins, but keep service alive
            isNavigatingOut = true
            Toast.makeText(context, "Session paused by Host", Toast.LENGTH_LONG)
                .show()
            onSessionPaused()
        } else if (uiState.participants.isNotEmpty() && !isUserInSession && !isRemoving) {
            isNavigatingOut = true
            Toast.makeText(
                context, context.getString(R.string.msg_user_left_session), Toast.LENGTH_SHORT
            )
                .show()
            // Leaves us on the Live Tracking Map if the target user drops
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

    val userSpeed = user.speed
    val speedKmh = (userSpeed * 3.6).toInt()

    val timeLeft = remember(distToEnd, speedKmh) {
        if (speedKmh > 1 && distToEnd != "..." && distToEnd.contains(" ")) {
            try {
                val parts = distToEnd.split(" ")
                val distVal = parts[0].toDoubleOrNull() ?: 0.0
                val isKm = parts[1] == "km"

                // Convert to KM so division by KM/H works properly
                val distKm = if (isKm) distVal else distVal / 1000.0

                val hours = distKm / speedKmh
                val minutes = (hours * 60).toInt()

                if (minutes > 60) {
                    context.getString(R.string.format_time_hr_min, minutes / 60, minutes % 60)
                } else {
                    context.getString(R.string.format_time_min, minutes)
                }
            } catch (_: Exception) {
                "--"
            }
        } else {
            "--"
        }
    }

    val configuration = LocalConfiguration.current
    val basePeekHeight =
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            dimensionResource(R.dimen.peek_height_landscape)
        else dimensionResource(R.dimen.peek_height_portrait)

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
            sheetShape = RoundedCornerShape(
                topStart = dimensionResource(R.dimen.corner_dialog),
                topEnd = dimensionResource(R.dimen.corner_dialog)
            ),
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            containerColor = Color.Transparent,
            sheetDragHandle = null,
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.padding_extra_large))
                        .navigationBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
                    Box(
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.drag_handle_width))
                            .height(dimensionResource(R.dimen.drag_handle_height))
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(
                        modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_large))
                    )

                    // Header Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(
                                    dimensionResource(R.dimen.profile_image_large)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (userPhoto != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(userPhoto)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "User Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Fallback to the default person icon if no photo exists
                                        Icon(
                                            Icons.Default.Person, null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(
                                                dimensionResource(R.dimen.icon_large)
                                            )
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(dimensionResource(R.dimen.status_dot_large))
                                    .clip(CircleShape)
                                    .background(
                                        if (user.status == "Online") Color.Green else Color.Gray
                                    )
                                    .border(
                                        dimensionResource(R.dimen.stroke_width_standard),
                                        MaterialTheme.colorScheme.surface, CircleShape
                                    )
                            )
                        }
                        Spacer(
                            modifier = Modifier.width(dimensionResource(R.dimen.padding_standard))
                        )
                        Column {
                            Text(
                                user.name, style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                userPhone.ifEmpty {
                                    stringResource(
                                        R.string.placeholder_no_contact
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_huge)))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActionItem(
                            Icons.Default.Call, stringResource(R.string.action_call),
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            if (userPhone.isNotEmpty()) context.startActivity(
                                Intent(Intent.ACTION_DIAL, "tel:$userPhone".toUri())
                            )
                            else Toast.makeText(
                                context, context.getString(R.string.error_no_phone),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        ActionItem(
                            Icons.AutoMirrored.Filled.Message,
                            stringResource(R.string.action_message),
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            if (userPhone.isNotEmpty()) context.startActivity(
                                Intent(Intent.ACTION_VIEW, "sms:$userPhone".toUri())
                            )
                            else Toast.makeText(
                                context, context.getString(R.string.error_no_phone),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        ActionItem(
                            Icons.Default.Whatsapp, stringResource(R.string.action_whatsapp),
                            Color(0xFFE0F2F1), Color(0xFF00695C)
                        ) {
                            openWhatsApp(context, userPhone)
                        }
                        ActionItem(
                            Icons.Default.Directions, stringResource(R.string.action_navigate),
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary
                        ) {
                            if (uiState.endPoint != null) {
                                val uri =
                                    "google.navigation:q=${uiState.endPoint!!.latitude()},${uiState.endPoint!!.longitude()}"
                                val intent = Intent(Intent.ACTION_VIEW, uri.toUri()).setPackage(
                                    "com.google.android.apps.maps"
                                )
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context, context.getString(
                                            R.string.error_maps_not_installed
                                        ), Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            } else {
                                Toast.makeText(
                                    context, context.getString(R.string.error_dest_not_set),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = dimensionResource(R.dimen.padding_extra_large)
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.2f
                        )
                    )

                    // Info Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.padding_standard)
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f), Icons.Default.NearMe,
                                stringResource(R.string.label_distance), distFromMe,
                                stringResource(R.string.label_from_you),
                                MaterialTheme.colorScheme.primary
                            )
                            Spacer(
                                modifier = Modifier.width(dimensionResource(R.dimen.padding_medium))
                            )
                            if (uiState.endPoint != null) {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.SportsScore,
                                    stringResource(R.string.label_to_destination), distToEnd,
                                    stringResource(R.string.label_remaining),
                                    MaterialTheme.colorScheme.error
                                )
                            } else {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.LocationOff,
                                    stringResource(R.string.label_destination),
                                    stringResource(R.string.label_not_selected),
                                    stringResource(R.string.label_by_host),
                                    MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        // Row 3: Speed | Time (If moving/dest set)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f), Icons.Outlined.Speed,
                                stringResource(R.string.label_speed),
                                stringResource(R.string.format_speed_kmh, speedKmh),
                                if (speedKmh > 0) stringResource(
                                    R.string.label_moving
                                ) else stringResource(R.string.label_stopped),
                                MaterialTheme.colorScheme.secondary
                            )
                            Spacer(
                                modifier = Modifier.width(dimensionResource(R.dimen.padding_medium))
                            )
                            if (uiState.endPoint != null) {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Outlined.Timer,
                                    stringResource(R.string.label_est_time),
                                    timeLeft, stringResource(R.string.label_to_arrival),
                                    MaterialTheme.colorScheme.primary
                                )
                            } else {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.LocationOff,
                                    stringResource(R.string.label_destination),
                                    stringResource(R.string.label_no_time),
                                    stringResource(R.string.label_end_loc_not_selected),
                                    MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        // Row 3: Battery | Status (Re-added)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f),
                                if (user.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                stringResource(R.string.label_battery), "${user.batteryLevel}%",
                                if (user.isCharging) stringResource(
                                    R.string.label_charging
                                ) else stringResource(R.string.label_normal),
                                if (user.batteryLevel < 20) Color.Red else MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(
                                modifier = Modifier.width(dimensionResource(R.dimen.padding_medium))
                            )
                            InfoCard(
                                Modifier.weight(1f), Icons.Default.Speed,
                                stringResource(R.string.label_status), user.status,
                                stringResource(R.string.label_activity),
                                MaterialTheme.colorScheme.tertiary
                            )
                        }

                    }
                    Spacer(
                        modifier = Modifier.height(dimensionResource(R.dimen.detail_bottom_spacing))
                    )
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
                                    .calculateTopPadding() + dimensionResource(
                                    R.dimen.padding_small
                                )
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
                                    context, context.getString(R.string.error_start_loc_not_set),
                                    Toast.LENGTH_SHORT
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
                                    context, context.getString(R.string.error_dest_not_set),
                                    Toast.LENGTH_SHORT
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
                            onInfoClick = { setShowInfoDialog(true) }

                        )

                        AnimatedVisibility(
                            visible = showMapChips && uiState.endPoint != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = dimensionResource(R.dimen.padding_standard),
                                    top = dimensionResource(R.dimen.padding_medium)
                                ),
                                verticalArrangement = Arrangement.spacedBy(
                                    dimensionResource(R.dimen.padding_small)
                                ) // Vertical spacing
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
                                        Icons.Outlined.Speed,
                                        stringResource(R.string.format_speed_kmh, speedKmh),
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
                        .padding(
                            bottom = animatedPeekHeight + dimensionResource(
                                R.dimen.padding_standard
                            ),
                            end = dimensionResource(R.dimen.padding_standard)
                        )
                        .padding(
                            bottom = if (isFullScreen) dimensionResource(
                                R.dimen.padding_huge
                            ) else 0.dp
                        )
                        .wrapContentWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.padding_medium)
                        )
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
                                    context, context.getString(R.string.error_start_loc_not_set),
                                    Toast.LENGTH_SHORT
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
                                    context,
                                    context.getString(R.string.error_user_loc_not_available),
                                    Toast.LENGTH_SHORT
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
                                    context, context.getString(R.string.error_dest_not_set),
                                    Toast.LENGTH_SHORT
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
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_box_size))
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.cd_toggle_fullscreen)
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

@Composable
fun StatusChip(icon: ImageVector, text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.9f),
        border = BorderStroke(
            dimensionResource(R.dimen.divider_thickness), Color.White.copy(alpha = 0.3f)
        ),
        shadowElevation = dimensionResource(R.dimen.stroke_width_standard)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_tiny)
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.padding_medium)),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_tiny)))
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
            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_card)),
            color = color,
            modifier = Modifier.size(dimensionResource(R.dimen.action_item_size))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_action))
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
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
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_standard)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            dimensionResource(R.dimen.divider_thickness),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_small)))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
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
        Toast.makeText(
            context, context.getString(R.string.error_whatsapp_not_installed), Toast.LENGTH_SHORT
        )
            .show()
    }
}