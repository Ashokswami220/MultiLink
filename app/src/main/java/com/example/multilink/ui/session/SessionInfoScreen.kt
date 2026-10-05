package com.example.multilink.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.ui.session.SessionViewModel
import com.example.multilink.ui.session.SessionViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionInfoScreen(
    sessionId: String,
    onBackClick: () -> Unit
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()
    val sessionData = uiState.sessionData

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Info", fontWeight = FontWeight.Bold) },
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
        if (sessionData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val scrollState = rememberScrollState()
        val isHost = uiState.currentUserId == sessionData.hostId
        val currentUserData = uiState.participants.find { it.id == uiState.currentUserId }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Core Details
            InfoSectionCard("Overview") {
                InfoRow(Icons.Outlined.Title, "Title", sessionData.title)
                InfoRow(
                    Icons.Outlined.Person, "Host",
                    sessionData.hostName + if (isHost) " (You)" else ""
                )
                InfoRow(Icons.Outlined.VpnKey, "Join Code", sessionData.joinCode)

                val createdStr =
                    SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(
                        Date(sessionData.createdTimestamp)
                    )
                InfoRow(Icons.Outlined.CalendarToday, "Created On", createdStr)
            }

            // Card 2: Tracking Mode & Limits
            InfoSectionCard("Tracking Rules") {
                val isParental = sessionData.sessionType == "Parental"
                val modeText = if (isParental) "Parental Control" else "Standard Active"
                val modeColor =
                    if (isParental) Color(0xFFE91E63) else MaterialTheme.colorScheme.primary

                InfoRowColored(Icons.Outlined.Security, "Mode", modeText, modeColor)
                InfoRow(
                    Icons.Outlined.Group, "Capacity",
                    "${uiState.participants.size} / ${sessionData.maxPeople} Users"
                )

                val expiryText = if (sessionData.durationUnit == "Forever") {
                    "No Expiry / No Limit"
                } else {
                    "${sessionData.durationVal} ${sessionData.durationUnit}"
                }
                InfoRow(Icons.Outlined.Timer, "Duration Limit", expiryText)
            }

            // Card 3: Permissions & Privacy
            InfoSectionCard("Permissions") {
                InfoRowBool(
                    Icons.Outlined.Visibility, "Users can see each other",
                    sessionData.isUsersVisible
                )
                InfoRowBool(
                    Icons.Outlined.Share, "Users can share invite link",
                    sessionData.isSharingAllowed
                )

                if (sessionData.sessionType == "Parental") {
                    InfoRowBool(
                        Icons.AutoMirrored.Filled.ExitToApp, "Users allowed to leave",
                        sessionData.isLeaveAllowed
                    )
                } else {
                    InfoRowBool(
                        Icons.Outlined.TaskAlt, "Destination Check-In Enabled",
                        sessionData.isArrivalTrackingEnabled
                    )
                }
            }

            // Card 4: My Status
            InfoSectionCard("Your Status") {
                InfoRow(Icons.Outlined.Badge, "Role", if (isHost) "Admin (Host)" else "Participant")

                if (currentUserData != null) {
                    val joinedStr = SimpleDateFormat("MMM dd • h:mm a", Locale.getDefault()).format(
                        Date(currentUserData.joinedAt)
                    )
                    InfoRow(Icons.AutoMirrored.Outlined.Login, "Joined Session", joinedStr)
                }
            }
        }
    }
}

@Composable
fun InfoSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InfoRowColored(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun InfoRowBool(icon: ImageVector, label: String, isEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
        )

        val statusText = if (isEnabled) "Yes" else "No"
        val statusColor = if (isEnabled) Color(0xFF048848) else MaterialTheme.colorScheme.error

        Text(
            statusText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = statusColor
        )
    }
}