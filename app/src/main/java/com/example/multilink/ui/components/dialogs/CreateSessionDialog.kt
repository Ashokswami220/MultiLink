package com.example.multilink.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.multilink.model.SessionData
import com.example.multilink.ui.components.location.LocationPicker
import com.mapbox.geojson.Point
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onSuccess: (SessionData, Boolean) -> Unit,
    existingSession: SessionData? = null
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }
    val isDark = isSystemInDarkTheme()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val context = LocalContext.current

    val inputSurfaceColor =
        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface


    // --- STATE ---
    var title by rememberSaveable { mutableStateOf("") }
    var isTitleError by remember { mutableStateOf(false) }
    var isPeopleError by remember { mutableStateOf(false) }
    var fromLoc by rememberSaveable { mutableStateOf("") }
    var toLoc by rememberSaveable { mutableStateOf("") }
    var isHours by rememberSaveable { mutableStateOf(true) }
    var durationVal by rememberSaveable { mutableStateOf("2") }
    var isDurationExpanded by rememberSaveable { mutableStateOf(false) }
    var peopleText by rememberSaveable { mutableStateOf("5") }
    var mapSelectionMode by rememberSaveable { mutableStateOf<String?>(null) }
    var isSharingMyLocation by rememberSaveable { mutableStateOf(true) }
    var isUsersVisible by rememberSaveable { mutableStateOf(true) }
    var isSharingAllowed by rememberSaveable { mutableStateOf(true) }

    var fromPoint by remember(existingSession) {
        mutableStateOf(
            if (existingSession != null && (existingSession.startLat ?: 0.0) != 0.0) {
                Point.fromLngLat(existingSession.startLng ?: 0.0, existingSession.startLat ?: 0.0)
            } else null
        )
    }
    var toPoint by remember(existingSession) {
        mutableStateOf(
            if (existingSession != null && (existingSession.endLat ?: 0.0) != 0.0) {
                Point.fromLngLat(existingSession.endLng ?: 0.0, existingSession.endLat ?: 0.0)
            } else null
        )
    }

    // Pre-fill Data
    LaunchedEffect(existingSession) {
        if (existingSession != null) {
            title = existingSession.title
            fromLoc = existingSession.fromLocation
            toLoc = existingSession.toLocation
            durationVal = existingSession.durationVal
            isHours = existingSession.durationUnit == "Hrs"
            peopleText = existingSession.maxPeople
            isUsersVisible = existingSession.isUsersVisible
            isSharingAllowed = existingSession.isSharingAllowed
            isSharingMyLocation = existingSession.isHostSharing
            if ((existingSession.startLat ?: 0.0) != 0.0) {
                fromPoint = Point.fromLngLat(existingSession.startLng!!, existingSession.startLat!!)
            }
            if ((existingSession.endLat ?: 0.0) != 0.0) {
                toPoint = Point.fromLngLat(existingSession.endLng!!, existingSession.endLat!!)
            }
        } else {
            title = ""
            fromLoc = ""
            toLoc = ""
            durationVal = "2"
            isHours = true
            peopleText = "5"
            isUsersVisible = true
            isSharingAllowed = true
            isSharingMyLocation = true
            fromPoint = null
            toPoint = null
        }
    }

    val durationOptions = remember(isHours) {
        if (isHours) (1..24).map { it.toString() } else (1..3).map { it.toString() }
    }

    fun triggerShake() {
        scope.launch {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis =
                        400; 0f at 0; (-10f) at 50; 10f at 100; (-10f) at 150; 0f at 200
                }
            )
        }
    }

    Dialog(
        onDismissRequest = {
            if (mapSelectionMode != null) mapSelectionMode = null else onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (mapSelectionMode == null) onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 800.dp)
                    .clickable(enabled = false) {}
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- HEADER ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (existingSession != null) "Edit Session" else if (pagerState.currentPage == 0) "New Session" else "Permissions",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- PAGER ---
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = true,
                        verticalAlignment = Alignment.Top,
                        pageSpacing = 16.dp
                    ) { page ->

                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (page == 0) {
                                // === STEP 1 ===
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = {
                                        title = it; if (it.isNotEmpty()) isTitleError = false
                                    },
                                    label = { Text("Session Name") },
                                    placeholder = { Text("e.g. Goa Trip") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    isError = isTitleError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = Color.Transparent,
                                        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                            alpha = 0.1f
                                        )
                                    ),
                                    trailingIcon = {
                                        if (title.isNotEmpty()) {
                                            IconButton(onClick = { title = "" }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                LocationSelector(
                                    label = "Start Point",
                                    value = fromLoc.ifEmpty { "Select on Map" },
                                    Icons.Outlined.MyLocation,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClear = if (fromLoc.isNotEmpty()) {
                                        {
                                            fromLoc = ""
                                            fromPoint = null
                                        }
                                    } else null
                                ) {
                                    mapSelectionMode = "from"
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LocationSelector(
                                    label = "Destination",
                                    value = toLoc.ifEmpty { "Select on Map" },
                                    Icons.Outlined.LocationOn,
                                    iconTint = MaterialTheme.colorScheme.error,
                                    onClear = if (toLoc.isNotEmpty()) {
                                        {
                                            toLoc = ""
                                            toPoint = null
                                        }
                                    } else null
                                ) {
                                    mapSelectionMode = "to"
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                // Duration
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = inputSurfaceColor,
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        Row(Modifier.padding(4.dp)) {
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (isHours) MaterialTheme.colorScheme.primary else Color.Transparent
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        ); isHours = true; durationVal = "2"
                                                    }
                                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    "Hrs",
                                                    color = if (isHours) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (!isHours) MaterialTheme.colorScheme.primary else Color.Transparent
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        ); isHours = false; durationVal = "1"
                                                    }
                                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    "Days",
                                                    color = if (!isHours) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = isDurationExpanded,
                                        onExpandedChange = {
                                            isDurationExpanded = !isDurationExpanded
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = durationVal,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = isDurationExpanded
                                                )
                                            },
                                            modifier = Modifier.menuAnchor(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedContainerColor = Color.Transparent
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = isDurationExpanded,
                                            onDismissRequest = { isDurationExpanded = false },
                                            modifier = Modifier.heightIn(max = 250.dp)
                                        ) {
                                            durationOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        durationVal = option; isDurationExpanded =
                                                        false
                                                    })
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Participants
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = peopleText,
                                        onValueChange = { input ->
                                            isPeopleError = false
                                            if (input.all { c -> c.isDigit() }) {
                                                val num = input.toIntOrNull()
                                                peopleText =
                                                    if (num == null) input else if (num <= 50) input else "50"
                                            }
                                        },
                                        label = { Text("Participants") },
                                        leadingIcon = { Icon(Icons.Default.Person, null) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        isError = isPeopleError,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedContainerColor = Color.Transparent,
                                            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                                alpha = 0.1f
                                            )
                                        )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    RepeatingIconButton(
                                        onClick = {
                                            val n = peopleText.toIntOrNull()
                                                ?: 0; if (n > 1) peopleText = (n - 1).toString()
                                        },
                                        containerColor = inputSurfaceColor
                                    ) {
                                        Icon(Icons.Default.Remove, null)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    RepeatingIconButton(
                                        onClick = {
                                            val n = peopleText.toIntOrNull()
                                                ?: 0; if (n < 50) peopleText = (n + 1).toString()
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }
                            } else {
                                // === STEP 2 ===
                                Spacer(modifier = Modifier.height(32.dp))
                                SettingToggleCard(
                                    "Share my location",
                                    "You are visible to others",
                                    Icons.Outlined.MyLocation,
                                    isSharingMyLocation,
                                    { isSharingMyLocation = it },
                                    if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(
                                        0.7f
                                    ) else MaterialTheme.colorScheme.primaryContainer.copy(
                                        0.5f
                                    ),
                                    MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                SettingToggleCard(
                                    "Allow Peer Tracking",
                                    "Users can see each other",
                                    if (isUsersVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    isUsersVisible,
                                    { isUsersVisible = it },
                                    if (isDark) MaterialTheme.colorScheme.tertiaryContainer.copy(
                                        0.7f
                                    ) else MaterialTheme.colorScheme.tertiaryContainer.copy(0.5f),
                                    MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                SettingToggleCard(
                                    "Allow Sharing",
                                    "Users can invite others",
                                    Icons.Default.Share,
                                    isSharingAllowed,
                                    { isSharingAllowed = it },
                                    if (isDark) MaterialTheme.colorScheme.secondaryContainer.copy(
                                        0.7f
                                    )
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(70.dp))
                            }

                            //  BUTTONS INSIDE SCROLLABLE AREA
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(2) { iteration ->
                                    val color =
                                        if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    Box(
                                        Modifier
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (title.trim()
                                            .isEmpty()
                                    ) {
                                        isTitleError = true
                                        triggerShake()
                                        if (pagerState.currentPage == 1) scope.launch {
                                            pagerState.animateScrollToPage(
                                                0
                                            )
                                        }
                                    } else {
                                        if (pagerState.currentPage == 0) {
                                            scope.launch { pagerState.animateScrollToPage(1) }
                                        } else {

                                            val newLimit = peopleText.toIntOrNull() ?: 0
                                            val currentActive = existingSession?.activeUsers ?: 0

                                            if (existingSession != null && newLimit in 1..<currentActive) {
                                                isPeopleError = true
                                                triggerShake()

                                                if (pagerState.currentPage == 1) {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(
                                                            0
                                                        )
                                                    }
                                                }

                                                android.widget.Toast.makeText(
                                                    context,
                                                    "You already have $currentActive users joined",
                                                    android.widget.Toast.LENGTH_LONG
                                                )
                                                    .show()
                                                return@Button
                                            }

                                            onSuccess(
                                                SessionData(
                                                    id = existingSession?.id ?: "",
                                                    title = title.trim(),
                                                    fromLocation = fromLoc,
                                                    toLocation = toLoc,
                                                    startLat = fromPoint?.latitude() ?: 0.0,
                                                    startLng = fromPoint?.longitude() ?: 0.0,
                                                    endLat = toPoint?.latitude() ?: 0.0,
                                                    endLng = toPoint?.longitude() ?: 0.0,
                                                    durationVal = durationVal,
                                                    durationUnit = if (isHours) "Hrs" else "Days",
                                                    maxPeople = peopleText,
                                                    isUsersVisible = isUsersVisible,
                                                    isSharingAllowed = isSharingAllowed,
                                                    isHostSharing = isSharingMyLocation,
                                                    hostId = existingSession?.hostId ?: "",
                                                    joinCode = existingSession?.joinCode ?: "",
                                                    status = existingSession?.status ?: "Live",
                                                    hostName = existingSession?.hostName ?: "",
                                                    createdTimestamp = existingSession?.createdTimestamp
                                                        ?: 0L
                                                ), isSharingMyLocation
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .offset(x = shakeOffset.value.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                val btnText = if (pagerState.currentPage == 0) "Next"
                                else if (existingSession != null) "Update Session"
                                else "Create Link"
                                Text(text = btnText, fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Map Overlay
            AnimatedVisibility(
                visible = mapSelectionMode != null,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LocationPicker(
                        onLocationSelected = { name, point ->
                            if (mapSelectionMode == "from") {
                                fromLoc = name; fromPoint = point
                            } else {
                                toLoc = name; toPoint = point
                            }
                            mapSelectionMode = null
                        },
                        onCancel = { mapSelectionMode = null }
                    )
                }
            }
        }
    }
}


@Composable
fun RepeatingIconButton(
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .size(40.dp)
            .indication(interactionSource, ripple(bounded = true, radius = 20.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressPoint ->
                        val press = PressInteraction.Press(pressPoint)
                        interactionSource.emit(press)

                        var job: Job? = null
                        coroutineScope {
                            job = launch {
                                delay(500)
                                while (isActive) {
                                    currentOnClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    delay(100)
                                }
                            }

                            val success = tryAwaitRelease()
                            if (job.isActive) {
                                job.cancel()
                                if (success) {
                                    currentOnClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            } else {
                                job.cancel()
                            }
                            val endInteraction = if (success) {
                                PressInteraction.Release(press)
                            } else {
                                PressInteraction.Cancel(press)
                            }
                            interactionSource.emit(endInteraction)
                        }
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun SettingToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color,
    iconColor: Color
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            onCheckedChange(!isChecked)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        shape = RoundedCornerShape(20.dp),
        color = if (isChecked) checkedColor else if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (isChecked) iconColor else iconColor.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    onCheckedChange(it)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = iconColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Composable
fun LocationSelector(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    onClear: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }


            Spacer(modifier = Modifier.weight(1f))

            if (onClear != null) {
                Surface(
                    onClick = onClear,
                    shape = RoundedCornerShape(50), // Pill shape
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.height(28.dp) // Compact height
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Icon(
                Icons.Default.Map,
                null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}