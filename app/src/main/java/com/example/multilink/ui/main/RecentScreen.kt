package com.example.multilink.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.R
import com.example.multilink.model.RecentSession
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RecentSortType { NEWEST, OLDEST, A_Z, Z_A }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen() {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(""))
    val recentHistory by viewModel.recentSessions.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentSortType by remember { mutableStateOf(RecentSortType.NEWEST) }

    val (showSortMenu, setShowSortMenu) = remember { mutableStateOf(false) }
    val (showBottomSheet, setShowBottomSheet) = remember { mutableStateOf(false) }
    val (selectedSession, setSelectedSession) = remember { mutableStateOf<RecentSession?>(null) }
    val (deletingSessionId, setDeletingSessionId) = remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sortedHistory = remember(recentHistory, currentSortType) {
        when (currentSortType) {
            RecentSortType.NEWEST -> recentHistory.sortedByDescending { it.completedTimestamp }
            RecentSortType.OLDEST -> recentHistory.sortedBy { it.completedTimestamp }
            RecentSortType.A_Z -> recentHistory.sortedBy { it.title.lowercase() }
            RecentSortType.Z_A -> recentHistory.sortedByDescending { it.title.lowercase() }
        }
    }

    if (showBottomSheet && selectedSession != null) {
        RecentSessionBottomSheet(
            session = selectedSession,
            sheetState = sheetState,
            onDismiss = {
                setShowBottomSheet(false)
                setSelectedSession(null)
            }
        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(id = R.dimen.padding_medium)
            )
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
                items(sortedHistory, key = { it.id }) { session ->
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
                            onClick = {
                                setSelectedSession(session)
                                setShowBottomSheet(true)
                            },
                            onDelete = {
                                setDeletingSessionId(session.id)
                                scope.launch {
                                    delay(400)
                                    viewModel.deleteRecentSession(session.id)
                                    setDeletingSessionId(null)
                                    Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentSessionBottomSheet(
    session: RecentSession,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(
                    start = 16.dp, end = 16.dp, bottom = 0.dp
                ),
            shape = RoundedCornerShape(
                topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp
            ),
            color = if (isDark) {
                lerp(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    Color.Black,
                    0.05f
                )
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = 24.dp, vertical = 12.dp
                    )
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Header Info (Title and Users)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title.ifEmpty { "Unnamed Session" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Group, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = session.participants,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 1. Reason & Date Banner
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = session.completionReason.ifEmpty { "Session Ended" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = session.completedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 2. Horizontal Metric Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer, null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Time Spent", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = 0.8f
                                    )
                                )
                                Text(
                                    session.duration,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ), color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn, null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Distance", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                        alpha = 0.8f
                                    )
                                )
                                Text(
                                    session.totalDistance,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ), color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 3. Host Info Card
                Text(
                    "HOST DETAILS", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
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
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                0.3f
                            )
                        )
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
                }

                Spacer(Modifier.height(20.dp))

                // 4. Clickable Navigation Info
                Text(
                    "LOCATIONS", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

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

                Box(
                    modifier = Modifier
                        .padding(start = 13.dp)
                        .height(16.dp)
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_large)),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_card)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_standard))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_standard)),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_box_size))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.History, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(
                                    dimensionResource(id = R.dimen.icon_inside_box)
                                )
                            )
                        }
                    }
                    Spacer(
                        modifier = Modifier.width(dimensionResource(id = R.dimen.padding_medium))
                    )
                    Text(
                        session.title.ifEmpty { "Unnamed Session" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ), color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_standard)))

            Surface(
                modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_small)),
                border = BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_medium))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val startTxt = session.startLoc.ifEmpty { "Location not selected by host" }
                        val endTxt = session.endLoc.ifEmpty { "Location not selected by host" }

                        Text(
                            startTxt, style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ), color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Navigation, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(90f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            endTxt, style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ), color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.3f
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatBadge(
                            icon = Icons.Default.Timer, text = "Time Spent: ${session.duration}"
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        StatBadge(
                            icon = Icons.Default.LocationOn, text = "Dist: ${session.totalDistance}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_stat))
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_small)))
                Text(
                    session.participants, style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ), color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatBadge(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_stat))
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_tiny)))
        Text(
            text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val isAvailable = locationName != "Location not selected by host"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAvailable) { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(28.dp), tint = iconColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = locationName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isAvailable) {
            Icon(
                Icons.Default.Directions, null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}