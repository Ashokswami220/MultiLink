package com.example.multilink.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.multilink.utils.HapticHelper

@Composable
fun SettingsScreen(
    onCustomizeProfileClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onExperimentClick: () -> Unit = {},
    onOldHomeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Mock states for the UI
    var selectedTheme by remember { mutableStateOf("System") }
    var isDynamicColorEnabled by remember { mutableStateOf(true) }
    var isHapticsEnabled by remember { mutableStateOf(true) }
    var isKeepAwakeEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Profile Card
        ProfileCard(onClick = onCustomizeProfileClick)

        // Appearance
        SettingsSection {
            ThemeSelector(
                selectedTheme = selectedTheme,
                onThemeSelected = {
                    if (isHapticsEnabled) HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                    selectedTheme = it
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSwitchItem(
                icon = Icons.Outlined.ColorLens,
                title = "Dynamic Color",
                subtitle = "Match app colors to your wallpaper",
                isChecked = isDynamicColorEnabled,
                onCheckedChange = {
                    isDynamicColorEnabled = it
                    if (isHapticsEnabled) HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                }
            )
        }

        // Preferences
        SettingsSection {
            SettingsSwitchItem(
                icon = Icons.Outlined.Vibration,
                title = "Haptic Feedback",
                subtitle = "Vibrate on button clicks",
                isChecked = isHapticsEnabled,
                onCheckedChange = {
                    isHapticsEnabled = it
                    if (it) HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SettingsSwitchItem(
                icon = Icons.Outlined.Visibility,
                title = "Keep Screen Awake",
                subtitle = "Prevent phone from sleeping while on map",
                isChecked = isKeepAwakeEnabled,
                onCheckedChange = {
                    isKeepAwakeEnabled = it
                    if (isHapticsEnabled) HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                }
            )
        }

        // Support Section (Hybrid - Horizontal + Full Width)
        SettingsSection {
            // Horizontal 3-button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SupportActionCard(icon = Icons.Outlined.Email, label = "Contact Us", onClick = { })
                SupportActionCard(
                    icon = Icons.Outlined.StarRate, label = "Rate The App", onClick = { })
                SupportActionCard(icon = Icons.Outlined.Share, label = "Share App", onClick = { })
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            // Full width item
            SettingsActionItem(
                icon = Icons.Outlined.Info,
                title = "About Us",
                onClick = { /* About Us Logic */ }
            )
        }

        // Quick Access
        SettingsSection {
            SettingsActionItem(
                icon = Icons.Outlined.Dashboard,
                title = "Sessions Dashboard",
                subtitle = "Access the classic sessions home screen",
                onClick = onOldHomeClick
            )
        }

        // Data & Account (Danger Zone)
        SettingsSection {
            SettingsActionItem(
                icon = Icons.Outlined.Science,
                title = "UI Experiments",
                subtitle = "Help us choose the best UI icons",
                iconTint = MaterialTheme.colorScheme.primary,
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = onExperimentClick
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SettingsActionItem(
                icon = Icons.Outlined.DeleteOutline,
                title = "Delete My Data",
                subtitle = "Clear all location and tracking history",
                iconTint = MaterialTheme.colorScheme.error,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { /* Clear Data Logic */ }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SettingsActionItem(
                icon = Icons.Outlined.PersonRemove,
                title = "Delete My Account",
                subtitle = "Permanently remove your account",
                iconTint = MaterialTheme.colorScheme.error,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { /* Delete Account Logic */ }
            )
        }

        // Sign Out Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                onClick = onSignOutClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun ProfileCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Tap to customize details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Edit Profile",
                tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun RowScope.SupportActionCard(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .weight(1f)
            .height(76.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ThemeSelector(selectedTheme: String, onThemeSelected: (String) -> Unit) {
    val themes = listOf("System", "Light", "Dark")
    val icons =
        listOf(Icons.Outlined.SettingsSuggest, Icons.Outlined.LightMode, Icons.Outlined.DarkMode)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        themes.forEachIndexed { index, theme ->
            val isSelected = selectedTheme == theme
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .clickable { onThemeSelected(theme) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = theme,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = theme,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .height(68.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(68.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}