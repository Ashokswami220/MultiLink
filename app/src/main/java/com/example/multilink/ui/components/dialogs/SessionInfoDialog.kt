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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

private val hostProfileCache = mutableMapOf<String, Map<String, String>>()

@Composable
fun SessionInfoDialog(
    session: SessionData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RealtimeRepository() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isAdmin = session.hostId == currentUserId
    val shouldShowCode = isAdmin || session.isSharingAllowed

    // --- STATE ---
    var hostEmail by rememberSaveable { mutableStateOf("Loading...") }
    var hostPhone by rememberSaveable { mutableStateOf("Loading...") }
    var hostPhotoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var timeLeftString by remember { mutableStateOf("...") }
    var isExpired by remember { mutableStateOf(false) }

    // Fetch Host Info
    LaunchedEffect(session.hostId) {
        if (hostProfileCache.containsKey(session.hostId)) {
            val details = hostProfileCache[session.hostId]!!
            hostPhone = details["phone"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
            hostEmail = details["email"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
            hostPhotoUrl = details["photoUrl"]?.takeIf { it.isNotEmpty() }
        } else {
            val details = repository.getGlobalUserProfile(session.hostId)
            if (details != null) {
                hostProfileCache[session.hostId] = details // Save to cache for next time
                hostPhone = details["phone"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
                hostEmail = details["email"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
                hostPhotoUrl = details["photoUrl"]?.takeIf { it.isNotEmpty() }
            } else {
                hostPhone = "Unknown"
                hostEmail = "Unknown"
            }
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
        val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(webIntent)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // ⭐ CHANGED: More squarish, darker overall theme wrapper
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
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
                        style = MaterialTheme.typography.headlineSmall.copy( // ⭐ CHANGED: Larger, bolder title
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // ⭐ CHANGED: Sleek Timer Chip
                    Surface(
                        color = if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(
                            alpha = 0.8f
                        ) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp), // Squarish pill
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (isExpired) Icons.Default.Warning else Icons.Outlined.Timer,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeLeftString,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- 2. JOIN CODE (If applicable) ---
                if (shouldShowCode && session.joinCode.isNotEmpty()) {
                    // ⭐ CHANGED: Used SurfaceContainerHigh to create the distinct dark box from the screenshot
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                scope.launch {
                                    val clipData = android.content.ClipData.newPlainText(
                                        "Join Code", session.joinCode
                                    )
                                    clipboard.setClipEntry(clipData.toClipEntry())
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "JOIN CODE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = session.joinCode,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!session.isSharingAllowed && isAdmin) {
                                    Text(
                                        text = "Visible only to Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // --- 3. HOST DETAILS ---
                // ⭐ CHANGED: Clean section header
                Text(
                    text = "HOST DETAILS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow, // Creates subtle contrast
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top Row: Avatar + Name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (hostPhotoUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(hostPhotoUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Host Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = session.hostName.ifEmpty { "Unknown Host" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Divider
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Bottom Row: Contact Info (Cleaner Icons)
                        ContactItem(Icons.Outlined.Phone, hostPhone)
                        Spacer(modifier = Modifier.height(10.dp))
                        ContactItem(Icons.Outlined.Email, hostEmail)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- 4. ROUTE INFO (Visual Timeline) ---
                Text(
                    text = "ROUTE INFO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Start Location
                    LocationItem(
                        icon = Icons.Outlined.RadioButtonUnchecked, // ⭐ CHANGED: Hollow circle for start
                        label = "Start Point",
                        location = session.fromLocation.ifEmpty { "Not selected by host" },
                        iconColor = MaterialTheme.colorScheme.primary,
                        onNavigate = {
                            openMap(
                                session.startLat ?: 0.0, session.startLng ?: 0.0,
                                session.fromLocation
                            )
                        }
                    )

                    // Vertical Connector Line
                    Box(
                        modifier = Modifier
                            .padding(start = 11.dp) // Aligns perfectly with 24.dp icon
                            .height(24.dp)
                            .width(2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    // End Location
                    LocationItem(
                        icon = Icons.Default.Place, // Solid Pin for end
                        label = "Destination",
                        location = session.toLocation.ifEmpty { "Not selected by host" },
                        iconColor = MaterialTheme.colorScheme.error,
                        onNavigate = {
                            openMap(
                                session.endLat ?: 0.0, session.endLng ?: 0.0, session.toLocation
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // --- 5. CLOSE BUTTON ---
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        "Close", style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
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
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LocationItem(
    icon: ImageVector,
    label: String,
    location: String,
    iconColor: Color,
    onNavigate: () -> Unit
) {
    val isLocationValid =
        location.isNotEmpty() && !location.contains("not selected", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isLocationValid
            ) { onNavigate() }, // ⭐ CHANGED: Made the whole row clickable instead of a button
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = location,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}