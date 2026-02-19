package com.example.multilink.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapScope
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.viewport
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.ViewAnnotationOptions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.ui.text.font.FontWeight
import com.example.multilink.model.SessionParticipant
import com.example.multilink.ui.tracker.UserMapMarker
import com.mapbox.maps.viewannotation.geometry
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import com.example.multilink.R
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.mapbox.maps.CameraOptions

@OptIn(MapboxExperimental::class)
@Composable
fun MultiLinkMap(
    modifier: Modifier = Modifier,
    viewportState: MapViewportState,
    hasLocationPermission: Boolean,
    enableLocationPuck: Boolean = true,
    followUser: Boolean = false,
    topContentPadding: Dp = 0.dp,
    routePoints: List<Point> = emptyList(),
    onMapLoaded: () -> Unit = {},
    content: (@Composable MapboxMapScope.() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val currentBearing = viewportState.cameraState?.bearing ?: 0.0
    val isDarkTheme = isSystemInDarkTheme()

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = modifier.fillMaxSize(),
            mapViewportState = viewportState,
            compass = {},
            scaleBar = {}

        ) {
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
                    style.setStyleImportConfigProperty(
                        "basemap", "showPointOfInterestLabels", Value(true)
                    )
                    style.setStyleImportConfigProperty("basemap", "show3dBuildings", Value(true))

                    val lightPreset = if (isDarkTheme) "night" else "day"
                    style.setStyleImportConfigProperty("basemap", "lightPreset", Value(lightPreset))

                    try {
                        mapView.scalebar.updateSettings {
                            enabled = false
                        }
                    } catch (_: Exception) {
                    }

                    try {
                        if (hasLocationPermission && enableLocationPuck) {
                            mapView.location.updateSettings {
                                enabled = true
                                pulsingEnabled = true
                                locationPuck = createDefault2DPuck(withBearing = true)
                            }
                        }
                    } catch (_: Exception) {
                    }

                    onMapLoaded()
                }
            }

            // --- 2. FOLLOW USER LOGIC ---
            if (followUser && hasLocationPermission) {
                MapEffect(Unit) { mapView ->
                    try {
                        val viewportPlugin = mapView.viewport
                        val followPuckOptions = FollowPuckViewportStateOptions.Builder()
                            .zoom(16.5)
                            .pitch(0.0)
                            .build()
                        val followPuckState =
                            viewportPlugin.makeFollowPuckViewportState(followPuckOptions)
                        viewportPlugin.transitionTo(followPuckState)
                    } catch (_: Exception) {
                    }
                }
            }

            // --- 3. DRAW ROUTE LINE ---
            if (routePoints.isNotEmpty()) {
                PolylineAnnotation(
                    points = routePoints
                ) {
                    lineColor = Color(0xFF2196F3)
                    lineWidth = 6.0
                    lineOpacity = 1.0
                }
            }

            // --- 4. CUSTOM MARKERS ---
            if (content != null) {
                content()
            }
        }

        AnimatedVisibility(
            visible = kotlin.math.abs(currentBearing) > 1.0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 140.dp, end = 8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    viewportState.flyTo(
                        CameraOptions.Builder()
                            .bearing(0.0)
                            .build()
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.compass_logo),
                    contentDescription = "Reset North",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(32.dp)
                        .rotate((-currentBearing).toFloat())
                )
            }
        }
    }
}


@Composable
fun MapboxMapScope.SessionMapContent(
    sessionStartPoint: Point?,
    startLocName: String,
    destinationPoint: Point?,
    endLocName: String,
    userLocations: List<Pair<Point, SessionParticipant>>,
    currentUserId: String? = null,
) {
    // 1. START MARKER
    if (sessionStartPoint != null) {
        key(sessionStartPoint) {
            ViewAnnotation(
                options = ViewAnnotationOptions.Builder()
                    .geometry(sessionStartPoint)
                    .allowOverlap(true)
                    .ignoreCameraPadding(true)
                    .allowOverlapWithPuck(true)
                    .build()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StartLocationMarker(label = startLocName)
                }
            }
        }
    }

    // 2. END MARKER
    if (destinationPoint != null) {
        key(destinationPoint) {
            ViewAnnotation(
                options = ViewAnnotationOptions.Builder()
                    .geometry(destinationPoint)
                    .allowOverlap(true)
                    .ignoreCameraPadding(true)
                    .allowOverlapWithPuck(true)
                    .build()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EndLocationMarker(label = endLocName)
                }
            }
        }
    }

    // 3. USER MARKERS
    userLocations.forEach { (location, user) ->
        // If currentUserId is provided, hide that specific user (because they are the blue puck)
        if (currentUserId == null || user.id != currentUserId) {
            key(user.id) {
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .geometry(location)
                        .allowOverlap(true)
                        .ignoreCameraPadding(true)
                        .allowOverlapWithPuck(true)
                        .build()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = (0).dp)
                    ) {
                        UserMapMarker(user = user)
                    }
                }
            }
        }
    }
}

