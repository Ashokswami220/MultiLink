package com.example.multilink.ui.tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.multilink.R
import com.example.multilink.model.SessionParticipant
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.MultiLinkMap
import com.example.multilink.ui.components.MyLocationFab
import com.example.multilink.ui.components.SessionControlBar
import com.example.multilink.ui.components.SessionMapContent
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import com.example.multilink.utils.HapticHelper
import com.example.multilink.utils.LocationUtils.calculateDistance
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalTrackingScreen(
    sessionId: String,
    userId: String,
    onBackClick: () -> Unit,
    onUserInfoClick: () -> Unit
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()
    val processedParticipants by viewModel.processedParticipants.collectAsState()

    val historyPoints by viewModel.historyPoints.collectAsState()
    val historyRoute by viewModel.historyRoute.collectAsState()
    val selectedDate by viewModel.currentSelectedDate.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val uiModel = remember(processedParticipants, userId) {
        processedParticipants.find { it.participant.id == userId }
    }
    val user = uiModel?.participant ?: SessionParticipant(id = userId, name = "Loading...")
    val userPhone = uiModel?.phoneNumber ?: ""
    val userPhoto = uiModel?.photoUrl

    val serviceLocationState by LocationService.currentLocation.collectAsState()
    var localLocationState by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc -> localLocationState = loc }
        }
    }

    val myRealPoint = remember(serviceLocationState, localLocationState) {
        val loc = serviceLocationState ?: localLocationState
        loc?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(
            75.7950, 26.9190
        )
    }

    val dateList = remember {
        val list = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        repeat(4) {
            list.add(sdf.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        list
    }

    val isToday = selectedDate == dateList[0]

    val targetUserLocation = remember(isToday, user, historyPoints) {
        if (isToday) {
            if (user.lat != 0.0) Point.fromLngLat(user.lng, user.lat) else null
        } else {
            val lastHistory = historyPoints.lastOrNull()
            if (lastHistory != null) {
                Point.fromLngLat(
                    (lastHistory["lng"] as Number).toDouble(),
                    (lastHistory["lat"] as Number).toDouble()
                )
            } else null
        }
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions { center(myRealPoint); zoom(14.0); pitch(0.0) }
    }


    LaunchedEffect(selectedDate) {
        viewModel.loadHistoryForDate(selectedDate, userId)
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionId, userId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.setSpecificUserWatching(userId, true)
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.setSpecificUserWatching(userId, false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setSpecificUserWatching(userId, false)
        }
    }

    var isNavigatingOut by remember { mutableStateOf(false) }
    val isUserInSession =
        remember(uiState.participants, userId) { uiState.participants.any { it.id == userId } }

    LaunchedEffect(uiState.isSessionActive, uiState.isRemoved, isUserInSession, uiState.isLoading) {
        if (uiState.isLoading || isNavigatingOut) return@LaunchedEffect
        if (!uiState.isSessionActive || uiState.isRemoved || (uiState.participants.isNotEmpty() && !isUserInSession)) {
            isNavigatingOut = true
            onBackClick()
        }
    }

    val (isFullScreen, setFullScreen) = remember { mutableStateOf(false) }
    BackHandler(enabled = isFullScreen) { setFullScreen(false) }

    val horizontalListState = rememberLazyListState()
    var selectedPinIndex by remember { mutableIntStateOf(-1) }
    var isTimelineExpanded by remember { mutableStateOf(false) }
    var isNewestFirst by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current

    val basePeekHeight = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        dimensionResource(R.dimen.peek_height_landscape)
    } else {
        if (isTimelineExpanded) 410.dp else 285.dp
    }

    val animatedPeekHeight by animateDpAsState(
        targetValue = if (isFullScreen) 0.dp else basePeekHeight,
        animationSpec = tween(durationMillis = 300), label = "peekHeight"
    )

    val scaffoldState = rememberBottomSheetScaffoldState()

    val allPins = remember(historyPoints) {
        historyPoints.filter { it["isPin"] == true }
            .mapIndexed { index, map -> index to map }
    }

    val displayPins = remember(allPins, isNewestFirst) {
        if (isNewestFirst) allPins.reversed() else allPins
    }

    LaunchedEffect(displayPins) {
        if (displayPins.isNotEmpty()) {
            horizontalListState.scrollToItem(0)
        }
    }

    LaunchedEffect(targetUserLocation) {
        if (selectedPinIndex == -1) {
            targetUserLocation?.let {
                mapViewportState.flyTo(
                    CameraOptions.Builder()
                        .center(it)
                        .zoom(15.0)
                        .build()
                )
            }
        }
    }

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
                val sheetScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.padding_extra_large))
                        .navigationBarsPadding()
                        .verticalScroll(sheetScrollState)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                                modifier = Modifier.width(
                                    dimensionResource(R.dimen.padding_standard)
                                )
                            )
                            Column {
                                Text(
                                    user.name, style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    userPhone.ifEmpty { "No Contact Provided" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box {
                            val (menuExpanded, setMenuExpanded) = remember { mutableStateOf(false) }

                            Surface(
                                onClick = {
                                    HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                    setMenuExpanded(true)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MoreVert, "More Options",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { setMenuExpanded(false) },
                                shape = RoundedCornerShape(
                                    dimensionResource(id = R.dimen.corner_menu_sheet)
                                )
                            ) {
                                DropdownMenuItem(
                                    text = { Text("User Info") },
                                    onClick = {
                                        HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                        setMenuExpanded(false)
                                        onUserInfoClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Info, null) }
                                )

                                if (uiState.isCurrentUserAdmin && uiState.currentUserId != userId) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Remove User",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            HapticHelper.trigger(context, HapticHelper.Type.ERROR)
                                            setMenuExpanded(false)
                                            isNavigatingOut = true
                                            viewModel.removeUser(userId, user.name)
                                            onBackClick()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.PersonRemove, null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dateList) { dateStr ->
                            val isSelected = dateStr == selectedDate
                            val displayDate = when (dateStr) {
                                dateList[0] -> "Today"
                                dateList[1] -> "Yesterday"
                                else -> SimpleDateFormat("EEE, dd", Locale.getDefault()).format(
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                        dateStr
                                    )!!
                                )
                            }

                            Surface(
                                onClick = {
                                    viewModel.loadHistoryForDate(dateStr, userId)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = displayDate,
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp, vertical = 8.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ExpandableHorizontalTimelineCard(
                        isToday = isToday,
                        displayPins = displayPins,
                        isExpanded = isTimelineExpanded,
                        onExpandToggle = { isTimelineExpanded = !isTimelineExpanded },
                        isNewestFirst = isNewestFirst,
                        onSortToggle = { isNewestFirst = !isNewestFirst },
                        listState = horizontalListState,
                        selectedPinIndex = selectedPinIndex,
                        onItemClick = { absoluteIndex, point ->
                            selectedPinIndex = absoluteIndex
                            setFullScreen(false)
                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }

                            val bottomOffset =
                                with(density) { animatedPeekHeight.toPx() }.toDouble()
                            mapViewportState.flyTo(
                                CameraOptions.Builder()
                                    .center(point)
                                    .zoom(19.0)
                                    .padding(EdgeInsets(0.0, 0.0, bottomOffset, 0.0))
                                    .build()
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CompactActionItem(
                            Icons.Default.Call, "Call",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            if (userPhone.isNotEmpty()) context.startActivity(
                                Intent(Intent.ACTION_DIAL, "tel:$userPhone".toUri())
                            ) else Toast.makeText(context, "No Phone", Toast.LENGTH_SHORT)
                                .show()
                        }
                        CompactActionItem(
                            Icons.AutoMirrored.Filled.Message, "Message",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            if (userPhone.isNotEmpty()) context.startActivity(
                                Intent(Intent.ACTION_VIEW, "sms:$userPhone".toUri())
                            ) else Toast.makeText(context, "No Phone", Toast.LENGTH_SHORT)
                                .show()
                        }
                        CompactActionItem(
                            Icons.Default.Whatsapp, "WhatsApp", Color(0xFFE0F2F1), Color(0xFF00695C)
                        ) {
                            try {
                                val cleanNumber = userPhone.replace(Regex("[^0-9]"), "")
                                val url = "https://api.whatsapp.com/send?phone=$cleanNumber"
                                val i = Intent(Intent.ACTION_VIEW)
                                i.data = url.toUri()
                                context.startActivity(i)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context, "WhatsApp not installed", Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                        CompactActionItem(
                            Icons.Default.Directions, "Navigate", MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary
                        ) {
                            if (uiState.endPoint != null) {
                                try {
                                    val uri =
                                        "google.navigation:q=${uiState.endPoint!!.latitude()},${uiState.endPoint!!.longitude()}".toUri()
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, uri).setPackage(
                                            "com.google.android.apps.maps"
                                        )
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context, "Maps not installed", Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            } else Toast.makeText(context, "No Destination", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = dimensionResource(R.dimen.padding_extra_large)
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.padding_standard)
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f), Icons.Default.NearMe, "Distance", distFromMe,
                                "from you", MaterialTheme.colorScheme.primary
                            )
                            Spacer(
                                modifier = Modifier.width(dimensionResource(R.dimen.padding_medium))
                            )
                            if (uiState.endPoint != null) {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.SportsScore,
                                    "To Destination", distToEnd, "remaining",
                                    MaterialTheme.colorScheme.error
                                )
                            } else {
                                InfoCard(
                                    Modifier.weight(1f), Icons.Default.LocationOff, "Destination",
                                    "Not Set", "by host", MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCard(
                                Modifier.weight(1f),
                                if (user.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                "Battery", "${user.batteryLevel}%",
                                if (user.isCharging) "Charging" else "Normal",
                                if (user.batteryLevel < 20) Color.Red else MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(
                                modifier = Modifier.width(dimensionResource(R.dimen.padding_medium))
                            )
                            InfoCard(
                                Modifier.weight(1f), Icons.Default.Speed, "Status", user.status,
                                "Activity", MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Spacer(
                        modifier = Modifier.height(dimensionResource(R.dimen.detail_bottom_spacing))
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                MultiLinkMap(
                    viewportState = mapViewportState,
                    hasLocationPermission = true,
                    enableLocationPuck = isToday,
                    routePoints = if (uiState.sessionData?.isRouteTracingEnabled == true) historyRoute else emptyList()
                ) {
                    SessionMapContent(
                        sessionStartPoint = uiState.startPoint, startLocName = uiState.startName,
                        destinationPoint = uiState.endPoint, endLocName = uiState.endName,
                        userLocations = if (isToday && targetUserLocation != null) listOf(
                            targetUserLocation to user
                        ) else emptyList(),
                        currentUserId = uiState.currentUserId
                    )

                    allPins.forEach { (absoluteIndex, pointData) ->
                        val lat = (pointData["lat"] as Number).toDouble()
                        val lng = (pointData["lng"] as Number).toDouble()

                        val isPinSelected = selectedPinIndex == absoluteIndex
                        val pinColor =
                            if (isPinSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        val pinScale by animateFloatAsState(
                            targetValue = if (isPinSelected) 1.3f else 1f, label = "pin_scale"
                        )

                        ViewAnnotation(
                            options = viewAnnotationOptions {
                                geometry(Point.fromLngLat(lng, lat))
                                allowOverlap(true)
                            }
                        ) {
                            Surface(
                                onClick = {
                                    HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                    selectedPinIndex = absoluteIndex
                                    setFullScreen(false)
                                    if (!isTimelineExpanded) isTimelineExpanded = true

                                    scope.launch {
                                        scaffoldState.bottomSheetState.partialExpand()
                                        
                                        val displayIndex =
                                            displayPins.indexOfFirst { it.first == absoluteIndex }
                                        if (displayIndex != -1) {
                                            delay(200)
                                            horizontalListState.animateScrollToItem(displayIndex)
                                        }

                                        // Zoom Map accurately with padding
                                        val bottomOffset =
                                            with(density) { animatedPeekHeight.toPx() }.toDouble()
                                        mapViewportState.flyTo(
                                            CameraOptions.Builder()
                                                .center(Point.fromLngLat(lng, lat))
                                                .zoom(20.0)
                                                .padding(EdgeInsets(0.0, 0.0, bottomOffset, 0.0))
                                                .build()
                                        )
                                    }
                                },
                                shape = CircleShape,
                                color = pinColor,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(pinScale),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${absoluteIndex + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold, fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isFullScreen,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        Surface(
                            onClick = {
                                HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                onBackClick()
                            },
                            shape = CircleShape, color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp, modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBackIos, "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
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
                            ), end = dimensionResource(R.dimen.padding_standard)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.padding_medium)
                        )
                    ) {
                        SessionControlBar(
                            onStartClick = {
                                uiState.startPoint?.let {
                                    mapViewportState.flyTo(
                                        CameraOptions.Builder()
                                            .center(it)
                                            .zoom(16.0)
                                            .build()
                                    )
                                }
                            },
                            onUserClick = {
                                selectedPinIndex = -1
                                targetUserLocation?.let {
                                    mapViewportState.flyTo(
                                        CameraOptions.Builder()
                                            .center(it)
                                            .zoom(16.0)
                                            .build()
                                    )
                                }
                            },
                            onEndClick = {
                                uiState.endPoint?.let {
                                    mapViewportState.flyTo(
                                        CameraOptions.Builder()
                                            .center(it)
                                            .zoom(16.0)
                                            .build()
                                    )
                                }
                            },
                            showUserButton = userId != uiState.currentUserId
                        )

                        FloatingActionButton(
                            onClick = { setFullScreen(!isFullScreen) },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_box_size))
                        ) {
                            Icon(
                                if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                null
                            )
                        }

                        if (isToday) {
                            MyLocationFab(
                                onClick = {
                                    mapViewportState.flyTo(
                                        CameraOptions.Builder()
                                            .center(myRealPoint)
                                            .zoom(16.0)
                                            .build()
                                    )
                                })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableHorizontalTimelineCard(
    isToday: Boolean,
    displayPins: List<Pair<Int, Map<String, Any>>>, // Pair contains (AbsoluteIndex, PointData)
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    isNewestFirst: Boolean,
    onSortToggle: () -> Unit,
    listState: LazyListState,
    selectedPinIndex: Int,
    onItemClick: (Int, Point) -> Unit
) {
    val hasData = displayPins.isNotEmpty() || isToday
    val title = if (isToday) "Live Tracking" else "History Data"
    val value =
        if (isToday) "Currently Active" else if (hasData) "Data Available" else "Data Not Available"
    val icon =
        if (isToday) Icons.Default.Timeline else if (hasData) Icons.Default.History else Icons.Default.CloudOff
    val tint = if (isToday) Color(
        0xFF048848
    ) else if (hasData) MaterialTheme.colorScheme.primary else Color.Gray

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(300)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_standard)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (displayPins.isNotEmpty()) onExpandToggle()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isExpanded) "${displayPins.size} Location Pins Saved" else value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isExpanded && displayPins.isNotEmpty()) {
                    var showSortMenu by remember { mutableStateOf(false) }

                    Box {
                        Surface(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                )
                            ),
                            color = Color.Transparent,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Newest First",
                                        fontWeight = if (isNewestFirst) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isNewestFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    if (!isNewestFirst) onSortToggle()
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Oldest First",
                                        fontWeight = if (!isNewestFirst) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isNewestFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    if (isNewestFirst) onSortToggle()
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                if (displayPins.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExpanded && displayPins.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayPins, key = { it.first }) { (absoluteIndex, pointData) ->
                        val lat = (pointData["lat"] as Number).toDouble()
                        val lng = (pointData["lng"] as Number).toDouble()

                        PillarTimelineItem(
                            absoluteIndex = absoluteIndex,
                            pointData = pointData,
                            isSelected = selectedPinIndex == absoluteIndex,
                            onClick = {
                                onItemClick(absoluteIndex, Point.fromLngLat(lng, lat))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PillarTimelineItem(
    absoluteIndex: Int, pointData: Map<String, Any>, isSelected: Boolean, onClick: () -> Unit
) {
    val timestamp = (pointData["timestamp"] as? Number)?.toLong() ?: 0L

    val timeParts = remember(timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        sdf.split(" ")
    }

    val timeString = timeParts.getOrNull(0) ?: ""
    val amPmString = timeParts.getOrNull(1) ?: ""

    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val animatedColor = remember { Animatable(Color.Transparent) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            animatedColor.animateTo(highlightColor, animationSpec = tween(200))
            delay(1500)
            animatedColor.animateTo(Color.Transparent, animationSpec = tween(500))
        } else {
            animatedColor.animateTo(Color.Transparent, animationSpec = tween(200))
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = animatedColor.value.copy(
            alpha = if (animatedColor.value == Color.Transparent) 0f else 1f
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(52.dp)
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${absoluteIndex + 1}", color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold, fontSize = 11.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                timeString,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                amPmString,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RowScope.CompactActionItem(
    icon: ImageVector, label: String, color: Color, iconColor: Color, onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Surface(
            onClick = onClick, shape = RoundedCornerShape(8.dp), color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, contentDescription = label, tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
        Text(
            label, style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp, fontWeight = FontWeight.Medium
            ), color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}