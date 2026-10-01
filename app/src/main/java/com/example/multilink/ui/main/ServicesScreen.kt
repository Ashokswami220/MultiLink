package com.example.multilink.ui.main

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun ServicesScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TrackingCardsSection()
        Spacer(modifier = Modifier.height(32.dp))

        ElevateYourRideSection()
        Spacer(modifier = Modifier.height(32.dp))

        PromoCardsSection()
        Spacer(modifier = Modifier.height(120.dp)) // padding for bottom bar
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
                title = "Track your family",
                imageRes = R.drawable.family_3d,
                imageSize = 60,
                modifier = Modifier.weight(0.5f),
                height = 120
            )
            TrackingCard(
                title = "Track Your trips",
                imageRes = R.drawable.trips_3d,
                imageSize = 68,
                modifier = Modifier.weight(0.5f),
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
