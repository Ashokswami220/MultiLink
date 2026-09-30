package com.example.multilink.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import com.example.multilink.R

data class ServiceItem(
    val title: String,
    val icon: ImageVector,
    val badgeText: String? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServicesScreen() {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val promoPageWidth = screenWidth * 0.9f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // --- For You Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "For you",
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

        Spacer(modifier = Modifier.height(16.dp))

        // 4x2 Grid removed and replaced by Tracking Cards below.


        // --- Tracking Cards Section (Layout 1: Masonry) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrackingCard(
                    title = "Track your family",
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(0.6f),
                    height = 120
                )
                TrackingCard(
                    title = "Track Your trips",
                    icon = Icons.Default.FlightTakeoff,
                    modifier = Modifier.weight(0.4f),
                    height = 120
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrackingCard(
                    title = "Track your Parcel",
                    icon = Icons.Default.Inventory,
                    modifier = Modifier.weight(0.4f),
                    height = 100
                )
                TrackingCard(
                    title = "Vehicles , trcuks , logistics",
                    icon = Icons.Default.LocalShipping,
                    modifier = Modifier.weight(0.6f),
                    height = 100
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

        Spacer(modifier = Modifier.height(32.dp))

        // --- Elevate Your Ride ---
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

        Spacer(modifier = Modifier.height(32.dp))

        // --- Promos ---
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

        Spacer(modifier = Modifier.height(120.dp)) // padding for bottom bar
    }
}

@Composable
fun ServiceIcon(item: ServiceItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable { },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )

            if (item.badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        androidx.compose.foundation.Image(
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
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    height: Int = 100,
    horizontalLayout: Boolean = false
) {
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (horizontalLayout) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
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
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
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
