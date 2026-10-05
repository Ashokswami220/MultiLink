package com.example.multilink.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.multilink.R
import com.example.multilink.ui.navigation.BottomNavDest
import com.example.multilink.ui.navigation.navItems

@Composable
fun MultiLinkNavigationRail(
    currentDestination: BottomNavDest,
    onDestinationSelected: (BottomNavDest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = navItems

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(220.dp), // Acts as an expanded navigation drawer
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = dimensionResource(id = R.dimen.elevation_card),
        shadowElevation = dimensionResource(id = R.dimen.elevation_dialog)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Vertical + WindowInsetsSides.Start
                        )
                    )
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.Start
            ) {
                items.forEach { item ->
                    val isSelected = currentDestination == item.dest
                    val interactionSource = remember { MutableInteractionSource() }

                    // Icon Bounce Animation
                    val scale = remember { Animatable(1f) }

                    LaunchedEffect(isSelected) {
                        if (isSelected) {
                            scale.animateTo(1.2f, tween(150))
                            scale.animateTo(
                                1f, spring(
                                    Spring.DampingRatioMediumBouncy, Spring.StiffnessLow
                                )
                            )
                        } else {
                            scale.snapTo(1f)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp) // Standard expanded rail item height
                            .clip(RoundedCornerShape(50)) // Pill shape for the highlight
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .clickable(
                                interactionSource = interactionSource, indication = null
                            ) { onDestinationSelected(item.dest) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(dimensionResource(id = R.dimen.icon_nav))
                                    .scale(scale.value)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = with(LocalDensity.current) {
                                        dimensionResource(
                                            id = if (isSelected) R.dimen.text_nav_label_15 else R.dimen.text_nav_label
                                        ).toSp()
                                    },
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.ExtraLight
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Subtle right divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(id = R.dimen.divider_thickness))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            )
        }
    }
}
