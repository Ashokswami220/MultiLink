package com.example.multilink.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.model.ActivityFeedItem
import com.example.multilink.ui.viewmodel.ActivityViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.airbnb.lottie.compose.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.example.multilink.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.multilink.utils.HapticHelper
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = viewModel(),
    onNavigateToLiveTracking: (String) -> Unit = {},
    onNavigateToNotificationDetail: (String) -> Unit = {}
) {
    val dashboardScrollState = rememberScrollState()
    val inboxScrollState = rememberScrollState()

    val startPage = remember { kotlin.random.Random.nextInt(0, 2) }
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(top = 8.dp)
        ) {
            MasterViewSwitcher(selectedTab = pagerState.targetPage) { selectedPage ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(selectedPage)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> DashboardSection(viewModel, dashboardScrollState)
                    1 -> InboxSection(
                        viewModel = viewModel,
                        scrollState = inboxScrollState,
                        onNavigateToLiveTracking = onNavigateToLiveTracking,
                        onNavigateToNotificationDetail = onNavigateToNotificationDetail
                    )
                }
            }
        }
    }
}

@Composable
fun MasterViewSwitcher(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Dashboard", "Inbox")
    val context = LocalContext.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(6.dp)
    ) {
        val tabWidth = this.maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f), label = "indicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, text ->
                val isSelected = selectedTab == index
                val textColor by animateColorAsState(
                    if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "text"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selectedTab != index) {
                                HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                            }
                            onTabSelected(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text, style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ), color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSection(
    viewModel: ActivityViewModel, scrollState: ScrollState
) {
    val stats by viewModel.userStats.collectAsState()
    val labels by viewModel.graphLabels.collectAsState()

    val sessionsData by viewModel.sessionsGraph.collectAsState()
    val distData by viewModel.distanceGraph.collectAsState()
    val timeData by viewModel.timeGraph.collectAsState()

    var graphTab by remember { mutableIntStateOf(0) }

    val activeData = when (graphTab) {
        0 -> sessionsData
        1 -> distData
        else -> timeData
    }

    val themeColor by animateColorAsState(
        targetValue = when (graphTab) {
            0 -> MaterialTheme.colorScheme.primary
            1 -> Color(0xFF10B981)
            else -> Color(0xFFF59E0B)
        },
        animationSpec = tween(500), label = "themeColor"
    )

    val formatValue: (Float) -> String = { value ->
        when (graphTab) {
            0 -> "${value.toInt()} sessions"
            1 -> String.format(Locale.getDefault(), "%.1f km", value)
            else -> "${value.toInt()} mins"
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minScrollHeight = this.maxHeight + 1.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Apply the +1dp height and padding directly to the inner container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minScrollHeight)
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
            ) {
                InteractiveSparklineGraph(
                    data = activeData, labels = labels, themeColor = themeColor,
                    valueFormatter = formatValue
                )
                Spacer(modifier = Modifier.height(16.dp))

                AppleStyleTabSwitcher(graphTab, themeColor) { graphTab = it }
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "All-Time Milestones",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val distanceFloat = (stats.totalDistanceMeters / 1000.0).toFloat()
                val minutesFloat = (stats.totalTimeSeconds / 60).toFloat()
                val sessionsFloat = stats.totalSessions.toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifetimeStatCard(
                        Modifier.weight(1f), Icons.Default.Explore, "Distance", distanceFloat,
                        { String.format(Locale.getDefault(), "%.1f", it) }, "km", Color(0xFF10B981),
                        StatCardType.DISTANCE
                    )
                    LifetimeStatCard(
                        Modifier.weight(1f), Icons.Default.HourglassBottom, "Time", minutesFloat, {
                            val h = (it / 60).toInt()
                            val m = (it % 60).toInt()
                            if (h > 0) "${h}h ${m}m" else "${m}m"
                        }, "", Color(0xFFF59E0B), StatCardType.TIME
                    )
                    LifetimeStatCard(
                        Modifier.weight(1f), Icons.Default.EmojiEvents, "Sessions", sessionsFloat, {
                            it.toInt()
                                .toString()
                        }, "total", MaterialTheme.colorScheme.primary, StatCardType.SESSIONS
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                LottieFidgetWidget()
            }
        }
    }
}


