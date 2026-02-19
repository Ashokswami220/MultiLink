package com.example.multilink.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.multilink.repo.RealtimeRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedJoinDialog(
    initialCode: String? = null,
    onDismiss: () -> Unit,
    onJoinConfirmed: (String) -> Unit
) {
    val repository = remember { RealtimeRepository() }
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val pagerState = rememberPagerState(pageCount = { 2 })

    // --- STATE ---
    var inputCode by rememberSaveable { mutableStateOf(initialCode ?: "") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var realSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    // Session Data
    var sessionTitle by rememberSaveable { mutableStateOf("") }
    var hostName by rememberSaveable { mutableStateOf("") }
    var hostId by rememberSaveable { mutableStateOf("") }
    var fromLoc by rememberSaveable { mutableStateOf("") }
    var toLoc by rememberSaveable { mutableStateOf("") }
    var userCount by rememberSaveable { mutableIntStateOf(0) }
    var maxLimit by rememberSaveable { mutableIntStateOf(0) }
    var duration by rememberSaveable { mutableStateOf("2 Hrs") }
    var status by rememberSaveable { mutableStateOf("Live") }

    // Contact Info
    var hostPhone by rememberSaveable { mutableStateOf("Loading...") }
    var hostEmail by rememberSaveable { mutableStateOf("Loading...") }

    var showInfo by rememberSaveable { mutableStateOf(false) }

    fun fetchAndMoveToPreview() {
        if (inputCode.length < 8) {
            errorMsg = "Code must be 8 characters."
            return
        }

        focusManager.clearFocus(force = true)
        keyboardController?.hide()

        scope.launch {
            isLoading = true
            errorMsg = null

            val resolvedId =
                if (inputCode.length > 10) inputCode else repository.getSessionIdFromCode(inputCode)

            if (resolvedId != null) {
                realSessionId = resolvedId
                repository.getSessionDetails(resolvedId)
                    .collectLatest { data ->
                        if (data.isNotEmpty()) {
                            sessionTitle = data["title"] as? String ?: "MultiLink Session"
                            hostName = data["hostName"] as? String ?: "Unknown"
                            hostId = data["hostId"] as? String ?: ""
                            fromLoc = data["fromLocation"] as? String ?: "Start"
                            toLoc = data["toLocation"] as? String ?: "End"
                            maxLimit = (data["maxPeople"] as? String)?.toIntOrNull() ?: 0

                            val dVal = data["durationVal"] as? String ?: "2"
                            val dUnit = data["durationUnit"] as? String ?: "Hrs"
                            duration = "$dVal $dUnit"
                            status = data["status"] as? String ?: "Live"

                            if (hostId.isNotEmpty()) {
                                val profile = repository.getGlobalUserProfile(hostId)
                                hostPhone = profile?.get("phone")
                                    ?.takeIf { it.isNotEmpty() } ?: "Not Available"
                                hostEmail = profile?.get("email")
                                    ?.takeIf { it.isNotEmpty() } ?: "Not Available"
                            } else {
                                hostPhone = "Not Available"
                                hostEmail = "Not Available"
                            }

                            isLoading = false
                            pagerState.animateScrollToPage(1)
                        } else {
                            isLoading = false
                            errorMsg = "Session details not found."
                        }
                    }
            } else {
                isLoading = false
                errorMsg = "Invalid Code. Please check and try again."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!initialCode.isNullOrEmpty()) fetchAndMoveToPreview()
    }

    LaunchedEffect(realSessionId) {
        if (realSessionId != null) {
            repository.getSessionUsers(realSessionId!!)
                .collectLatest { users ->
                    userCount = users.size
                }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus(true)
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 720.dp)
                    .clickable(enabled = false) {}
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- HEADER ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (pagerState.currentPage == 0) "Join Session" else "You're Invited!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (pagerState.currentPage == 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { showInfo = !showInfo },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        if (showInfo) Icons.Default.Close else Icons.Outlined.Info,
                                        "Info",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close, "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Info Card Animation
                    AnimatedVisibility(
                        visible = showInfo && pagerState.currentPage == 1,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Filled.Info, null, Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "This session shares your live location with the host and group on a secure map.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
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
                                // === PAGE 1: INPUT ===
                                Text(
                                    "Enter the 8-character code shared by the host.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = {
                                        if (it.length <= 8) inputCode = it.uppercase()
                                    },
                                    label = { Text("Session Code") },
                                    placeholder = { Text("e.g. A1B2C3D4") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { fetchAndMoveToPreview() }),
                                    isError = errorMsg != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = Color.Transparent,
                                        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                            alpha = 0.1f
                                        )
                                    )
                                )

                                AnimatedVisibility(visible = errorMsg != null) {
                                    Text(
                                        text = errorMsg ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { fetchAndMoveToPreview() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = inputCode.length >= 8 && !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (isLoading) CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    else Text("Next", fontSize = 16.sp)
                                }
                            } else {
                                // === PAGE 2: COMPACT TICKET PREVIEW ===

                                // Title & Code
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        sessionTitle,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(50),
                                    ) {
                                        Text(
                                            inputCode,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp, vertical = 4.dp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // TICKET INFO CARD
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp, MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shadowElevation = 2.dp
                                ) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // ⭐ Row 1: Host Name (0.6f) & Status (0.4f)
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            InfoStat(
                                                Icons.Default.Person, "Host", hostName,
                                                Modifier.weight(0.6f)
                                            )
                                            val statusColor =
                                                if (status == "Live") Color(0xFF4CAF50) else Color(
                                                    0xFFFF9800
                                                )
                                            InfoStat(
                                                Icons.Filled.Sensors, "Status", status,
                                                Modifier.weight(0.4f), statusColor
                                            )
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.3f
                                            )
                                        )

                                        // ⭐ Row 2: Phone (Full Width)
                                        InfoStat(
                                            Icons.Default.Phone, "Mobile", hostPhone,
                                            Modifier.fillMaxWidth()
                                        )

                                        // ⭐ Row 3: Email (Full Width)
                                        InfoStat(
                                            Icons.Default.Email, "Email", hostEmail,
                                            Modifier.fillMaxWidth()
                                        )

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.3f
                                            )
                                        )

                                        // ⭐ Row 4: Start (0.6f) & Dest (0.4f)
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            InfoStat(
                                                Icons.Default.Place, "Start", fromLoc,
                                                Modifier.weight(0.6f)
                                            )
                                            InfoStat(
                                                Icons.Default.LocationOn, "Dest", toLoc,
                                                Modifier.weight(0.4f)
                                            )
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.3f
                                            )
                                        )

                                        // ⭐ Row 5: Users (0.6f) & Duration (0.4f)
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val isFull = maxLimit in 1..userCount
                                            InfoStat(
                                                Icons.Default.Group,
                                                "Active Users",
                                                "$userCount / ${if (maxLimit == 0) "∞" else maxLimit}",
                                                Modifier.weight(0.6f),
                                                if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                            InfoStat(
                                                Icons.Default.AccessTime, "Duration", duration,
                                                Modifier.weight(0.4f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(
                                                    0
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp, MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack, null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Back")
                                    }

                                    Button(
                                        onClick = {
                                            if (realSessionId != null) onJoinConfirmed(
                                                realSessionId!!
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        enabled = (maxLimit == 0 || userCount < maxLimit)
                                    ) { Text("Join Session", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    // Removed width constraint, allowing weight to work
    Row(verticalAlignment = Alignment.Top, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = color
            )
        }
    }
}