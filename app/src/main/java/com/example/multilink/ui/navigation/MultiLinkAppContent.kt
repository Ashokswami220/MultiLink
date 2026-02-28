package com.example.multilink.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.multilink.R
import com.google.firebase.auth.FirebaseAuth

data class MultiLinkNavItem(
    val dest: BottomNavDest,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiLinkTopBar(
    modifier: Modifier = Modifier,
    onDrawerClick: () -> Unit,
    onProfileClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    profileColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    titleAlpha: Float = 1f,
    elevation: Dp = dimensionResource(id = R.dimen.elevation_top_bar),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val myPhotoUrl = auth.currentUser?.photoUrl?.toString()

    CenterAlignedTopAppBar(
        modifier = modifier.shadow(elevation = elevation),
        title = {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.alpha(titleAlpha)
            )
        },
        navigationIcon = {
            IconButton(onClick = onDrawerClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(id = R.string.cd_open_drawer),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_menu)),
                )
            }
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                if (myPhotoUrl != null) {
                    AsyncImage(
                        model = myPhotoUrl,
                        contentDescription = stringResource(id = R.string.cd_profile),
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.icon_profile))
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(id = R.string.cd_profile),
                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_profile)),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = profileColor,
            titleContentColor = contentColor
        ),
        windowInsets = windowInsets
    )
}


val navItems
    @Composable get() = listOf(
        MultiLinkNavItem(
            dest = BottomNavDest.Home,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            label = stringResource(id = R.string.nav_home)
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

@Composable
fun MultiLinkNavigationBar(
    currentDestination: BottomNavDest,
    onDestinationSelected: (BottomNavDest) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomNavigationBar(
        currentDestination = currentDestination,
        onDestinationSelected = onDestinationSelected,
        items = navItems,
        modifier = modifier
    )
}

@Composable
private fun BottomNavigationBar(
    currentDestination: BottomNavDest,
    onDestinationSelected: (BottomNavDest) -> Unit,
    items: List<MultiLinkNavItem>,
    modifier: Modifier = Modifier,
) {
    val barCornerSize = dimensionResource(id = R.dimen.corner_bottom_bar)

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = barCornerSize, topEnd = barCornerSize),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = dimensionResource(id = R.dimen.elevation_card),
        shadowElevation = dimensionResource(id = R.dimen.elevation_dialog)
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding()
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = dimensionResource(id = R.dimen.divider_thickness)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(id = R.dimen.bottom_bar_height))
                    .padding(horizontal = dimensionResource(id = R.dimen.padding_standard)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentDestination == item.dest
                    val interactionSource = remember { MutableInteractionSource() }

                    // --- Animation State ---
                    val scale = remember { Animatable(1f) }

                    LaunchedEffect(isSelected) {
                        if (isSelected) {
                            scale.animateTo(
                                targetValue = 1.2f,
                                animationSpec = tween(durationMillis = 150)
                            )
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        } else {
                            scale.snapTo(1f)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onDestinationSelected(item.dest) }
                    ) {

                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isSelected)
                                dimensionResource(id = R.dimen.nav_indicator_width_selected)
                            else
                                dimensionResource(id = R.dimen.nav_indicator_width_unselected),
                            label = "widthAnim",
                            animationSpec = spring(dampingRatio = 0.6f)
                        )

                        Box(
                            modifier = Modifier
                                .height(dimensionResource(id = R.dimen.nav_indicator_height))
                                .width(indicatorWidth)
                                .clip(
                                    RoundedCornerShape(dimensionResource(id = R.dimen.corner_pill))
                                )
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .size(dimensionResource(id = R.dimen.icon_nav))
                                    .scale(scale.value)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(
                                dimensionResource(id = R.dimen.padding_nav_item_spacer)
                            )
                        )

                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = with(LocalDensity.current) {
                                    dimensionResource(id = R.dimen.text_nav_label).toSp()
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }
}

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
                // ⭐ FIXED: Grouped items tightly together and centered them vertically
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

                    // ⭐ FIXED: Removed the sweeping fraction animation. Now it's a solid pill clip.
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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

@Composable
fun rememberSingleClick(
    delayMillis: Long = 500L,
    onClick: () -> Unit,
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > delayMillis) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    MaterialTheme {
        MultiLinkNavigationBar(
            currentDestination = BottomNavDest.Home,
            onDestinationSelected = {}
        )
    }
}