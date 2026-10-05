package com.example.multilink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multilink.ui.navigation.BottomNavDest
import com.example.multilink.ui.navigation.MultiLinkNavItem
import com.example.multilink.ui.navigation.navItems
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@Composable
fun MultiLinkNavigationBar(
    currentDestination: BottomNavDest,
    onDestinationSelected: (BottomNavDest) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    BottomNavigationBar(
        currentDestination = currentDestination,
        onDestinationSelected = onDestinationSelected,
        items = navItems,
        hazeState = hazeState,
        modifier = modifier
    )
}

@Composable
private fun BottomNavigationBar(
    currentDestination: BottomNavDest,
    onDestinationSelected: (BottomNavDest) -> Unit,
    items: List<MultiLinkNavItem>,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 10.dp), // Added 10.dp bottom padding
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.75f) // Made it even smaller in width
                .height(60.dp) // Decreased slight height
                .clip(CircleShape)
                .hazeEffect(state = hazeState) {
                    blurRadius = 20.dp
                }
                .background(Color.White.copy(alpha = 0.15f))
                .border(
                    width = 1.5.dp,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentDestination == item.dest
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onDestinationSelected(item.dest) },
                    contentAlignment = Alignment.Center
                ) {
                    val bgColor =
                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(bgColor)
                    ) {
                        Box {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.label,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
