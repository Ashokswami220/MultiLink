package com.example.multilink.ui.otherScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.multilink.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentScreen(onNavigateBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UI Experiments") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- FOR YOU EXPERIMENT ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "For You (3D Illustrations)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ForYouExperimentSection()
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // --- LAYOUT 1 ---
            Column {
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
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "See more",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Layout1()
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // --- LAYOUT 2 ---
            Column {
                Text(
                    text = "Layout 2: 3-Column Top",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Layout2()
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // --- LAYOUT 3 ---
            Column {
                Text(
                    text = "Layout 3: Hero Banner",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Layout3()
            }
        }
    }
}

@Composable
fun Layout1() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(
                title = "Family Circle", imageRes = R.drawable.family_3d, imageSize = 60,
                modifier = Modifier.weight(0.5f), height = 120
            )
            ExperimentCard(
                title = "Live Trips", imageRes = R.drawable.trips_3d, imageSize = 64,
                modifier = Modifier.weight(0.5f), height = 120
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(
                title = "Parcels", imageRes = R.drawable.parcel_3d, imageSize = 50,
                modifier = Modifier.weight(0.5f), height = 100
            )
            ExperimentCard(
                title = "Vehicles & Trucks", imageRes = R.drawable.vehicles_3d, imageSize = 64,
                modifier = Modifier.weight(0.5f), height = 100, topPadding = 2
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        ExperimentCard(
            title = "See all", icon = Icons.Default.Apps, modifier = Modifier.fillMaxWidth(),
            height = 60, horizontalLayout = true
        )
    }
}

@Composable
fun Layout2() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(
                title = "Family", icon = Icons.Default.Groups, modifier = Modifier.weight(1f),
                height = 100
            )
            ExperimentCard(
                title = "Trips", icon = Icons.Default.FlightTakeoff, modifier = Modifier.weight(1f),
                height = 100
            )
            ExperimentCard(
                title = "Parcel", icon = Icons.Default.Inventory, modifier = Modifier.weight(1f),
                height = 100
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(
                title = "Vehicles, Trucks, logistics", icon = Icons.Default.LocalShipping,
                modifier = Modifier.weight(0.66f), height = 100
            )
            ExperimentCard(
                title = "See all", icon = Icons.Default.Apps, modifier = Modifier.weight(0.33f),
                height = 100
            )
        }
    }
}

@Composable
fun Layout3() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ExperimentCard(
            title = "Track your family", icon = Icons.Default.Groups,
            modifier = Modifier.fillMaxWidth(), height = 140
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(
                title = "Track Your trips", icon = Icons.Default.FlightTakeoff,
                modifier = Modifier.weight(1f), height = 80
            )
            ExperimentCard(
                title = "Track your Parcel", icon = Icons.Default.Inventory,
                modifier = Modifier.weight(1f), height = 80
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(
                title = "Vehicles & Trucks", icon = Icons.Default.LocalShipping,
                modifier = Modifier.weight(1f), height = 80
            )
            ExperimentCard(
                title = "See all", icon = Icons.Default.Apps, modifier = Modifier.weight(1f),
                height = 80
            )
        }
    }
}

@Composable
fun ExperimentCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector? = null,
    imageRes: Int? = null,
    imageSize: Int = 48,
    height: Int,
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
                Spacer(modifier = Modifier.width(4.dp))
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
fun ForYouExperimentSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ForYouExperimentIcon("Trips", R.drawable.trips_3d, imageSize = 48)
            ForYouExperimentIcon("Family", R.drawable.family_3d, imageSize = 48)
            ForYouExperimentIcon("Parcel", R.drawable.parcel_3d, imageSize = 40)
            ForYouExperimentIcon("Vehicles", R.drawable.vehicles_3d, imageSize = 48)
        }
    }
}

@Composable
fun ForYouExperimentIcon(
    title: String, imageRes: Int?, imageSize: Int = 48, isMore: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable { },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isMore) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            } else if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(imageSize.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


