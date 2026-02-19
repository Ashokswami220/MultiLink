package com.example.multilink.ui.components.session

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multilink.R
import com.example.multilink.model.SessionData
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun SessionCard(
    data: SessionData,
    onClick: () -> Unit,
    onStopClick: () -> Unit,
    onShareClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onEditClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val isAdmin = data.hostId == currentUserId

    val clipboardManager = LocalClipboardManager.current
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showStopWarning by remember { mutableStateOf(false) }
    var showLeaveWarning by remember { mutableStateOf(false) }

    val canShare = isAdmin || data.isSharingAllowed
    val isPaused = data.status == "Paused"
    val statusColor = if (isPaused) Color(0xFFFF9800) else Color(0xFF4CAF50)
    val statusText = if (isPaused) "PAUSED" else "LIVE"

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isPaused) 1f else 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )

    var timeLeftString by remember { mutableStateOf("Calculating...") }
    var exactEndDateTime by remember { mutableStateOf("") }
    var isExpired by remember { mutableStateOf(false) }

    LaunchedEffect(data) {
        val createdTime = data.createdTimestamp
        if (createdTime == 0L) {
            timeLeftString = "${data.durationVal} ${data.durationUnit}"
            return@LaunchedEffect
        }
        val durationVal = data.durationVal.toLongOrNull() ?: 0L
        val durationMillis = if (data.durationUnit == "Hrs") TimeUnit.HOURS.toMillis(
            durationVal
        ) else TimeUnit.DAYS.toMillis(durationVal)
        val endTime = createdTime + durationMillis
        val sdf = SimpleDateFormat("EEE, d MMM 'at' h:mm a", Locale.getDefault())
        exactEndDateTime = sdf.format(Date(endTime))

        while (true) {
            val now = System.currentTimeMillis()
            val diff = endTime - now
            if (diff <= 0) {
                timeLeftString = "Expired"; isExpired = true; break
            } else {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                val hrs = TimeUnit.MILLISECONDS.toHours(diff) % 24
                val mins = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                timeLeftString =
                    if (days > 0) "${days}d ${hrs}h left" else if (hrs > 0) "${hrs}h ${mins}m left" else "${mins}m left"
            }
            delay(60000)
        }
    }

    if (showStopWarning) {
        AlertDialog(
            onDismissRequest = { showStopWarning = false },
            title = { Text("Delete Session?") },
            text = { Text("This will permanently remove the session for everyone. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = { showStopWarning = false; onStopClick() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showStopWarning = false }) { Text("Cancel") } }
        )
    }
    // User Leave Warning
    if (showLeaveWarning) {
        AlertDialog(
            onDismissRequest = { showLeaveWarning = false },
            title = { Text("Leave Session?") },
            text = {
                Text(
                    "You will stop sharing your location. You can rejoin later using the code."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLeaveWarning = false; onStopClick() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveWarning = false }) {
                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_standard)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = { if (!isPaused) onClick() },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        data.title.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ), color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (data.joinCode.isNotEmpty() && canShare) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            clipboardManager.setText(
                                                AnnotatedString(data.joinCode)
                                            ); Toast.makeText(
                                            context, "Code Copied", Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Code: ${data.joinCode}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ), color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.ContentCopy, "Copy", Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isAdmin) "ADMIN" else "USER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold, fontSize = 11.sp
                            ),
                            color = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, statusColor.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(alpha)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText, style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold, fontSize = 11.sp
                                ), color = statusColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // Locations
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.MyLocation, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = data.fromLocation.ifEmpty {
                            stringResource(
                                id = R.string.loc_not_sel_by_host
                            )
                        },
                        style = if (data.fromLocation.isNotEmpty())
                            MaterialTheme.typography.bodyMedium
                        else
                            MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = Color.Gray
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.5.dp)
                        .height(16.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = data.toLocation.ifEmpty {
                            stringResource(
                                id = R.string.loc_not_sel_by_host
                            )
                        },
                        style = if (data.toLocation.isNotEmpty()) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (isExpired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        if (!isExpired && exactEndDateTime.isNotEmpty()) Toast.makeText(
                            context, "Ends on: $exactEndDateTime", Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Timer, null,
                            tint = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeLeftString, style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.Info, null,
                            tint = if (isExpired) MaterialTheme.colorScheme.onErrorContainer.copy(
                                alpha = 0.5f
                            ) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp, 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Group, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${data.activeUsers} / ${data.maxPeople}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ACTION BUTTONS & MENU ---
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAdmin) {
                    if (isPaused) {
                        // Admin & Paused -> RESUME (Full Width)
                        Button(
                            onClick = onResumeClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Resume", style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        // Admin & Live -> SHARE + PAUSE
                        if (canShare) {
                            Button(
                                onClick = onShareClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Share", style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Button(
                            onClick = onPauseClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pause", style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    // USER VIEW
                    if (isPaused) {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Session Paused by Host",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        if (canShare) {
                            Button(
                                onClick = onShareClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Share", style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Button(
                            onClick = { showLeaveWarning = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp, null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Leave", style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // MENU
                Spacer(modifier = Modifier.width(12.dp))
                Box {
                    FilledTonalIconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) { Icon(Icons.Default.MoreVert, "More") }

                    val itemsCount = if (isAdmin) 3 else (if (isPaused) 2 else 1) // Approx count
                    val menuHeight = (itemsCount * 48 + 16).dp
                    val anchorHeight = 50.dp
                    val totalOffset = menuHeight + anchorHeight

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        shape = RoundedCornerShape(
                            dimensionResource(id = R.dimen.corner_menu_sheet)
                        ),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        offset = DpOffset(x = 0.dp, y = -totalOffset)
                    ) {
                        // Admin: Delete at TOP
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete Session", color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { isMenuExpanded = false; showStopWarning = true }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Session Info") },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = { isMenuExpanded = false; onInfoClick() }
                        )

                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Edit Session") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { isMenuExpanded = false; onEditClick() }
                            )
                        }

                        // User Paused: Leave Option appears here
                        if (!isAdmin && isPaused) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Leave Session", color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp, null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { isMenuExpanded = false; onStopClick() }
                            )
                        }
                    }
                }
            }
        }
    }
}