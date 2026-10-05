package com.example.multilink.ui.services

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Devices
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multilink.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServicesScreen() {
    var titleHeightPx by remember { mutableFloatStateOf(0f) }
    var searchBarHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val headerAlpha by remember {
        derivedStateOf {
            if (titleHeightPx == 0f) 1f
            else (1f - (headerOffsetPx.absoluteValue / titleHeightPx).coerceIn(0f, 1f))
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffsetPx + delta
                headerOffsetPx = newOffset.coerceIn(-titleHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    val density = LocalDensity.current
    val totalHeaderHeightDp = remember(titleHeightPx, searchBarHeightPx, density) {
        with(density) { (titleHeightPx + searchBarHeightPx).toDp() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .nestedScroll(nestedScrollConnection)
    ) {
        // --- SCROLLABLE CONTENT ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (totalHeaderHeightDp > 0.dp) totalHeaderHeightDp + 12.dp else 120.dp,
                bottom = 120.dp
            )
        ) {
            item {
                RecentSessionsSection()
                Spacer(modifier = Modifier.height(24.dp))
                TrackingCardsSection()
                Spacer(modifier = Modifier.height(32.dp))

                ElevateYourRideSection()
                Spacer(modifier = Modifier.height(32.dp))

                PromoCardsSection()
            }
        }

        // --- COLLAPSING HEADER OVERLAY ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = headerOffsetPx.roundToInt()) }
                .background(MaterialTheme.colorScheme.background)
                .zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = headerAlpha }
                    .onGloballyPositioned { coordinates ->
                        titleHeightPx = coordinates.size.height.toFloat()
                    }
            ) {
                Text(
                    text = "MultiLink",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp, bottom = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        searchBarHeightPx = coordinates.size.height.toFloat()
                    }
                    .padding(top = 8.dp)
            ) {
                JoinSessionSearchBar()
            }
        }
    }
}

@Composable
fun TrackingCardsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "Track your",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "See more",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrackingCard(
                title = "Family Circle",
                imageRes = R.drawable.family_3d,
                imageSize = 60,
                modifier = Modifier.weight(0.5f),
                height = 100
            )
            TrackingCard(
                title = "Live Trips",
                imageRes = R.drawable.trips_3d,
                imageSize = 64,
                modifier = Modifier.weight(0.5f),
                height = 100,
                topPadding = 6
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrackingCard(
                title = "Parcels",
                imageRes = R.drawable.parcel_3d,
                imageSize = 54,
                modifier = Modifier.weight(0.5f),
                height = 100
            )
            TrackingCard(
                title = "Vehicles & Trucks",
                imageRes = R.drawable.vehicles_3d,
                imageSize = 68,
                modifier = Modifier.weight(0.5f),
                height = 100,
                topPadding = 0
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TrackingCard(
            title = "See all",
            icon = Icons.Default.Apps,
            modifier = Modifier.fillMaxWidth(),
            height = 60,
            horizontalLayout = true
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElevateYourRideSection() {
    Text(
        text = "Elevate your ride",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    val elevatePagerState = rememberPagerState(pageCount = { 3 })
    HorizontalPager(
        state = elevatePagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSize = PageSize.Fixed(280.dp),
        pageSpacing = 8.dp
    ) { page ->
        when (page) {
            0 -> ElevateCard(
                title = "Request Uber XL",
                subtitle = "Spacious comfortable SUV rides",
                imageRes = R.drawable.uber_xl
            )

            1 -> ElevateCard(
                title = "Request Premier",
                subtitle = "Ride with top-rated drivers",
                imageRes = R.drawable.uber_premier
            )

            2 -> ElevateCard(
                title = "Uber Pet",
                subtitle = "Go with your pet",
                imageRes = R.drawable.uber_xl
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromoCardsSection() {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val promoPageWidth = screenWidth * 0.9f

    val promoPagerState = rememberPagerState(pageCount = { 4 })
    HorizontalPager(
        state = promoPagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSize = PageSize.Fixed(promoPageWidth),
        pageSpacing = 8.dp
    ) { page ->
        when (page) {
            0 -> PromoCard("Enjoy 5% off XL", "Book now")
            1 -> PromoCard("Get 10% off Moto", "Book now")
            2 -> PromoCard("Rentals discount", "Book now")
            3 -> PromoCard("Package delivery", "Book now")
        }
    }
}

@Composable
fun ElevateCard(title: String, subtitle: String, imageRes: Int) {
    val isDarkTheme = isSystemInDarkTheme()
    val bottomBgColor = if (isDarkTheme) Color(0xFF181818) else MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bottomBgColor)
            .clickable { }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        )

        Column(modifier = Modifier.padding(start = 0.dp, top = 6.dp, end = 12.dp, bottom = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TrackingCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector? = null,
    imageRes: Int? = null,
    imageSize: Int = 48,
    height: Int = 100,
    horizontalLayout: Boolean = false,
    topPadding: Int = 12
) {
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { }
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = topPadding.dp),
        contentAlignment = if (horizontalLayout) Alignment.Center else Alignment.TopCenter
    ) {
        if (horizontalLayout) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(imageSize.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PromoCard(title: String, buttonText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    buttonText,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Abstract illustration placeholder
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD1C1).copy(alpha = 0.8f))
        )
    }
}

@Composable
fun JoinSessionSearchBar(
    modifier: Modifier = Modifier
) {
    val isLight = !isSystemInDarkTheme()
    val bgColor = if (isLight) Color(0xFFF3F3F3) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isLight) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (isLight) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
    val pillBgColor = if (isLight) Color.White else MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(60.dp)
            .shadow(elevation = 2.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable { /* Handle click */ },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Join Link",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Join session",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // Right side "Link" button (resembling the "Later" button)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBgColor)
                    .clickable { /* Handle link click */ }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scan",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecentSessionsSection() {
    var showEmptyState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (!showEmptyState) {
            // Sessions State
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showEmptyState = true }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecentSessionCard(name = "Family trip", status = "Live")
                RecentSessionCard(name = "Weekend rental", status = "Paused")
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)
                    )
                    .clickable { showEmptyState = false },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No sessions",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "You have no sessions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun RecentSessionCard(name: String, status: String) {
    val isLive = status.equals("Live", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        // Middle Content (Name & Status)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isLive) Color(
                    0xFF048848
                ) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }

        // Right Chevron
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Details",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
