package com.example.multilink.ui.otherScreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.ui.viewmodel.ActivityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notificationId: String,
    viewModel: ActivityViewModel,
    onBackClick: () -> Unit,
    onNavigateToLiveTracking: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val repository = remember { RealtimeRepository() }

    val feed by viewModel.activityFeed.collectAsState()
    val item = feed.find { it.id == notificationId }

    var personName by remember { mutableStateOf("Loading...") }
    var personPhone by remember { mutableStateOf("Loading...") }
    var personEmail by remember { mutableStateOf("Loading...") }
    var personPhotoUrl by remember { mutableStateOf<String?>(null) }
    var sessionTitle by remember { mutableStateOf("Loading...") }

    val titleStr = item?.title?.lowercase(Locale.getDefault()) ?: ""
    val isHostAction =
        item?.type == "invite" || titleStr.contains("removed") || titleStr.contains("kicked")
    val sectionTitle = if (isHostAction) "HOST DETAILS" else "PARTICIPANT DETAILS"
    val roleSubtitle = if (isHostAction) "Host" else "Participant"

    LaunchedEffect(item) {
        if (item != null && item.sessionId.isNotEmpty()) {
            repository.getSessionDetails(item.sessionId)
                .collectLatest { data ->
                    if (data.isNotEmpty()) {
                        sessionTitle = data["title"] as? String ?: "Unknown Session"
                        val hostId = data["hostId"] as? String ?: ""

                        // 1. Determine who we should look up
                        val targetUserId = if (isHostAction) {
                            hostId
                        } else {
                            item.actorId // This is the person who joined/left
                        }

                        // 2. Fallback check for legacy data
                        val finalLookupId = if (targetUserId.isEmpty() && !isHostAction) {
                            hostId
                        } else {
                            targetUserId
                        }

                        // 3. Fetch the profile
                        if (finalLookupId.isNotEmpty()) {
                            val profile = repository.getGlobalUserProfile(finalLookupId)
                            if (profile != null) {
                                personName =
                                    profile["name"]?.takeIf { it.isNotEmpty() } ?: "Unknown"
                                personPhone =
                                    profile["phone"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
                                personEmail =
                                    profile["email"]?.takeIf { it.isNotEmpty() } ?: "Not Shared"
                                personPhotoUrl = profile["photoUrl"]?.takeIf { it.isNotEmpty() }
                            } else {
                                personName = "User Not Found"
                                personPhone = "Not Available"
                                personEmail = "Not Available"
                            }
                        } else {
                            personName = "Unknown (Missing ID)"
                            personPhone = "Not Available"
                            personEmail = "Not Available"
                        }
                    }
                }
        }
    }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Notification not found or deleted.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val exactTime = remember(item.timestamp) {
        SimpleDateFormat("EEEE, dd MMM yyyy 'at' hh:mm a", Locale.getDefault()).format(
            Date(item.timestamp)
        )
    }

    val (icon, tintColor, bgColor) = when {
        item.type == "invite" -> Triple(
            Icons.Default.PersonAdd, MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )

        item.type == "alert" || item.type == "removed" || titleStr.contains(
            "left"
        ) || titleStr.contains("removed") || titleStr.contains("kicked") ->
            Triple(
                Icons.AutoMirrored.Filled.ExitToApp, Color(0xFFEF4444),
                Color(0xFFEF4444).copy(alpha = 0.15f)
            )

        item.type == "joined" || titleStr.contains("joined") || titleStr.contains("participant") ->
            Triple(Icons.Default.GroupAdd, Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.15f))

        else -> Triple(
            Icons.AutoMirrored.Filled.DirectionsRun, Color(0xFF3B82F6),
            Color(0xFF3B82F6).copy(alpha = 0.15f)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBackIos, "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.deleteItem(item.id)
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT)
                                .show()
                            onBackClick()
                        }
                    }) {
                        Icon(
                            Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error
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
        // ⭐ ADDED: The 1-Pixel Magic Trick wrapper!
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
                    // --- TOP HEADER ---
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = RoundedCornerShape(14.dp), color = bgColor,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon, null, tint = tintColor, modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title.ifEmpty { "Update" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                item.message,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                exactTime, style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ), color = tintColor
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // --- PERSON DETAILS ---
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                        Text(
                            sectionTitle, style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                            ), color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (personPhotoUrl != null) AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(personPhotoUrl)
                                            .crossfade(true)
                                            .build(), contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    else Icon(
                                        Icons.Default.Person, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    personName, style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    roleSubtitle, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                    personPhone, style = MaterialTheme.typography.bodyMedium
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
                                    personEmail, style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // --- SESSION INFO ---
                    if (item.sessionId.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                            Text(
                                "RELATED SESSION INFO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                                ), color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Map, null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    sessionTitle, style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    // --- ACTION BUTTONS ---
                    if (item.type == "invite") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.deleteItem(
                                            item.id
                                        ); onBackClick()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp), shape = RoundedCornerShape(12.dp)
                            ) { Text("Decline") }
                            Button(
                                onClick = {
                                    viewModel.acceptInvite(item.sessionId, item.id) { success ->
                                        if (success) {
                                            onBackClick()
                                            onNavigateToLiveTracking(item.sessionId)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp), shape = RoundedCornerShape(12.dp)
                            ) { Text("Accept Invite") }
                        }
                    }
                }
            }
        }
    }
}