package com.example.multilink.ui.tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.BuildConfig
import com.example.multilink.repo.RouteRepository
import com.example.multilink.repo.RouteResult
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.MultiLinkMap
import com.example.multilink.ui.components.SessionMapContent
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.*
import androidx.core.net.toUri
import com.example.multilink.repo.RealtimeRepository
import kotlinx.coroutines.launch

object NavigationCache {
    var sessionId: String? = null
    var currentRoute: RouteResult? = null
}

@Composable
fun SoloNavigationScreen(
    sessionId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()
    val token = BuildConfig.MAPBOX_ACCESS_TOKEN
    val routeRepository = remember { RouteRepository(token) }

    val repository = remember { RealtimeRepository() }
    val scope = rememberCoroutineScope()

    val currentUserId =
        remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var isArrivalEnabled by remember { mutableStateOf(false) }
    var hasArrived by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        repository.getSessionDetails(sessionId)
            .collect { data ->
                isArrivalEnabled = data["isArrivalTrackingEnabled"] as? Boolean ?: false
            }
    }

    LaunchedEffect(sessionId, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val userRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("sessions")
                .child(sessionId)
                .child("users")
                .child(currentUserId)
                .child("hasArrived")
            userRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    hasArrived = snapshot.getValue(Boolean::class.java) ?: false
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }

    var routeResult by remember {
        mutableStateOf(
            if (NavigationCache.sessionId == sessionId) NavigationCache.currentRoute else null
        )
    }
    val serviceLocationState by LocationService.currentLocation.collectAsState()
    var localLocationState by remember { mutableStateOf<Location?>(null) }

    var isNavigating by rememberSaveable { mutableStateOf(true) }
    var is3DMode by rememberSaveable { mutableStateOf(true) }
    var isVoiceEnabled by rememberSaveable { mutableStateOf(true) }

    // Internal interaction states (don't strictly need saving across rotation)
    var isInteractionEnabled by remember { mutableStateOf(false) }
    var hasInitialFlyToDone by remember { mutableStateOf(false) }
    var isRerouting by remember { mutableStateOf(false) }
    var isViewMenuExpanded by remember { mutableStateOf(false) }

    var showTooFarDialog by remember { mutableStateOf(false) }
    var showArrivedConfirmDialog by remember { mutableStateOf(false) }

    val handleBackPress = {
        if (isNavigating) {
            isNavigating = false
        } else {
            onBackClick()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        tts = textToSpeech
        onDispose { textToSpeech.shutdown() }
    }

    LaunchedEffect(Unit) {
        delay(700)
        isInteractionEnabled = true
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

    var lastKnownLoc by rememberSaveable { mutableStateOf<Pair<Double, Double>?>(null) }
    var lastKnownBearing by rememberSaveable { mutableDoubleStateOf(0.0) }

    val myRealPoint = remember(serviceLocationState, localLocationState) {
        val loc = serviceLocationState ?: localLocationState
        if (loc != null) {
            lastKnownLoc = Pair(loc.longitude, loc.latitude)
            lastKnownBearing = loc.bearing.toDouble()
            Point.fromLngLat(loc.longitude, loc.latitude)
        } else {
            lastKnownLoc?.let { Point.fromLngLat(it.first, it.second) }
        }
    }

    val currentSpeed = remember(serviceLocationState, localLocationState) {
        val loc = serviceLocationState ?: localLocationState
        loc?.speed ?: 0f
    }

    val currentBearing = remember(serviceLocationState, localLocationState) {
        val loc = serviceLocationState ?: localLocationState
        if (loc != null) loc.bearing.toDouble() else lastKnownBearing
    }

    val distToDestMeters = remember(myRealPoint, uiState.endPoint) {
        if (myRealPoint != null && uiState.endPoint != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                myRealPoint.latitude(), myRealPoint.longitude(), uiState.endPoint!!.latitude(),
                uiState.endPoint!!.longitude(), results
            )
            results[0]
        } else Float.MAX_VALUE
    }

    val handleArrivedClick = {
        if (uiState.endPoint == null) {
            Toast.makeText(context, "Destination not set by host.", Toast.LENGTH_SHORT)
                .show()
        } else if (distToDestMeters > 200f) {
            showTooFarDialog = true
        } else {
            showArrivedConfirmDialog = true
        }
    }

    fun calculateRouteBearing(user: Point, target: Point): Double {
        val lat1 = Math.toRadians(user.latitude())
        val lon1 = Math.toRadians(user.longitude())
        val lat2 = Math.toRadians(target.latitude())
        val lon2 = Math.toRadians(target.longitude())
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun distanceBetween(p1: Point, p2: Point): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude(), results
        )
        return results[0]
    }

    fun getTurnIcon(modifier: String?): ImageVector {
        return when (modifier) {
            "right", "sharp right", "slight right" -> Icons.Default.TurnRight
            "left", "sharp left", "slight left" -> Icons.Default.TurnLeft
            else -> Icons.Default.ArrowUpward
        }
    }

    // Fetch Route & Cache it
    LaunchedEffect(myRealPoint, uiState.endPoint) {
        if (myRealPoint != null && uiState.endPoint != null && routeResult == null) {
            val fetchedRoute = routeRepository.getNavigationRoute(myRealPoint!!, uiState.endPoint!!)
            if (fetchedRoute != null) {
                routeResult = fetchedRoute
                NavigationCache.sessionId = sessionId
                NavigationCache.currentRoute = fetchedRoute
            }
        }
    }

    // Off-Route Detection
    LaunchedEffect(myRealPoint) {
        if (isNavigating && myRealPoint != null && routeResult != null && !isRerouting && uiState.endPoint != null) {
            var minDistToLine = Float.MAX_VALUE
            routeResult!!.routePoints.forEach { point ->
                val dist = distanceBetween(myRealPoint!!, point)
                if (dist < minDistToLine) minDistToLine = dist
            }

            if (minDistToLine > 60f) {
                isRerouting = true
                if (isVoiceEnabled) tts?.speak("Rerouting", TextToSpeech.QUEUE_FLUSH, null, null)
                val newRoute = routeRepository.getNavigationRoute(myRealPoint!!, uiState.endPoint!!)
                if (newRoute != null) {
                    routeResult = newRoute
                    NavigationCache.currentRoute = newRoute
                }
                isRerouting = false
            }
        }
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(75.7950, 26.9190))
            zoom(14.0)
        }
    }

    val stepData = remember(myRealPoint, routeResult) {
        if (myRealPoint == null || routeResult == null) return@remember null
        var closestDist = Float.MAX_VALUE
        var closestIndex = 0
        routeResult!!.steps.forEachIndexed { index, step ->
            val dist = distanceBetween(myRealPoint, step.location)
            if (dist < closestDist) {
                closestDist = dist
                closestIndex = index
            }
        }

        val targetIndex =
            if (closestDist < 30f && closestIndex + 1 < routeResult!!.steps.size) closestIndex + 1 else closestIndex
        val secondaryIndex = if (targetIndex + 1 < routeResult!!.steps.size) targetIndex + 1 else -1

        Pair(
            routeResult!!.steps[targetIndex],
            if (secondaryIndex != -1) routeResult!!.steps[secondaryIndex] else null
        )
    }

    val upcomingStep = stepData?.first
    val secondaryStep = stepData?.second

    val currentInstruction = upcomingStep?.instruction
    LaunchedEffect(currentInstruction) {
        if (isNavigating && currentInstruction != null && !isRerouting && isVoiceEnabled) {
            tts?.speak(currentInstruction, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // This runs instantly when the button is pressed, regardless of movement.
    LaunchedEffect(is3DMode) {
        if (hasInitialFlyToDone) {
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .pitch(if (is3DMode) 60.0 else 0.0)
                    .build()
            )
        }
    }

    LaunchedEffect(myRealPoint, isNavigating, upcomingStep) {
        if (isNavigating && myRealPoint != null) {
            val targetBearing = if (currentSpeed > 1f && currentBearing != 0.0) {
                currentBearing
            } else if (upcomingStep != null) {
                calculateRouteBearing(myRealPoint, upcomingStep.location)
            } else {
                currentBearing
            }

            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(myRealPoint)
                    .bearing(targetBearing)
                    .pitch(if (is3DMode) 60.0 else 0.0)
                    .zoom(17.5)
                    .build()
            )
        } else if (myRealPoint != null && !hasInitialFlyToDone) {
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(myRealPoint)
                    .zoom(16.0)
                    .pitch(if (is3DMode) 60.0 else 0.0)
                    .build()
            )
            hasInitialFlyToDone = true
        }
    }

    if (showTooFarDialog) {
        AlertDialog(
            onDismissRequest = { showTooFarDialog = false },
            title = { Text("Too Far Away") },
            text = {
                Text(
                    "You must be within 200 meters of the destination to check in. You are currently ${distToDestMeters.toInt()} meters away."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTooFarDialog = false }) {
                    Text(
                        "Got it"
                    )
                }
            }
        )
    }

    if (showArrivedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showArrivedConfirmDialog = false },
            title = { Text("Mark as Arrived?") },
            text = {
                Text(
                    "This will automatically pause your location tracking and notify everyone that you have reached the destination."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showArrivedConfirmDialog = false
                    scope.launch { repository.markUserAsArrived(sessionId, uiState.currentUserId) }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showArrivedConfirmDialog = false }) {
                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }

    val turnDistanceFloat = remember(myRealPoint, upcomingStep) {
        if (myRealPoint != null && upcomingStep != null) {
            distanceBetween(myRealPoint, upcomingStep.location)
        } else Float.MAX_VALUE
    }

    val turnDistanceText = remember(turnDistanceFloat) {
        if (turnDistanceFloat != Float.MAX_VALUE) {
            if (turnDistanceFloat > 1000) "%.1f km".format(
                turnDistanceFloat / 1000f
            ) else "${turnDistanceFloat.toInt()} m"
        } else "..."
    }

    val turnIcon = remember(upcomingStep) { getTurnIcon(upcomingStep?.modifier) }

    val topBarColor by animateColorAsState(
        targetValue = when {
            isRerouting -> Color(0xFFB71C1C)
            turnDistanceFloat < 150f -> Color(0xFFE65100)
            else -> Color(0xFF1B5E20)
        },
        animationSpec = tween(500)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        MultiLinkMap(
            viewportState = mapViewportState,
            hasLocationPermission = true,
            enableLocationPuck = true,
            followUser = false, // Must be false for our custom 3D logic to control the camera
            routePoints = routeResult?.routePoints ?: emptyList()
        ) {
            SessionMapContent(
                sessionStartPoint = null,
                startLocName = "",
                destinationPoint = uiState.endPoint,
                endLocName = uiState.endName,
                userLocations = emptyList(),
                currentUserId = "solo_mode"
            )
        }

        AnimatedVisibility(
            visible = isNavigating,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(if (isPortrait) Alignment.TopCenter else Alignment.TopStart)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isPortrait) 1f else 0.45f)
                    .padding(16.dp)
                    .statusBarsPadding(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = topBarColor),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    IconButton(onClick = handleBackPress, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = turnIcon, contentDescription = null,
                        modifier = Modifier.size(48.dp), tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRerouting) "Rerouting..." else turnDistanceText,
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                        )
                        Text(
                            text = upcomingStep?.instruction ?: "Stay on route",
                            fontSize = 16.sp, fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        if (secondaryStep != null && turnDistanceFloat < 200f && !isRerouting) {
                            Text(
                                text = "Then: ${secondaryStep.instruction}",
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp), maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (!isNavigating) {
            Surface(
                onClick = handleBackPress,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos, "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (isPortrait) 105.dp else 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { isVoiceEnabled = !isVoiceEnabled },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            ) {
                Icon(
                    if (isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    "Toggle Voice"
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(visible = isViewMenuExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallFloatingActionButton(
                            onClick = { is3DMode = true; isViewMenuExpanded = false },
                            containerColor = if (is3DMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (is3DMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        ) { Text("3D", fontWeight = FontWeight.Bold) }

                        SmallFloatingActionButton(
                            onClick = { is3DMode = false; isViewMenuExpanded = false },
                            containerColor = if (!is3DMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (!is3DMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        ) { Text("2D", fontWeight = FontWeight.Bold) }
                    }
                }
                FloatingActionButton(
                    onClick = { isViewMenuExpanded = !isViewMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape
                ) { Icon(if (is3DMode) Icons.Default.ViewInAr else Icons.Default.Layers, null) }
            }

            FloatingActionButton(
                onClick = {
                    myRealPoint?.let {
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(it)
                                .zoom(17.0)
                                .pitch(if (is3DMode) 60.0 else 0.0)
                                .build()
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            ) { Icon(Icons.Default.MyLocation, null) }

            Button(
                onClick = {
                    uiState.endPoint?.let { point ->
                        try {
                            val uri =
                                "google.navigation:q=${point.latitude()},${point.longitude()}".toUri()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, uri).setPackage(
                                    "com.google.android.apps.maps"
                                )
                            )
                        } catch (_: Exception) {
                            Toast.makeText(context, "Maps not found", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open G Maps", fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            modifier = Modifier
                .align(if (isPortrait) Alignment.BottomCenter else Alignment.BottomStart)
                .fillMaxWidth(if (isPortrait) 1f else 0.45f)
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            val distTotal = routeResult?.distanceMeters?.let {
                if (it > 1000) "%.1f km".format(
                    it / 1000
                ) else "${it.toInt()} m"
            } ?: "--"
            val eta =
                routeResult?.durationSeconds?.let { s -> if (s / 60 > 60) "${(s / 3600).toInt()}h ${(s % 3600 / 60).toInt()}m" else "${(s / 60).toInt()} min" }
                    ?: "--"

            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = eta, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = distTotal, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isArrivalEnabled && !hasArrived) {
                    Button(
                        onClick = handleArrivedClick, modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.TaskAlt, null, modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        isNavigating = !isNavigating
                        if (isNavigating) {
                            myRealPoint?.let {
                                mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(it)
                                        .zoom(17.5)
                                        .pitch(if (is3DMode) 60.0 else 0.0)
                                        .build()
                                )
                            }
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNavigating) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                        contentColor = if (isNavigating) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isNavigating) Icons.Default.Close else Icons.Default.Navigation, null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isNavigating) "Exit" else "Start", fontSize = 14.sp)
                }
            }
        }

        if (!isInteractionEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { })
        }
    }
}