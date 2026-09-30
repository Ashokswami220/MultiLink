package com.example.multilink.ui.otherScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
            // --- LAYOUT 1 ---
            Column {
                Text(
                    text = "Layout 1: Staggered / Masonry",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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
            ExperimentCard(title = "Track your family", icon = Icons.Default.Groups, modifier = Modifier.weight(0.6f), height = 120)
            ExperimentCard(title = "Track Your trips", icon = Icons.Default.FlightTakeoff, modifier = Modifier.weight(0.4f), height = 120)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(title = "Track your Parcel", icon = Icons.Default.Inventory, modifier = Modifier.weight(0.4f), height = 100)
            ExperimentCard(title = "Vehicles & Trucks", icon = Icons.Default.LocalShipping, modifier = Modifier.weight(0.6f), height = 100)
        }
        Spacer(modifier = Modifier.height(12.dp))
        ExperimentCard(title = "See all", icon = Icons.Default.Apps, modifier = Modifier.fillMaxWidth(), height = 60, horizontalLayout = true)
    }
}

@Composable
fun Layout2() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(title = "Family", icon = Icons.Default.Groups, modifier = Modifier.weight(1f), height = 100)
            ExperimentCard(title = "Trips", icon = Icons.Default.FlightTakeoff, modifier = Modifier.weight(1f), height = 100)
            ExperimentCard(title = "Parcel", icon = Icons.Default.Inventory, modifier = Modifier.weight(1f), height = 100)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(title = "Vehicles, Trucks, logistics", icon = Icons.Default.LocalShipping, modifier = Modifier.weight(0.66f), height = 100)
            ExperimentCard(title = "See all", icon = Icons.Default.Apps, modifier = Modifier.weight(0.33f), height = 100)
        }
    }
}

@Composable
fun Layout3() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ExperimentCard(title = "Track your family", icon = Icons.Default.Groups, modifier = Modifier.fillMaxWidth(), height = 140)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(title = "Track Your trips", icon = Icons.Default.FlightTakeoff, modifier = Modifier.weight(1f), height = 80)
            ExperimentCard(title = "Track your Parcel", icon = Icons.Default.Inventory, modifier = Modifier.weight(1f), height = 80)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentCard(title = "Vehicles & Trucks", icon = Icons.Default.LocalShipping, modifier = Modifier.weight(1f), height = 80)
            ExperimentCard(title = "See all", icon = Icons.Default.Apps, modifier = Modifier.weight(1f), height = 80)
        }
    }
}

@Composable
fun ExperimentCard(
    title: String, 
    icon: ImageVector, 
    modifier: Modifier = Modifier, 
    height: Int,
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


