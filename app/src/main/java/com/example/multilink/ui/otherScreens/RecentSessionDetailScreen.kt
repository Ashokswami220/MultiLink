package com.example.multilink.ui.otherScreens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentSessionDetailScreen(
    sessionId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(""))
    val recentHistory by viewModel.recentSessions.collectAsState()

    val session = recentHistory.find { it.id == sessionId }

    if (session == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Session not found or deleted.", color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val reasonLower = session.completionReason.lowercase()
    val (statusIcon, iconTint, iconBgColor) = remember(reasonLower) {
        when {
            reasonLower.contains("removed") || reasonLower.contains("kicked") -> Triple(
                Icons.Default.PersonRemove, Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.15f)
            )

            reasonLower.contains("left") -> Triple(
                Icons.AutoMirrored.Filled.ExitToApp, Color(0xFFF59E0B),
                Color(0xFFF59E0B).copy(alpha = 0.15f)
            )

            reasonLower.contains("you ended") -> Triple(
                Icons.Outlined.DeleteSweep, Color(0xFF3B82F6), Color(0xFF3B82F6).copy(alpha = 0.15f)
            )

            reasonLower.contains("admin") || reasonLower.contains("host") -> Triple(
                Icons.Default.CheckCircleOutline, Color(0xFF10B981),
                Color(0xFF10B981).copy(alpha = 0.15f)
            )

            reasonLower.contains("expire") || reasonLower.contains("time") -> Triple(
                Icons.Default.TimerOff, Color(0xFF8B5CF6), Color(0xFF8B5CF6).copy(alpha = 0.15f)
            )

            else -> Triple(
                Icons.Default.CheckCircleOutline, Color(0xFF10B981),
                Color(0xFF10B981).copy(alpha = 0.15f)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBackIosNew, contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onBackClick()
                            scope.launch {
                                viewModel.deleteRecentSession(session.id)
                                Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val minScrollHeight = this.maxHeight + 1.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minScrollHeight)
                ) {
                    // --- TOP HEADER (COMPRESSED) ---
                    Row(
                        modifier = Modifier.padding(
                            start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp), // Slightly sharper
                            color = iconBgColor,
                            modifier = Modifier.size(52.dp) // Slightly smaller
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    statusIcon, null, tint = iconTint,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title.ifEmpty { "Unnamed Session" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hosted by ${session.hostName.ifEmpty { "Unknown" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = session.completedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.8f
                                )
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    // --- STATUS & METRICS (COMPRESSED) ---
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        DetailRow(
                            "Status", session.completionReason.ifEmpty { "Completed" }, iconTint
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow("Time Spent", session.duration)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow("Total Distance", session.totalDistance)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow("Participants", session.participants)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    // --- LOCATIONS (CARDS REMOVED, FLAT UI) ---
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                        Text(
                            "ROUTE INFO", style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                            ), color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        LocationClickableItem(
                            icon = Icons.Outlined.RadioButtonUnchecked,
                            label = "Start Point",
                            locationName = session.startLoc.ifEmpty { "Location not selected by host" },
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                openMapIntent(
                                    context, session.startLat, session.startLng, session.startLoc
                                )
                            }
                        )

                        // The connecting line
                        Box(
                            modifier = Modifier
                                .padding(start = 11.dp)
                                .height(16.dp)
                                .width(2.dp)
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                        )

                        LocationClickableItem(
                            icon = Icons.Default.Place,
                            label = "Destination",
                            locationName = session.endLoc.ifEmpty { "Location not selected by host" },
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                openMapIntent(
                                    context, session.endLat, session.endLng, session.endLoc
                                )
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    // --- HOST CONTACT (CARDS REMOVED, FLAT UI) ---
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                        Text(
                            "HOST CONTACT", style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                            ), color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = session.hostName.ifEmpty { "Unknown Host" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Phone, null, modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            SelectionContainer {
                                Text(
                                    session.hostPhone, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Email, null, modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            SelectionContainer {
                                Text(
                                    session.hostEmail, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

fun openMapIntent(context: Context, lat: Double?, lng: Double?, label: String) {
    if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) {
        Toast.makeText(context, "Coordinates not available", Toast.LENGTH_SHORT)
            .show()
        return
    }
    val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

@Composable
fun LocationClickableItem(
    icon: ImageVector, label: String, locationName: String, iconColor: Color, onClick: () -> Unit
) {
    val isAvailable =
        locationName.isNotEmpty() && !locationName.contains("not selected", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAvailable) { onClick() }
            .padding(vertical = 4.dp), // Tighter vertical touch padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = locationName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        if (isAvailable) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.Default.Directions, null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}