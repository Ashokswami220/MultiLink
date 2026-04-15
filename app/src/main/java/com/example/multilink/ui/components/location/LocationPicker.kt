package com.example.multilink.ui.components.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.R
import com.example.multilink.ui.viewmodel.LocationSearchViewModel
import com.google.android.gms.location.LocationServices
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.viewport
import java.util.Locale

data class LocationDialogData(
    val point: Point,
    val coordinateString: String,
    val defaultName: String
)


@OptIn(MapboxExperimental::class)
@Composable
fun LocationPicker(
    onLocationSelected: (String, Point) -> Unit,
    onCancel: () -> Unit,
    viewModel: LocationSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDarkTheme = isSystemInDarkTheme()

    // 1. Location Client
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }


    // Map State
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(78.9629, 20.5937)) // Default India
            zoom(4.0)
            pitch(0.0)
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val showSearchResults by viewModel.showSearchResults.collectAsState()
    val selectedPlace by viewModel.selectedPlace.collectAsState()
    val moveToPlaceTrigger by viewModel.moveToPlaceTrigger.collectAsState()

    var isMapLoading by remember { mutableStateOf(true) }
    var recenterTrigger by remember { mutableIntStateOf(0) }

    // Dialog State
    var activeDialogData by remember { mutableStateOf<LocationDialogData?>(null) }

    // Permissions
    var hasLocationPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Helper to snap camera
    fun snapToUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    mapViewportState.setCameraOptions {
                        center(Point.fromLngLat(location.longitude, location.latitude))
                        zoom(16.0)
                        pitch(0.0)
                    }
                }
            }
        } catch (_: SecurityException) {
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            recenterTrigger++
            snapToUserLocation()
        }
    }

    // Initial Check
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            snapToUserLocation()
            recenterTrigger++
        }
    }

    fun getProximity(): String {
        val center = mapViewportState.cameraState?.center
        return if (center != null) "${center.longitude()},${center.latitude()}" else ""
    }


    Scaffold(
        floatingActionButton = {
            if (!isMapLoading) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(bottom = 40.dp)
                        .navigationBarsPadding()
                ) {
                    // MY LOCATION FAB
                    FloatingActionButton(
                        onClick = {
                            if (hasLocationPermission) {
                                recenterTrigger++
                                snapToUserLocation()
                                viewModel.clearSearch()
                                focusManager.clearFocus()
                            } else permissionLauncher.launch(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }, containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            Icons.Outlined.MyLocation, stringResource(id = R.string.cd_my_location)
                        )
                    }

                    // CONFIRM FAB
                    FloatingActionButton(
                        onClick = {
                            val currentCenter = mapViewportState.cameraState?.center
                            if (currentCenter != null) {
                                val lat = String.format(Locale.US, "%.4f", currentCenter.latitude())
                                val lng =
                                    String.format(Locale.US, "%.4f", currentCenter.longitude())
                                val defName =
                                    if (searchQuery.isNotEmpty() && selectedPlace != null) selectedPlace!!.name else ""

                                activeDialogData =
                                    LocationDialogData(currentCenter, "$lat, $lng", defName)
                            }
                        }, containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Check, stringResource(id = R.string.cd_confirm_location))
                    }
                }
            }
        }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // MAP
            MapboxMap(
                Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                scaleBar = {}
            ) {
                MapEffect(Unit) { mapView ->
                    mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->

                        val lightPreset = if (isDarkTheme) "night" else "day"
                        style.setStyleImportConfigProperty(
                            "basemap", "lightPreset", Value(lightPreset)
                        )
                        style.setStyleImportConfigProperty(
                            "basemap", "showPointOfInterestLabels", Value(true)
                        )
                        style.setStyleImportConfigProperty(
                            "basemap", "show3dBuildings", Value(true)
                        )

                        try {
                            mapView.scalebar.updateSettings {
                                enabled = false
                            }
                        } catch (_: Exception) {
                        }

                        isMapLoading = false
                    }
                }

                // Follow User
                MapEffect(recenterTrigger) { mapView ->
                    if (hasLocationPermission && recenterTrigger > 0) {
                        mapView.location.updateSettings {
                            enabled = true
                            pulsingEnabled = true
                            locationPuck = createDefault2DPuck(withBearing = true)
                        }
                        val viewportPlugin = mapView.viewport
                        val followPuckOptions = FollowPuckViewportStateOptions.Builder()
                            .zoom(17.5)
                            .pitch(0.0)
                            .build()
                        val state = viewportPlugin.makeFollowPuckViewportState(followPuckOptions)
                        viewportPlugin.transitionTo(state)
                    }
                }

                // Move to Place
                MapEffect(moveToPlaceTrigger) { mapView ->
                    moveToPlaceTrigger?.let { point ->
                        val cameraOptions = CameraOptions.Builder()
                            .center(point)
                            .zoom(18.0)
                            .build()
                        mapView.mapboxMap.setCamera(cameraOptions)
                        viewModel.clearMoveToTrigger()
                    }
                }

                // Selected Label
                if (selectedPlace != null) {
                    MapEffect(selectedPlace) { mapView ->
                        val style = mapView.mapboxMap.style
                        if (style != null) {
                            val sourceId = "selected-place-source"
                            val layerId = "selected-place-layer"
                            if (style.styleLayerExists(layerId)) style.removeStyleLayer(layerId)
                            if (style.styleSourceExists(sourceId)) style.removeStyleSource(sourceId)
                            val point = selectedPlace!!.point
                            val feature = Feature.fromGeometry(point)
                            feature.addStringProperty("label_name", selectedPlace!!.name)
                            style.addSource(geoJsonSource(sourceId) { feature(feature) })
                            style.addLayer(symbolLayer(layerId, sourceId) {
                                textField(get("label_name"))
                                textSize(14.0)
                                textAnchor(TextAnchor.TOP)
                                textOffset(listOf(0.0, 1.5))
                            })
                        }
                    }
                }
            }

            // LOADING
            if (isMapLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(enabled = false) {}, contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading Map...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                // CENTER PIN
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .offset(y = (-24).dp)
                )
            }

            // SEARCH BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(dimensionResource(id = R.dimen.padding_standard))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCancel, modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface, CircleShape
                        )
                    ) {
                        Icon(Icons.Default.Close, stringResource(id = R.string.cd_close_dialog))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            viewModel.onSearchQueryChanged(
                                it, getProximity()
                            )
                        }, // Route keystrokes to ViewModel
                        placeholder = { Text(stringResource(id = R.string.search_hint)) },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (isSearching) CircularProgressIndicator(
                                Modifier.size(20.dp), strokeWidth = 2.dp
                            )
                            else if (searchQuery.isNotEmpty()) Icon(
                                Icons.Default.Close, null, Modifier.clickable {
                                    viewModel.clearSearch() // Clear via ViewModel
                                    focusManager.clearFocus()
                                })
                            else Icon(
                                Icons.Default.Search, null, Modifier.clickable {
                                    viewModel.forceSearch(searchQuery, getProximity())
                                    focusManager.clearFocus()
                                })
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.forceSearch(searchQuery, getProximity())
                                focusManager.clearFocus()
                            })
                    )
                }

                // RESULTS LIST
                if (searchResults.isNotEmpty() && showSearchResults) {
                    Card(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 250.dp)
                    ) {
                        LazyColumn {
                            items(searchResults) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectResult(
                                                result
                                            ) //Handled in ViewModel without spawning Dispatchers.IO here
                                            focusManager.clearFocus()
                                        }
                                        .padding(16.dp)) {
                                    Text(result.name)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        // --- NAME DIALOG ---
        activeDialogData?.let { data ->
            var customName by remember { mutableStateOf(data.defaultName) }
            val isValid = customName.trim().length >= 3

            AlertDialog(
                onDismissRequest = { activeDialogData = null },
                title = { Text("Name this location") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Place Name") },
                            placeholder = { Text("e.g. Home, Park") },
                            isError = !isValid && customName.isNotEmpty(),
                            supportingText = {
                                if (!isValid) Text("Minimum 3 characters required")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Coordinates: ${data.coordinateString}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = isValid,
                        onClick = {
                            onLocationSelected(customName, data.point)
                            activeDialogData = null
                        }
                    ) {
                        Text("Save & Select")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialogData = null }) { Text("Cancel") }
                }
            )
        }
    }
}