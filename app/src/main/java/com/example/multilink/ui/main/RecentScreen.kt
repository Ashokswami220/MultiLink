package com.example.multilink.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.R
import com.example.multilink.model.RecentSession
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecentSortType { NEWEST, OLDEST, A_Z, Z_A }

fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Unknown"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes mins ago"
        hours < 24 -> "$hours hours ago"
        days == 1L -> "1 day ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    onSessionClick: (String) -> Unit
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(""))
    val recentHistory by viewModel.recentSessions.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentSortType by remember { mutableStateOf(RecentSortType.NEWEST) }
    val (showSortMenu, setShowSortMenu) = remember { mutableStateOf(false) }
    val (deletingSessionId, setDeletingSessionId) = remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }

    val sortedHistory = remember(recentHistory, currentSortType) {
        when (currentSortType) {
            RecentSortType.NEWEST -> recentHistory.sortedByDescending { it.completedTimestamp }
            RecentSortType.OLDEST -> recentHistory.sortedBy { it.completedTimestamp }
            RecentSortType.A_Z -> recentHistory.sortedBy { it.title.lowercase() }
            RecentSortType.Z_A -> recentHistory.sortedByDescending { it.title.lowercase() }
        }
    }


    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (recentHistory.isNotEmpty()) {
                Box {
                    FloatingActionButton(
                        onClick = { setShowSortMenu(true) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { setShowSortMenu(false) },
                        shape = RoundedCornerShape(
                            dimensionResource(id = R.dimen.corner_menu_sheet)
                        ),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        offset = DpOffset(x = 0.dp, y = (-10).dp)
                    ) {
                        SortMenuItem(
                            "Newest First", currentSortType == RecentSortType.NEWEST
                        ) { currentSortType = RecentSortType.NEWEST; setShowSortMenu(false) }
                        SortMenuItem(
                            "Oldest First", currentSortType == RecentSortType.OLDEST
                        ) { currentSortType = RecentSortType.OLDEST; setShowSortMenu(false) }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SortMenuItem(
                            "Name (A - Z)", currentSortType == RecentSortType.A_Z
                        ) { currentSortType = RecentSortType.A_Z; setShowSortMenu(false) }
                        SortMenuItem(
                            "Name (Z - A)", currentSortType == RecentSortType.Z_A
                        ) { currentSortType = RecentSortType.Z_A; setShowSortMenu(false) }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)) {
                        Text(
                            text = stringResource(id = R.string.recent_header),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(id = R.string.recent_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (sortedHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .fillParentMaxHeight(0.9f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No completed sessions yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(sortedHistory, key = { _, it -> it.id }) { index, session ->
                        AnimatedVisibility(
                            visible = session.id != deletingSessionId,
                            exit = slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(300)
                            ) + shrinkVertically(
                                animationSpec = tween(300, delayMillis = 150)
                            ) + fadeOut(animationSpec = tween(300))
                        ) {
                            RecentSessionCard(
                                session = session,
                                isLast = index == sortedHistory.size - 1,
                                onClick = { onSessionClick(session.id) },
                                onDelete = {
                                    setDeletingSessionId(session.id)
                                    scope.launch {
                                        delay(400)
                                        viewModel.deleteRecentSession(session.id)
                                        setDeletingSessionId(null)
                                        Toast.makeText(
                                            context, "Deleted", Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }
                            )
                        }
                    }
                    if (sortedHistory.size == 1) {
                        item { Spacer(modifier = Modifier.fillParentMaxHeight(0.55f)) }
                    } else if (sortedHistory.size == 2) {
                        item { Spacer(modifier = Modifier.fillParentMaxHeight(0.2f)) }
                    }
                }
            }

            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn(tween(200)) + slideInVertically(
                    initialOffsetY = { -it }, animationSpec = tween(200)
                ),
                exit = fadeOut(tween(200)) + slideOutVertically(
                    targetOffsetY = { -it }, animationSpec = tween(200)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.shadow(6.dp, CircleShape),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    }
}


@Composable
fun SortMenuItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        trailingIcon = {
            if (isSelected) Icon(
                Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
    )
}


@Composable
fun RecentSessionCard(
    session: RecentSession,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeAgo = remember(session.completedTimestamp) {
        formatRelativeTime(session.completedTimestamp)
    }

    val reasonLower = session.completionReason.lowercase()
    val statusIcon = remember(reasonLower) {
        when {
            reasonLower.contains("removed") || reasonLower.contains(
                "kicked"
            ) -> Icons.Default.PersonRemove

            reasonLower.contains("left") -> Icons.AutoMirrored.Filled.ExitToApp
            reasonLower.contains("you ended") -> Icons.Outlined.DeleteSweep
            reasonLower.contains("admin") || reasonLower.contains(
                "host"
            ) -> Icons.Default.CheckCircleOutline

            reasonLower.contains("expire") || reasonLower.contains("time") -> Icons.Default.TimerOff
            else -> Icons.Default.CheckCircleOutline
        }
    }

    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val iconBgColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp, end = 12.dp, top = 16.dp,
                    bottom = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon (Squircle)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBgColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hosted by ${session.hostName.ifEmpty { "Unknown" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.title.ifEmpty { "Unnamed Session" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Location Map Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val startTxt = session.startLoc.ifEmpty { "Unknown" }
                        .split(",")
                        .first()
                    val endTxt = session.endLoc.ifEmpty { "Unknown" }
                        .split(",")
                        .first()

                    Text(
                        text = startTxt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = endTxt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Info (Distance & Duration) + Delete Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = session.totalDistance,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Trash Icon on the far right
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 88.dp, end = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 2.dp
            )
        }
    }
}
