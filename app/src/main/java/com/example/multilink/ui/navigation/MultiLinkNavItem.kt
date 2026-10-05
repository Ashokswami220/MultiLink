package com.example.multilink.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.multilink.R

data class MultiLinkNavItem(
    val dest: BottomNavDest,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
)

val navItems
    @Composable get() = listOf(
        MultiLinkNavItem(
            dest = BottomNavDest.Home,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            label = "Home"
        ),
        MultiLinkNavItem(
            dest = BottomNavDest.Activity,
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications,
            label = stringResource(id = R.string.nav_activity)
        ),
        MultiLinkNavItem(
            dest = BottomNavDest.Recent,
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History,
            label = stringResource(id = R.string.nav_recent)
        ),
        MultiLinkNavItem(
            dest = BottomNavDest.Settings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            label = stringResource(id = R.string.nav_settings)
        )
    )
