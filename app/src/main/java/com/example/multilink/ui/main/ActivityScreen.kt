package com.example.multilink.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.model.ActivityFeedItem
import com.example.multilink.ui.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = viewModel(),
    onNavigateToLiveTracking: (String) -> Unit = {}
) {
    val stats by viewModel.userStats.collectAsState()
    val feed by viewModel.activityFeed.collectAsState()
    val context = LocalContext.current

    val distanceKm = String.format(Locale.getDefault(), "%.1f", stats.totalDistanceMeters / 1000.0)
    val hours = stats.totalTimeSeconds / 3600
    val minutes = (stats.totalTimeSeconds % 3600) / 60
    val timeStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Your Activity",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Guaranteed Safe Core Icons
                    StatCard(
                        Modifier.weight(1f), Icons.Default.LocationOn, "Distance", "$distanceKm km"
                    )
                    StatCard(Modifier.weight(1f), Icons.Default.Timer, "Time", timeStr)
                    StatCard(
                        Modifier.weight(1f), Icons.AutoMirrored.Filled.List, "Sessions",
                        "${stats.totalSessions}"
                    )
                }
            }
        }

        item {
            Text(
                text = "Recent Notifications",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        if (feed.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent activity.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(feed, key = { it.id }) { item ->
                var isDeleting by remember { mutableStateOf(false) }

                AnimatedVisibility(
                    visible = !isDeleting,
                    exit = shrinkVertically() + fadeOut(tween(300))
                ) {
                    FeedItemCard(
                        item = item,
                        onAccept = {
                            viewModel.acceptInvite(item.sessionId, item.id) { success ->
                                if (success) {
                                    Toast.makeText(context, "Joined!", Toast.LENGTH_SHORT)
                                        .show()
                                    onNavigateToLiveTracking(item.sessionId)
                                } else {
                                    Toast.makeText(context, "Failed to join", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        },
                        onDismiss = {
                            isDeleting = true
                            viewModel.deleteItem(item.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeedItemCard(
    item: ActivityFeedItem,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }

    val iconInfo = when (item.type) {
        "invite" -> Pair(Icons.Default.GroupAdd, MaterialTheme.colorScheme.primaryContainer)
        "alert" -> Pair(Icons.Default.Warning, MaterialTheme.colorScheme.errorContainer)
        else -> Pair(Icons.Default.Info, MaterialTheme.colorScheme.secondaryContainer)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconInfo.second),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconInfo.first, null, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dateStr, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.type == "invite") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAccept, shape = RoundedCornerShape(8.dp)) {
                            Text("Accept")
                        }
                        OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                            Text("Decline")
                        }
                    }
                }
            }
            if (item.type != "invite") {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}