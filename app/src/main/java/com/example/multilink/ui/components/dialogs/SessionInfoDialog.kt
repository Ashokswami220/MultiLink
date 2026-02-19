package com.example.multilink.ui.components.dialogs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.multilink.model.SessionData
import com.example.multilink.repo.RealtimeRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

@Composable
fun SessionInfoDialog(
    session: SessionData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RealtimeRepository() }
    val clipboardManager = LocalClipboardManager.current

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isAdmin = session.hostId == currentUserId
    val shouldShowCode = isAdmin || session.isSharingAllowed

    // --- STATE ---
    var hostEmail by rememberSaveable { mutableStateOf("Loading...") }
    var hostPhone by rememberSaveable { mutableStateOf("Loading...") }
    var timeLeftString by remember { mutableStateOf("...") }
    var isExpired by remember { mutableStateOf(false) }

    // Fetch Host Info
    LaunchedEffect(session.hostId) {
        val details = repository.getGlobalUserProfile(session.hostId)
        if (details != null) {
            hostPhone = details["phone"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
            hostEmail = details["email"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
        } else {
            hostPhone = "Unknown"
            hostEmail = "Unknown"
        }
    }

    // Timer Logic
    LaunchedEffect(session) {
        val durationVal = session.durationVal.toLongOrNull() ?: 0L
        val durationMillis = if (session.durationUnit == "Hrs") TimeUnit.HOURS.toMillis(
            durationVal
        ) else TimeUnit.DAYS.toMillis(durationVal)
        val endTime = session.createdTimestamp + durationMillis

        while (true) {
            val diff = endTime - System.currentTimeMillis()
            if (diff <= 0) {
                timeLeftString = "Expired"; isExpired = true; break
            } else {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                val hrs = TimeUnit.MILLISECONDS.toHours(diff) % 24
                val mins = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                timeLeftString = if (days > 0) "${days}d ${hrs}h left" else "${hrs}h ${mins}m left"
            }
            delay(60000)
        }
    }

    // --- NAVIGATION HELPER (Using Coordinates) ---
    fun openMap(lat: Double, lng: Double, label: String) {
        if (lat == 0.0 && lng == 0.0) {
            Toast.makeText(context, "Coordinates not available", Toast.LENGTH_SHORT)
                .show()
            return
        }
        // geo:lat,lng?q=lat,lng(Label) ensures marker drops exactly at coord
        val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to browser
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(webIntent)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- 1. HEADER (Title + Timer) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Timer Chip
                    Surface(
                        color = if (isExpired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (isExpired) Icons.Default.Warning else Icons.Outlined.Timer,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeLeftString,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 2. JOIN CODE (If applicable) ---
                if (shouldShowCode && session.joinCode.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(session.joinCode))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "JOIN CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = session.joinCode,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!session.isSharingAllowed && isAdmin) {
                                    Text(
                                        "Visible only to Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(session.joinCode))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                                    .show()
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy, "Copy",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- 3. HOST DETAILS (Redesigned: Clean Card) ---
                Text(
                    text = "HOST DETAILS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Top Row: Avatar + Name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = session.hostName.ifEmpty { "Unknown Host" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Divider
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Bottom Row: Contact Info
                        ContactItem(Icons.Outlined.Phone, hostPhone)
                        Spacer(modifier = Modifier.height(6.dp))
                        ContactItem(Icons.Outlined.Email, hostEmail)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 4. ROUTE INFO (Vertical + Navigate) ---
                Text(
                    text = "ROUTE INFO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Start Location
                    LocationItem(
                        icon = Icons.Default.TripOrigin,
                        label = "Start Point",
                        location = session.fromLocation.ifEmpty { "Not selected by host" },
                        color = MaterialTheme.colorScheme.primary,
                        onNavigate = {
                            openMap(
                                session.startLat ?: 0.0, session.startLng ?: 0.0,
                                session.fromLocation
                            )
                        }
                    )

                    // Vertical Dotted Connector (Visual)
                    Box(
                        modifier = Modifier
                            .padding(start = 11.dp) // Align with icon center
                            .height(16.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // End Location
                    LocationItem(
                        icon = Icons.Default.Place,
                        label = "Destination",
                        location = session.toLocation.ifEmpty { "Not selected by host" },
                        color = MaterialTheme.colorScheme.error,
                        onNavigate = {
                            openMap(
                                session.endLat ?: 0.0, session.endLng ?: 0.0, session.toLocation
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 5. CLOSE BUTTON ---
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ContactItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LocationItem(
    icon: ImageVector,
    label: String,
    location: String,
    color: Color,
    onNavigate: () -> Unit
) {
    val isLocationValid =
        location.isNotEmpty() && !location.contains("not selected", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Icon and Text
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(22.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Navigate Button (Google Maps)
        if (isLocationValid) {
            IconButton(
                onClick = onNavigate,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Directions,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}