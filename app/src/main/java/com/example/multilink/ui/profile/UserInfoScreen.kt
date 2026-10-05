package com.example.multilink.ui.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material.icons.outlined.Email
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
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.ui.tracker.openWhatsApp
import com.example.multilink.ui.session.SessionViewModel
import com.example.multilink.ui.session.SessionViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    sessionId: String,
    userId: String,
    onBackClick: () -> Unit
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { RealtimeRepository() }

    val participantData = uiState.participants.find { it.id == userId }
    var globalProfile by remember { mutableStateOf<Map<String, String>?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    // ⭐ NEW: Check if this user is the host
    val isAdmin = uiState.sessionData?.hostId == userId

    LaunchedEffect(userId) {
        scope.launch {
            globalProfile = repo.getGlobalUserProfile(userId)
            isLoadingProfile = false
        }
    }

    // ⭐ FIXED: Removed the bottomBar entirely so buttons flow with the content
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (participantData == null || isLoadingProfile) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val photoUrl = globalProfile?.get("photoUrl")
        val phone = globalProfile?.get("phone") ?: ""
        val email = globalProfile?.get("email") ?: "No email provided"
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState) // ⭐ Enables scrolling for smaller screens
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start // ⭐ FIXED: Align content to start instead of center
        ) {
            // ⭐ NEW: Row Layout for Profile Picture (Left) and Name/Role (Right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Picture
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(88.dp) // ⭐ Resized slightly for the row layout
                    ) {
                        if (!photoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Person, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Status Dot
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(
                                if (participantData.status == "Online") Color(
                                    0xFF048848
                                ) else Color.Gray
                            )
                            .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Name and Role
                Column {
                    Text(
                        text = participantData.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // ⭐ NEW: Role Badge (Admin vs Participant)
                    Surface(
                        color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isAdmin) "Admin (Host)" else "Participant",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // User Data Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Phone Number", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                phone.ifEmpty { "Not provided" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Email, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Email Address", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                email, style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Login, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Joined Session", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val joinedStr = SimpleDateFormat(
                                "MMM dd, yyyy • h:mm a", Locale.getDefault()
                            ).format(Date(participantData.joinedAt))
                            Text(
                                joinedStr, style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ⭐ FIXED: Action Buttons placed directly below the info card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subtle Call Button
                Button(
                    onClick = {
                        if (phone.isNotEmpty()) context.startActivity(
                            Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                        )
                        else Toast.makeText(
                            context, "No phone number available", Toast.LENGTH_SHORT
                        )
                            .show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Call, contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Call", fontWeight = FontWeight.Bold)
                }

                // Subtle WhatsApp Button
                Button(
                    onClick = {
                        openWhatsApp(context, phone, "WhatsApp not installed")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        Icons.Default.Whatsapp, contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("WhatsApp", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // Extra padding at the bottom for scrolling
        }
    }
}