@Composable
fun InboxSection(
    viewModel: ActivityViewModel,
    scrollState: ScrollState,
    onNavigateToLiveTracking: (String) -> Unit,
    onNavigateToNotificationDetail: (String) -> Unit
) {
    val feed by viewModel.activityFeed.collectAsState()
    val context = LocalContext.current

    val groupedFeed = remember(feed) {
        val groups = mutableListOf<List<ActivityFeedItem>>()
        val handledIds = mutableSetOf<String>()
        feed.forEach { item ->
            if (item.sessionId.isEmpty()) {
                groups.add(listOf(item))
            } else if (!handledIds.contains(item.sessionId)) {
                val sessionItems = feed.filter { it.sessionId == item.sessionId }
                groups.add(sessionItems)
                handledIds.add(item.sessionId)
            }
        }
        groups
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                    .padding(top = 8.dp, bottom = 80.dp)
            ) {
                if (feed.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, start = 24.dp, end = 24.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DoneAll, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "All caught up!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "No new notifications.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    groupedFeed.forEach { sessionItems ->
                        key("group_${sessionItems.first().id}") {
                            var isGroupDeleting by remember { mutableStateOf(false) }
                            val scope = rememberCoroutineScope()

                            AnimatedVisibility(
                                visible = !isGroupDeleting,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = spring(dampingRatio = 0.7f)
                                ) + fadeIn(),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { -it }, animationSpec = tween(300)
                                ) + shrinkVertically(
                                    animationSpec = tween(300, delayMillis = 150)
                                ) + fadeOut(tween(300))
                            ) {
                                Column {
                                    val firstItem = sessionItems.first()
                                    val start = firstItem.message.indexOf("'")
                                    val end = firstItem.message.lastIndexOf("'")
                                    val sessionName = if (start != -1 && end != -1 && start < end) {
                                        firstItem.message.substring(start + 1, end)
                                    } else {
                                        "Session Updates"
                                    }

                                    // HEADER
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                            .padding(top = 20.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sessionName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            if (sessionItems.size > 1) {
                                                Text(
                                                    text = "${sessionItems.size} notifications",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // CLOSE ALL BUTTON
                                        Surface(
                                            onClick = {
                                                scope.launch {
                                                    isGroupDeleting = true
                                                    delay(300)
                                                    sessionItems.forEach {
                                                        viewModel.deleteItem(
                                                            it.id
                                                        )
                                                    }
                                                }
                                            },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear All",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }

                                    // CARDS (Flat List)
                                    sessionItems.forEachIndexed { index, item ->
                                        var isItemDeleting by remember { mutableStateOf(false) }

                                        AnimatedVisibility(
                                            visible = !isItemDeleting,
                                            enter = fadeIn(),
                                            exit = slideOutHorizontally(
                                                targetOffsetX = { -it }, animationSpec = tween(300)
                                            ) + shrinkVertically(
                                                animationSpec = tween(300, delayMillis = 150)
                                            ) + fadeOut(tween(300))
                                        ) {
                                            FeedItemCard(
                                                item = item,
                                                isLast = index == sessionItems.size - 1,
                                                onClick = {
                                                    onNavigateToNotificationDetail(item.id)
                                                },
                                                onAccept = {
                                                    viewModel.acceptInvite(
                                                        item.sessionId, item.id
                                                    ) { success ->
                                                        if (success) {
                                                            Toast.makeText(
                                                                context, "Joined successfully!",
                                                                Toast.LENGTH_SHORT
                                                            )
                                                                .show()
                                                            onNavigateToLiveTracking(item.sessionId)
                                                        } else {
                                                            Toast.makeText(
                                                                context, "Failed to join session",
                                                                Toast.LENGTH_SHORT
                                                            )
                                                                .show()
                                                        }
                                                    }
                                                },
                                                onDismiss = {
                                                    scope.launch {
                                                        if (sessionItems.size == 1) {
                                                            isGroupDeleting = true
                                                        } else {
                                                            isItemDeleting = true
                                                        }
                                                        delay(300)
                                                        viewModel.deleteItem(item.id)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FeedItemCard(
    item: ActivityFeedItem,
    isLast: Boolean,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val timeAgo = remember(item.timestamp) {
        val diff = System.currentTimeMillis() - item.timestamp
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "1 day ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(java.util.Date(item.timestamp))
            }
        }
    }

    val titleStr = item.title.lowercase(Locale.getDefault())
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon (Squircle)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon, contentDescription = null, tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Type of Notification
                Text(
                    text = item.title.ifEmpty { "Notification" },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Actual message
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                // Accept/Decline Buttons for Invites
                if (item.type == "invite") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onAccept, shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text("Accept", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onDismiss, shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text("Decline", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // X close button for individual item
            if (item.type != "invite") {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(y = (-4).dp)
                ) {
                    Icon(
                        Icons.Default.Close, contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sleek separator line
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 88.dp, end = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )
        }
    }
}

enum class StatCardType { DISTANCE, TIME, SESSIONS }

@Composable
fun LifetimeStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector, label: String,
    targetValue: Float,
    valueFormatter: (Float) -> String,
    unit: String,
    glowColor: Color,
    type: StatCardType
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnimating by remember { mutableStateOf(false) }

    val animatedNumber = remember { Animatable(targetValue) }
    val iconRotationZ = remember { Animatable(0f) }
    val iconRotationY = remember { Animatable(0f) }

    LaunchedEffect(targetValue) { if (!isAnimating) animatedNumber.snapTo(targetValue) }

    Surface(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) {
            if (isAnimating) return@clickable
            isAnimating = true

            HapticHelper.trigger(context, HapticHelper.Type.LIGHT)

            coroutineScope.launch {
                val targetRot = 360f * 3f
                if (type == StatCardType.SESSIONS) {
                    iconRotationY.animateTo(targetRot, tween(1500, easing = FastOutSlowInEasing))
                    iconRotationY.snapTo(0f)
                } else {
                    iconRotationZ.animateTo(targetRot, tween(1500, easing = FastOutSlowInEasing))
                    iconRotationZ.snapTo(0f)
                }
            }

            coroutineScope.launch {
                animatedNumber.snapTo(0f)
                animatedNumber.animateTo(targetValue, tween(1500, easing = FastOutSlowInEasing))

                HapticHelper.trigger(context, HapticHelper.Type.HEAVY)

                isAnimating = false
            }
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(glowColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, contentDescription = null, tint = glowColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = iconRotationZ.value; rotationY = iconRotationY.value
                            })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = valueFormatter(animatedNumber.value),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Visible
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(2.dp)); Text(
                            text = unit, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                bottom = 3.dp
                            )
                        )
                    }
                }
                Text(
                    text = label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun AppleStyleTabSwitcher(selectedTab: Int, activeColor: Color, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Sessions", "Distance", "Time")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        val tabWidth = this.maxWidth / tabs.size
        val tabWidthPx = this.constraints.maxWidth / tabs.size.toFloat()

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab, animationSpec = spring(
                dampingRatio = 0.7f, stiffness = 300f
            ), label = "indicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(activeColor)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onTabSelected(
                            (offset.x / tabWidthPx).toInt()
                                .coerceIn(0, tabs.size - 1)
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onTabSelected(
                            (change.position.x / tabWidthPx).toInt()
                                .coerceIn(0, tabs.size - 1)
                        )
                    }
                }
        ) {
            tabs.forEachIndexed { index, text ->
                val isSelected = selectedTab == index
                val textColor by animateColorAsState(
                    if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "text"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text, style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ), color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveSparklineGraph(
    data: List<Float>, labels: List<String>, themeColor: Color, valueFormatter: (Float) -> String
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val animationProgress = remember { Animatable(0f) }

    var touchX by remember { mutableFloatStateOf(-1f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    var targetBirdX by remember { mutableFloatStateOf(0f) }
    var isFacingRight by remember { mutableStateOf(true) }

    val animBirdX by animateFloatAsState(
        targetValue = targetBirdX,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "bird_flight_x"
    )

    val isMoving = (animBirdX - targetBirdX).absoluteValue > 2f

    val infiniteTransition = rememberInfiniteTransition(label = "flap")
    val flapHeight by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(90), RepeatMode.Reverse),
        label = "flap_height"
    )

    val textPaint = remember(labelColor) {
        android.graphics.Paint()
            .apply {
                color = labelColor; textSize = 32f; textAlign =
                android.graphics.Paint.Align.RIGHT; isAntiAlias = true
            }
    }

    val rawMax = data.maxOrNull()
        ?.takeIf { it > 0f } ?: 1f
    val stepSize = when {
        rawMax <= 5f -> 1f; rawMax <= 10f -> 2f; rawMax <= 25f -> 5f
        rawMax <= 50f -> 10f; rawMax <= 100f -> 20f; rawMax <= 500f -> 100f
        else -> 500f
    }
    val graphMax = kotlin.math.ceil(rawMax / stepSize) * stepSize
    val yLabels = buildList {
        var current = 0f; while (current <= graphMax) {
        add(current); current += stepSize
    }
    }

    LaunchedEffect(data) {
        touchX = -1f; selectedIndex = -1; isFacingRight = true
        animationProgress.snapTo(0f); animationProgress.animateTo(
        1f, tween(1000, easing = FastOutSlowInEasing)
    )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp), // ⭐ FIXED: Removed unused onGloballyPositioned modifier
        shape = RoundedCornerShape(24.dp), color = containerColor,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top
            ) {
                Column {
                    val displayValue = if (selectedIndex in data.indices) valueFormatter(
                        data[selectedIndex]
                    ) else valueFormatter(data.sum())
                    val displayLabel = if (selectedIndex in labels.indices) {
                        if (labels[selectedIndex] == "Now") "Today" else "On this day"
                    } else "Last 7 Days"
                    Text(
                        displayLabel, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        displayValue, style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ), color = themeColor
                    )
                }

                Surface(color = themeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(50)) {
                    Text(
                        "Hold & Scrub", style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ), color = themeColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // ⭐ FIXED: Removed unused variable assignments in pointer input
                    .pointerInput(data) {
                        detectTapGestures(
                            onPress = {
                                touchX = it.x
                                if (tryAwaitRelease()) {
                                    touchX = -1f; selectedIndex = -1; isFacingRight = true
                                }
                            }
                        )
                    }
                    .pointerInput(data) {
                        detectHorizontalDragGestures(
                            onDragStart = { touchX = it.x },
                            onDragEnd = {
                                touchX = -1f; selectedIndex = -1; isFacingRight = true
                            },
                            onDragCancel = {
                                touchX = -1f; selectedIndex = -1; isFacingRight = true
                            },
                            onHorizontalDrag = { change, _ ->
                                val currX = change.position.x
                                if (currX > touchX) isFacingRight = true
                                else if (currX < touchX) isFacingRight = false
                                touchX = currX
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val graphPaddingTop = 44.dp.toPx()
                    val graphPaddingBottom = 12.dp.toPx()
                    val graphPaddingLeft = 44.dp.toPx()
                    val graphPaddingRight = 12.dp.toPx()

                    val availableHeight = size.height - graphPaddingTop - graphPaddingBottom
                    val availableWidth = size.width - graphPaddingLeft - graphPaddingRight
                    val xStep = availableWidth / (data.size - 1).coerceAtLeast(1)

                    if (touchX >= 0f) {
                        val newIndex = ((touchX - graphPaddingLeft) / xStep).roundToInt()
                            .coerceIn(0, data.size - 1)
                        if (newIndex != selectedIndex) {
                            selectedIndex = newIndex
                            HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                        }
                    }

                    val coordinates = data.mapIndexed { index, value ->
                        Offset(
                            graphPaddingLeft + (index * xStep),
                            graphPaddingTop + availableHeight - ((value / graphMax) * availableHeight)
                        )
                    }

                    if (coordinates.isNotEmpty()) {
                        val newTargetX =
                            if (selectedIndex in coordinates.indices) coordinates[selectedIndex].x else coordinates.last().x
                        if (targetBirdX != newTargetX) coroutineScope.launch {
                            targetBirdX = newTargetX
                        }
                    }

                    val strokePath = Path().apply {
                        if (coordinates.isNotEmpty()) {
                            moveTo(
                                coordinates.first().x, coordinates.first().y
                            ); for (i in 1 until coordinates.size) {
                                val prev = coordinates[i - 1]
                                val curr = coordinates[i]
                                val controlX = (prev.x + curr.x) / 2f; cubicTo(
                                    controlX, prev.y, controlX, curr.y, curr.x, curr.y
                                )
                            }
                        }
                    }
                    val fillPath = Path().apply {
                        addPath(strokePath); if (coordinates.isNotEmpty()) {
                        lineTo(coordinates.last().x, size.height); lineTo(
                            coordinates.first().x, size.height
                        ); close()
                    }
                    }

                    clipRect(right = size.width * animationProgress.value) {

                        yLabels.forEach { value ->
                            val y =
                                graphPaddingTop + availableHeight - ((value / graphMax) * availableHeight)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.2f),
                                start = Offset(graphPaddingLeft, y),
                                end = Offset(size.width - graphPaddingRight, y),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 10f), 0f
                                )
                            )

                            val labelStr = when {
                                value >= 1000f -> {
                                    val kVal = value / 1000f
                                    if (kVal % 1 == 0f) "${kVal.toInt()}k" else String.format(
                                        Locale.getDefault(), "%.1fk", kVal
                                    )
                                }

                                value % 1 == 0f -> value.toInt()
                                    .toString()

                                else -> value.toString()
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                labelStr, graphPaddingLeft - 16f, y + 10f, textPaint
                            )
                        }

                        drawPath(
                            path = fillPath, brush = Brush.verticalGradient(
                                colors = listOf(themeColor.copy(alpha = 0.4f), Color.Transparent),
                                startY = graphPaddingTop, endY = size.height
                            )
                        )
                        drawPath(
                            path = strokePath, color = themeColor,
                            style = Stroke(width = 4.dp.toPx())
                        )

                        if (selectedIndex in coordinates.indices) {
                            val selectedPoint = coordinates[selectedIndex]
                            drawLine(
                                color = themeColor.copy(alpha = 0.5f),
                                start = Offset(selectedPoint.x, graphPaddingTop - 8.dp.toPx()),
                                end = Offset(selectedPoint.x, size.height),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 10f), 0f
                                )
                            )
                            drawCircle(
                                color = themeColor, radius = 6.dp.toPx(), center = selectedPoint
                            )
                            drawCircle(
                                color = Color.White, radius = 3.dp.toPx(), center = selectedPoint
                            )
                        } else coordinates.forEach { point ->
                            drawCircle(
                                color = themeColor, radius = 3.dp.toPx(), center = point
                            )
                        }

                        if (animBirdX > 0f) {
                            val birdX = animBirdX
                            val birdY = graphPaddingTop - 12.dp.toPx()
                            val wingFlap = if (isMoving) flapHeight.dp.toPx() else 4.dp.toPx()

                            withTransform({
                                              scale(
                                                  scaleX = if (isFacingRight) 1f else -1f,
                                                  scaleY = 1f, pivot = Offset(birdX, birdY)
                                              )
                                          }) {
                                val bodyPath = Path().apply {
                                    moveTo(
                                        birdX + 10.dp.toPx(), birdY
                                    )
                                    quadraticTo(
                                        birdX + 2.dp.toPx(), birdY - 8.dp.toPx(),
                                        birdX - 8.dp.toPx(),
                                        birdY - 2.dp.toPx()
                                    )
                                    lineTo(birdX - 16.dp.toPx(), birdY + 2.dp.toPx()); lineTo(
                                    birdX - 12.dp.toPx(), birdY + 4.dp.toPx()
                                ); lineTo(
                                    birdX - 16.dp.toPx(), birdY + 8.dp.toPx()
                                )
                                    quadraticTo(
                                        birdX - 4.dp.toPx(), birdY + 10.dp.toPx(),
                                        birdX + 6.dp.toPx(),
                                        birdY + 4.dp.toPx()
                                    )
                                    close()
                                }
                                drawPath(bodyPath, color = themeColor, style = Fill)
                                drawCircle(
                                    color = containerColor, radius = 1.5.dp.toPx(),
                                    center = Offset(birdX + 4.dp.toPx(), birdY - 1.dp.toPx())
                                )
                                val wingPath = Path().apply {
                                    moveTo(
                                        birdX - 2.dp.toPx(), birdY + 2.dp.toPx()
                                    )
                                    quadraticTo(
                                        birdX - 8.dp.toPx(), birdY - 10.dp.toPx() + wingFlap,
                                        birdX - 14.dp.toPx(), birdY - 12.dp.toPx() + wingFlap
                                    )
                                    quadraticTo(
                                        birdX - 8.dp.toPx(), birdY - 2.dp.toPx() + wingFlap * 0.5f,
                                        birdX + 2.dp.toPx(), birdY + 4.dp.toPx()
                                    )
                                }
                                drawPath(
                                    wingPath, color = containerColor, style = Stroke(
                                        width = 2.dp.toPx(), cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                                drawPath(wingPath, color = themeColor, style = Fill)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, end = 12.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEachIndexed { index, label ->
                    val isSelected = index == selectedIndex
                    val labelAlpha by animateFloatAsState(
                        if (selectedIndex == -1 || isSelected) 1f else 0.3f, label = "alpha"
                    )
                    Text(
                        text = label, style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = labelAlpha)
                    )
                }
            }
        }
    }
}

@Composable
fun LottieFidgetWidget(modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.dashboard_anim))

    var isRunning by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val lottieSizePx = with(density) { 80.dp.toPx() }

    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isRunning,
        iterations = LottieConstants.IterateForever
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    trackWidthPx = coordinates.size.width.toFloat()
                }
                .pointerInput(trackWidthPx) {
                    detectTapGestures(
                        onPress = {
                            if (isRunning || trackWidthPx == 0f) return@detectTapGestures

                            coroutineScope.launch {
                                isRunning = true

                                // --- 1. START HAPTIC ---
                                HapticHelper.trigger(context, HapticHelper.Type.MEDIUM)

                                // --- 2. FOOTSTEP HAPTICS LOOP ---
                                val footstepsJob = launch {
                                    delay(150)
                                    while (isActive && isRunning) {
                                        HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                        delay(180L)
                                    }
                                }

                                // --- 3. ANIMATION LOGIC ---
                                // Run off the right edge
                                offsetX.animateTo(
                                    targetValue = trackWidthPx,
                                    animationSpec = tween(
                                        durationMillis = 1500, easing = LinearEasing
                                    )
                                )

                                // Teleport off-screen left
                                offsetX.snapTo(-lottieSizePx)

                                // Run back to the center
                                val returnDuration = ((lottieSizePx / trackWidthPx) * 1500).toInt()
                                    .coerceAtLeast(300)
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = returnDuration, easing = LinearEasing
                                    )
                                )

                                // --- 4. STOP & CLEANUP ---
                                isRunning = false
                                footstepsJob.cancel()

                                // --- 5. STOP HAPTIC ---
                                HapticHelper.trigger(context, HapticHelper.Type.HEAVY)
                            }
                        }
                    )
                }
        ) {
            LottieAnimation(
                composition = composition,
                progress = { lottieProgress },
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(80.dp)
                    .graphicsLayer {
                        val zoomScale = 1.7f
                        scaleX = zoomScale
                        scaleY = zoomScale
                    }
            )
        }
    }
}