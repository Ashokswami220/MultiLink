package com.example.multilink.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer // ⭐ follow this thing for all upcoming prompts
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.*
import com.example.multilink.R
import com.example.multilink.ui.navigation.MultiLinkRoutes
import com.example.multilink.utils.openAppSettings
import com.example.multilink.utils.openLocationSettings
import com.example.multilink.utils.rememberGpsEnabledState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun GlobalTrackingBlocker(
    currentRoute: String?,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isExemptScreen = currentRoute == MultiLinkRoutes.LOGIN ||
            currentRoute == MultiLinkRoutes.COMPLETE_PROFILE ||
            currentRoute == null

    val isGpsEnabled by rememberGpsEnabledState()

    var hasLocationPermission by remember { mutableStateOf(true) }
    var hasNotificationPermission by remember { mutableStateOf(true) }

    val checkPermissions = {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    LaunchedEffect(isExemptScreen) {
        if (!isExemptScreen) {
            checkPermissions()
            val permissionsToRequest = mutableListOf<String>()
            if (!hasLocationPermission) permissionsToRequest.add(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (!isExemptScreen && (!hasLocationPermission || !isGpsEnabled || !hasNotificationPermission)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                    .zIndex(100f)
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    !hasLocationPermission -> {
                        BlockingScreenContent(
                            icon = Icons.Default.LocationOff,
                            featureName = "Location Access",
                            actionMessage = "we need this permission to find you on the map.",
                            buttonText = "Fix Permissions",
                            onButtonClick = { context.openAppSettings() }
                        )
                    }

                    !isGpsEnabled -> {
                        BlockingScreenContent(
                            icon = Icons.Default.GpsOff,
                            featureName = "Device GPS",
                            actionMessage = "please turn it on so your friends can see your live location.",
                            buttonText = "Turn On GPS",
                            onButtonClick = { context.openLocationSettings() }
                        )
                    }

                    !hasNotificationPermission -> {
                        BlockingScreenContent(
                            icon = Icons.Default.NotificationsOff,
                            featureName = "Notifications",
                            actionMessage = "we need this to keep tracking alive in the background.",
                            buttonText = "Allow Notifications",
                            onButtonClick = { context.openAppSettings() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockingScreenContent(
    icon: ImageVector,
    featureName: String,
    actionMessage: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val userName = auth.currentUser?.displayName?.split(" ")
        ?.firstOrNull() ?: "there"

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.looking_around))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    val (showReasonDialog, setShowReasonDialog) = remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val lottieSizeDp = 100.dp
    val lottieSizePx = with(density) { lottieSizeDp.toPx() }

    val offsetX = remember { Animatable(-lottieSizePx) }

    LaunchedEffect(trackWidthPx) {
        if (trackWidthPx > 0f) {
            while (isActive) {
                offsetX.snapTo(-lottieSizePx)
                offsetX.animateTo(
                    targetValue = trackWidthPx,
                    // ⭐ follow this thing for all upcoming prompts: Increased duration to 6000ms for a slower walk
                    animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
                )
            }
        }
    }

    if (showReasonDialog) {
        AlertDialog(
            onDismissRequest = { setShowReasonDialog(false) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Why is this needed?") },
            text = {
                Text(
                    actionMessage,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { setShowReasonDialog(false) }) {
                    Text("Got it")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {

            // --- TOP ROW: Walking Animation Stage ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .onGloballyPositioned { coordinates ->
                        trackWidthPx = coordinates.size.width.toFloat()
                    }
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .size(lottieSizeDp)
                        .align(Alignment.BottomStart)
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        // ⭐ follow this thing for all upcoming prompts: scaleX = -1f mirrors the animation horizontally!
                        .graphicsLayer { scaleX = -1f }
                )
            }

            // --- BOTTOM SECTION: Left-Aligned Info & Button ---
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$featureName disabled",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    IconButton(onClick = { setShowReasonDialog(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Why?",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ⭐ follow this thing for all upcoming prompts: Removed excess text and kept it short and sweet.
                Text(
                    text = "Hey $userName,",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "It seems you turned off $featureName.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onButtonClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}