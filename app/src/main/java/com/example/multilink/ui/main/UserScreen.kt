package com.example.multilink.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multilink.R
import com.example.multilink.ui.theme.MultiLinkTheme

// --- DATA MODEL ---
data class Viewer(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val accessDuration: String,
    val accessLevel: String = "Precise",
    val isAlert: Boolean = false
)

@Composable
fun UserScreen() {
    val context = LocalContext.current

    // --- FAKE DATA ---
    val viewers = remember {
        listOf(
            Viewer("1", "Mom", "+91 98765 12345", "Since 9:00 AM", "Precise"),
            Viewer("2", "Rahul (Manager)", "+91 99887 77665", "Since 20 mins ago", "Approx"),
            Viewer("3", "Unknown Device", "+91 00000 00000", "Active for 2 days", "Precise", isAlert = true),
            Viewer("4", "Priya Singh", "+91 11223 34455", "Just now", "Precise"),
            Viewer("5", "Dad", "+91 55667 77889", "Since 1h", "Approx")
        )
    }

    var isGhostMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = dimensionResource(id = R.dimen.padding_standard),
            bottom = 120.dp
        ),
        // Reduced spacing to make the list look connected
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        // --- 1. EDIT PROFILE CARD (Tertiary Color) ---
        item {
            EditProfileLinkCard(
                onClick = {
                    Toast.makeText(context, "Edit Profile Clicked", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))
        }

        // --- 2. GHOST MODE ---
        item {
            GhostModeCard(
                isChecked = isGhostMode,
                onCheckedChange = { isGhostMode = it }
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_large)))
        }

        // --- 3. SECTION HEADER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.padding_large))
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Who is tracking you",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${viewers.size} Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // --- 4. VIEWERS LIST (With Corner Logic) ---
        itemsIndexed(viewers) { index, viewer ->
            val isFirst = index == 0
            val isLast = index == viewers.lastIndex

            // Logic for Rounded Corners
            val shape = when {
                viewers.size == 1 -> RoundedCornerShape(dimensionResource(id = R.dimen.corner_standard))
                isFirst -> RoundedCornerShape(
                    topStart = dimensionResource(id = R.dimen.corner_standard),
                    topEnd = dimensionResource(id = R.dimen.corner_standard),
                    bottomStart = 2.dp,
                    bottomEnd = 2.dp
                )

                isLast -> RoundedCornerShape(
                    topStart = 2.dp,
                    topEnd = 2.dp,
                    bottomStart = dimensionResource(id = R.dimen.corner_standard),
                    bottomEnd = dimensionResource(id = R.dimen.corner_standard)
                )

                else -> RoundedCornerShape(2.dp) // Middle items are mostly square
            }

            ViewerCard(
                viewer = viewer,
                shape = shape,
                onStopClick = {
                    Toast.makeText(
                        context,
                        "Stopped sharing with ${viewer.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

// --- COMPOSABLES ---

@Composable
fun EditProfileLinkCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_large)),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_standard)),
        // Change: Tertiary Colors
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_standard))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary, // Darker tertiary for icon bg
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_medium)))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ashok Swami",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Edit profile & personal details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GhostModeCard(isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_large)),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_standard)),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_standard))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isChecked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_medium)))
                Column {
                    Text(
                        text = stringResource(id = R.string.label_ghost_mode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isChecked) "Location hidden from everyone" else stringResource(id = R.string.desc_ghost_mode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fixed Switch Colors for Visibility
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onErrorContainer,
                    // Make track lighter/distinct so it is visible against errorContainer
                    checkedTrackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}

@Composable
fun ViewerCard(
    viewer: Viewer,
    shape: Shape,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_large)),
        shape = shape,
        // Minimalist: Lighter background, smaller elevation
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT SIDE: Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (viewer.isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = viewer.name.take(1),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (viewer.isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = viewer.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Minimalist: Combined Status Line
                    Text(
                        text = "${viewer.accessLevel} • ${viewer.accessDuration}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (viewer.isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // RIGHT SIDE: Text Button "Stop"
            TextButton(
                onClick = onStopClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = "Stop",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserScreenUpdated() {
    MultiLinkTheme {
        UserScreen()
    }
}