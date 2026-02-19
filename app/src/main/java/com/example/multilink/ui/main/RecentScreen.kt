package com.example.multilink.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.multilink.R
import com.example.multilink.model.RecentSession
import com.example.multilink.ui.theme.MultiLinkTheme


@Composable
fun RecentScreen() {
    val recentHistory = remember {
        listOf(
            RecentSession("1", "Jaipur Road Trip", "Yesterday • 9:00 PM", "4h 30m", "Rahul, Sneha + 3", "Home", "Jaipur City", "260 km"),
            RecentSession("2", "Office Commute", "Yesterday • 6:15 PM", "45m", "Shared with Mom", "Office", "Home", "12 km"),
            RecentSession("3", "Weekend Trek", "20 Oct • 11:00 AM", "2 days", "Hiking Group", "Base Camp", "Summit Point", "15 km"),
            RecentSession("4", "Airport Drop", "18 Oct • 5:40 AM", "1h 10m", "Dad", "Home", "Airport T2", "35 km"),
            RecentSession("5", "Grocery Run", "15 Oct • 7:30 PM", "30m", "Priya", "Home", "D-Mart", "5 km")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = dimensionResource(id = R.dimen.padding_list_bottom)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        // --- HEADER ---
        item {
            Column(modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.padding_extra_large),
                end = dimensionResource(id = R.dimen.padding_extra_large),
                top = dimensionResource(id = R.dimen.padding_standard)
            )) {
                Text(
                    text = stringResource(id = R.string.recent_header),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.recent_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- LIST ITEMS ---
        items(recentHistory) { session ->
            RecentSessionCard(
                session = session,
                onClick = { /* Navigate to detail later */ }
            )
        }
    }
}

@Composable
fun RecentSessionCard(
    session: RecentSession,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_large)),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_card)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_standard))
        ) {
            // 1. TOP ROW: Title & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    // Icon Box
                    Surface(
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_standard)), // 12dp
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_box_size)) // 48dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_inside_box)) // 24dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_medium)))

                    Column {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_tiny)), // 12dp
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_tiny)))
                            Text(
                                text = session.completedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_standard)))

            // 2. ROUTE & STATS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        RoundedCornerShape(dimensionResource(id = R.dimen.corner_small)) // 8dp
                    )
                    .padding(dimensionResource(id = R.dimen.padding_medium)), // 12dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route Column
                Column(modifier = Modifier.weight(1f)) {
                    // Route Visual
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.startLoc,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_small)))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_tiny)) // 12dp
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_small)))
                        Text(
                            text = session.endLoc,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))

                    // Stats Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatBadge(icon = Icons.Default.Timer, text = session.duration)
                        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_medium)))
                        StatBadge(icon = Icons.Default.LocationOn, text = session.totalDistance)
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

            // 3. FOOTER: Participants
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.padding_standard)) // 16dp roughly
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_small)))
                Text(
                    text = session.participants,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatBadge(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_stat)) // 14dp
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_tiny)))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecentScreen() {
    MultiLinkTheme {
        RecentScreen()
    }
}