@Composable
fun MyLocationFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        modifier = modifier.size(48.dp)
    ) {
        Icon(Icons.Outlined.MyLocation, "Follow Me")
    }
}

@Composable
fun SessionControlBar(
    modifier: Modifier = Modifier,
    onStartClick: () -> Unit,
    onUserClick: () -> Unit,
    onEndClick: () -> Unit,
    showUserButton: Boolean = true
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .height(48.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(50)
            )
            .border(
                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onStartClick, modifier = Modifier.height(40.dp)) {
            Icon(Icons.Default.Flag, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Start", style = MaterialTheme.typography.labelMedium)
        }
        ControlDivider()
        if (showUserButton) {
            TextButton(onClick = onUserClick, modifier = Modifier.height(40.dp)) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("User", style = MaterialTheme.typography.labelMedium)
            }
            ControlDivider()
        }
        TextButton(onClick = onEndClick, modifier = Modifier.height(40.dp)) {
            Icon(Icons.Default.SportsScore, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("End", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ControlDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}


@Composable
fun StartLocationMarker(label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label.isNotEmpty() && label != "Start") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Gray, CircleShape)
                .border(2.dp, Color.LightGray, CircleShape)
        )
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun EndLocationMarker(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (label.isNotEmpty() && label != "End") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFFEA4335).copy(alpha = 0.3f))
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEA4335),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Box(contentAlignment = Alignment.Center) {
            // 1. Draw the Dot FIRST (Bottom Layer)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
            )

            // 2. Draw the Icon SECOND (Top Layer)
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFEA4335),
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-28).dp) // Moves the pin up so the tip touches the dot
            )
        }
    }
}


@Composable
fun MapTopBar(
    modifier: Modifier = Modifier,
    startName: String?,
    endName: String?,
    isViewerAdmin: Boolean = false,
    isTargetUserSelf: Boolean = false,
    isSessionAdminMode: Boolean = false,
    sessionStatus: String = "Live",
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onCallClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onPauseClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val notSelectedText = stringResource(R.string.loc_not_sel_by_host)
    val displayStart =
        if (startName.isNullOrEmpty() || startName == "Start") notSelectedText else startName
    val displayEnd = if (endName.isNullOrEmpty() || endName == "End") notSelectedText else endName

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left: Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. Center Left: Icon Strip
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .padding(top = 10.dp, bottom = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = "Start",
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(16.dp)
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .width(2.dp)
                        .padding(vertical = 2.dp)
                ) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        pathEffect = pathEffect,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "End",
                    tint = Color(0xFFEA4335),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Center Right: Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { onStartClick() }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                ) {
                    Text(
                        text = displayStart,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { onEndClick() }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = displayEnd,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(modifier = Modifier.align(Alignment.Top)) {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert, "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.widthIn(min = 140.dp),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_menu_sheet))
                ) {

                    if (isSessionAdminMode && isViewerAdmin) {
                        val isPaused = sessionStatus == "Paused"
                        DropdownMenuItem(
                            text = { Text(if (isPaused) "Resume Session" else "Pause Session") },
                            leadingIcon = {
                                Icon(
                                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    null
                                )
                            },
                            onClick = { showMenu = false; onPauseClick() }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete Session", color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { showMenu = false; onDeleteClick() }
                        )
                    }

                    if (!isSessionAdminMode && !isTargetUserSelf) {
                        DropdownMenuItem(
                            text = { Text("Call") },
                            leadingIcon = { Icon(Icons.Default.Call, null) },
                            onClick = { showMenu = false; onCallClick() }
                        )
                        if (isViewerAdmin) {
                            DropdownMenuItem(
                                text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { showMenu = false; onRemoveClick() }
                            )
                        }
                    }

                    // Common Option
                    DropdownMenuItem(
                        text = { Text("Session Info") },
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        onClick = { showMenu = false; onInfoClick() }
                    )
                }
            }
        }
    }
}