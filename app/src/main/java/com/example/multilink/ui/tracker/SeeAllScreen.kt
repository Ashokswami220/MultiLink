package com.example.multilink.ui.tracker

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.multilink.R
import com.example.multilink.model.SessionParticipant
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.dialogs.DeleteSessionDialog
import com.example.multilink.ui.components.dialogs.PauseSessionDialog
import com.example.multilink.ui.components.dialogs.SessionInfoDialog
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import com.example.multilink.utils.LocationUtils
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.multilink.ui.viewmodel.SessionUiEvent
import com.example.multilink.utils.HapticHelper
import kotlinx.coroutines.delay
import androidx.core.net.toUri


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    sessionId: String,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onTrackAllClick: (String) -> Unit,
    onSessionEnded: () -> Unit,
    onSessionPaused: () -> Unit
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()

    // Observed from ViewModel directly
    val sortedParticipants by viewModel.processedParticipants.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    val context = LocalContext.current
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe UI Events (Toasts, Navigation)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is SessionUiEvent.ShowToast -> Toast.makeText(
                    context, event.message, Toast.LENGTH_SHORT
                )
                    .show()

                is SessionUiEvent.NavigateBack -> onBackClick()
                else -> {}
            }
        }
    }

    // Clean Lifecycle Observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setSessionWatching(true)
                Lifecycle.Event.ON_STOP -> viewModel.setSessionWatching(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val (showMenu, setShowMenu) = remember { mutableStateOf(false) }
    val (showSortOptions, setShowSortOptions) = remember { mutableStateOf(false) }
    val (showFilterOptions, setShowFilterOptions) = remember { mutableStateOf(false) }
    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (showPauseDialog, setShowPauseDialog) = remember { mutableStateOf(false) }
    val (showInfoDialog, setShowInfoDialog) = remember { mutableStateOf(false) }
    val (isSearchExpanded, setSearchExpanded) = rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isSearchExpanded) {
        setSearchExpanded(false)
        viewModel.updateSearchQuery("")
    }

    fun debounceClick(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 1000) {
            lastClickTime = now
            action()
        }
    }

    // --- NAVIGATION GUARDS ---
    var isNavigatingOut by remember { mutableStateOf(false) }
    val msgSessionEnded = stringResource(R.string.msg_session_ended_host)
    val msgRemoved = stringResource(R.string.msg_removed_from_session)

    LaunchedEffect(
        uiState.isSessionActive, uiState.isRemoved, uiState.isLoading, uiState.sessionData?.status
    ) {
        if (uiState.isLoading || isNavigatingOut) return@LaunchedEffect
        val isPaused = uiState.sessionData?.status == "Paused"

        if (!uiState.isSessionActive) {
            isNavigatingOut = true
            Toast.makeText(context, msgSessionEnded, Toast.LENGTH_LONG)
                .show()
            onSessionEnded()
        } else if (uiState.isRemoved) {
            delay(100)
            isNavigatingOut = true
            Toast.makeText(context, msgRemoved, Toast.LENGTH_LONG)
                .show()
            onSessionEnded()
        } else if (isPaused && !uiState.isCurrentUserAdmin) {
            isNavigatingOut = true
            Toast.makeText(context, "Session paused by Host", Toast.LENGTH_LONG)
                .show()
            onSessionPaused()
        }
    }

    if (showDeleteDialog) {
        DeleteSessionDialog(
            onConfirm = { setShowDeleteDialog(false); viewModel.deleteSession() },
            onDismiss = { setShowDeleteDialog(false) }
        )
    }

    if (showPauseDialog) {
        val isPaused = uiState.sessionData?.status == "Paused"
        PauseSessionDialog(
            isPaused = isPaused,
            onConfirm = { setShowPauseDialog(false); viewModel.toggleSessionPause(isPaused) },
            onDismiss = { setShowPauseDialog(false) }
        )
    }

    if (showInfoDialog && uiState.sessionData != null) {
        SessionInfoDialog(session = uiState.sessionData!!, onDismiss = { setShowInfoDialog(false) })
    }

    Scaffold(
        topBar = {
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            Column {
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = isSearchExpanded,
                            transitionSpec = {
                                (expandHorizontally(expandFrom = Alignment.End) + fadeIn(
                                    tween(300)
                                )) togetherWith
                                        (shrinkHorizontally(
                                            shrinkTowards = Alignment.End
                                        ) + fadeOut(tween(300)))
                            }, label = "SearchBarAnimation"
                        ) { expanded ->
                            if (expanded) {
                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                TextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        viewModel.updateSearchQuery(
                                            it
                                        )
                                    }, // <-- Updated
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    placeholder = {
                                        Text(
                                            "Search users...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                HapticHelper.trigger(
                                                    context, HapticHelper.Type.LIGHT
                                                )
                                                viewModel.updateSearchQuery("")
                                            }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear Search"
                                                )
                                            }
                                        }
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                            } else {
                                Text(
                                    text = uiState.sessionTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                            if (isSearchExpanded) {
                                setSearchExpanded(false)
                                viewModel.updateSearchQuery("")
                                keyboardController?.hide()
                            } else {
                                onBackClick()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (!isSearchExpanded) {
                            IconButton(onClick = {
                                HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                setSearchExpanded(true)
                            }) { Icon(Icons.Default.Search, "Search") }

                            Box {
                                IconButton(onClick = {
                                    HapticHelper.trigger(context, HapticHelper.Type.LIGHT)
                                    setShowMenu(true); setShowSortOptions(
                                    false
                                ); setShowFilterOptions(false)
                                }) { Icon(Icons.Default.MoreVert, "Menu") }

                                DropdownMenu(
                                    expanded = showMenu, onDismissRequest = { setShowMenu(false) },
                                    shape = RoundedCornerShape(
                                        dimensionResource(id = R.dimen.corner_menu_sheet)
                                    )
                                ) {
                                    if (showSortOptions) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Back",
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                    null, tint = MaterialTheme.colorScheme.secondary
                                                )
                                            },
                                            onClick = { setShowSortOptions(false) }
                                        )
                                        HorizontalDivider()
                                        MenuRadioItem(
                                            "Name: A - Z", sortType == "A_Z"
                                        ) { viewModel.updateSortType("A_Z"); setShowMenu(false) }
                                        MenuRadioItem(
                                            "Name: Z - A", sortType == "Z_A"
                                        ) { viewModel.updateSortType("Z_A"); setShowMenu(false) }
                                        MenuRadioItem(
                                            "Joined First", sortType == "JOINED_FIRST"
                                        ) {
                                            viewModel.updateSortType("JOINED_FIRST"); setShowMenu(
                                            false
                                        )
                                        }
                                        MenuRadioItem(
                                            "Joined Late", sortType == "JOINED_LATE"
                                        ) {
                                            viewModel.updateSortType("JOINED_LATE"); setShowMenu(
                                            false
                                        )
                                        }
                                    } else if (showFilterOptions) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Back",
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                    null, tint = MaterialTheme.colorScheme.secondary
                                                )
                                            },
                                            onClick = { setShowFilterOptions(false) }
                                        )
                                        HorizontalDivider()
                                        MenuRadioItem(
                                            "Show All Users", filterType == "All"
                                        ) { viewModel.updateFilterType("All"); setShowMenu(false) }
                                        MenuRadioItem(
                                            "Show Active", filterType == "Active"
                                        ) {
                                            viewModel.updateFilterType("Active"); setShowMenu(
                                            false
                                        )
                                        }
                                        MenuRadioItem(
                                            "Show Paused", filterType == "Paused"
                                        ) {
                                            viewModel.updateFilterType("Paused"); setShowMenu(
                                            false
                                        )
                                        }
                                    } else {
                                        MenuActionItem(
                                            text = "Session Info", icon = Icons.Default.Info,
                                            onClick = {
                                                setShowMenu(
                                                    false
                                                ); setShowInfoDialog(true)
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Filter By") },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    null
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Default.FilterList, null) },
                                            onClick = { setShowFilterOptions(true) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Sort By") },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    null
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Sort, null
                                                )
                                            },
                                            onClick = { setShowSortOptions(true) }
                                        )

                                        if (uiState.isCurrentUserAdmin) {
                                            HorizontalDivider()
                                            val isPaused = uiState.sessionData?.status == "Paused"
                                            MenuActionItem(
                                                text = if (isPaused) "Resume Session" else "Pause Session",
                                                icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                onClick = {
                                                    setShowMenu(false); setShowPauseDialog(
                                                    true
                                                )
                                                }
                                            )
                                            MenuActionItem(
                                                text = "Delete Session",
                                                icon = Icons.Default.Delete,
                                                iconColor = MaterialTheme.colorScheme.error,
                                                textColor = MaterialTheme.colorScheme.error,
                                                onClick = {
                                                    setShowMenu(false); setShowDeleteDialog(
                                                    true
                                                )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.5.dp
                )
            }
        },
        bottomBar = {
            LiveTrackBottomBar(
                userCount = uiState.participants.size,
                onTrackClick = {
                    debounceClick {
                        val serviceIntent = Intent(context, LocationService::class.java).apply {
                            action = LocationService.ACTION_START
                            putExtra(LocationService.EXTRA_SESSION_ID, sessionId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(
                            serviceIntent
                        )
                        else context.startService(serviceIntent)

                        onTrackAllClick(sessionId)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // UI Model handles passing the specific profile data to the Grid Card now
                items(sortedParticipants, key = { it.participant.id }) { uiModel ->
                    UserGridCard(
                        user = uiModel.participant,
                        phoneNumber = uiModel.phoneNumber,
                        photoUrl = uiModel.photoUrl,
                        isCardUserAdmin = (uiModel.participant.id == uiState.hostId),
                        isViewerAdmin = uiState.isCurrentUserAdmin,
                        destLat = uiState.endLat,
                        destLng = uiState.endLng,
                        isArrivalTrackingEnabled = uiState.sessionData?.isArrivalTrackingEnabled == true,
                        onToggleArrivedClick = {
                            viewModel.toggleUserArrived(
                                uiModel.participant.id, !uiModel.participant.hasArrived
                            )
                        },
                        onClick = { debounceClick { onUserClick(uiModel.participant.id) } },
                        onCallClick = {
                            if (uiModel.phoneNumber.isNotEmpty()) {
                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    "tel:${uiModel.phoneNumber}".toUri()
                                )
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(
                                    context, "No phone number available", Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        },
                        onRemoveClick = {
                            viewModel.removeUser(
                                uiModel.participant.id, uiModel.participant.name
                            )
                        },
                        onTogglePauseClick = {
                            viewModel.toggleUserPause(
                                uiModel.participant.id, uiModel.participant.status,
                                uiModel.participant.name
                            )
                        }

                    )
                }
            }
        }
    }
}

@Composable
fun LiveTrackBottomBar(
    userCount: Int,
    isLoading: Boolean = false,
    onTrackClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = dimensionResource(R.dimen.elevation_dialog)
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = dimensionResource(R.dimen.divider_thickness)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.padding_large),
                        vertical = dimensionResource(R.dimen.padding_standard)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.header_session_lobby),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.format_active_participants, userCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onTrackClick,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_standard)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(
                            horizontal = dimensionResource(R.dimen.padding_extra_large),
                            vertical = dimensionResource(R.dimen.padding_medium)
                        ),
                        modifier = Modifier.heightIn(min = dimensionResource(R.dimen.icon_box_size))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = dimensionResource(R.dimen.stroke_width_standard)
                            )
                            Spacer(
                                modifier = Modifier.width(dimensionResource(R.dimen.padding_small))
                            )
                            Text(stringResource(R.string.state_loading))
                        } else {
                            Icon(
                                Icons.Default.Map, null,
                                Modifier.size(dimensionResource(R.dimen.icon_small))
                            )
                            Spacer(
                                modifier = Modifier.width(dimensionResource(R.dimen.padding_small))
                            )
                            Text(
                                stringResource(R.string.btn_track_all),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_stat))
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_mini)))
                    Text(
                        text = stringResource(R.string.desc_track_all),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun UserGridCard(
    user: SessionParticipant,
    phoneNumber: String,
    photoUrl: String?,
    isCardUserAdmin: Boolean,
    isViewerAdmin: Boolean,
    destLat: Double,
    destLng: Double,
    isArrivalTrackingEnabled: Boolean,
    onToggleArrivedClick: () -> Unit,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onTogglePauseClick: () -> Unit
) {
    val (expanded, setExpanded) = remember { mutableStateOf(false) }

    val distanceString = remember(user.lat, user.lng, destLat, destLng) {
        LocationUtils.calculateDistance(user.lat, user.lng, destLat, destLng)
    }

    val isUserPaused = user.status == "Paused"
    val hasArrived = user.hasArrived

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_standard)),
        colors = CardDefaults.cardColors(
            containerColor = if (isUserPaused) MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            ) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.elevation_card)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.alpha(if (isUserPaused) 0.6f else 1f)) {
            // HEADER ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Image with Status Dot
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_box_size))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (photoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(
                                        dimensionResource(R.dimen.icon_inside_box)
                                    )
                                )
                            }
                        }
                    }
                    // Status Dot
                    Box(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.padding_medium))
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(
                                if (hasArrived) Color(0xFF4CAF50)
                                else if (isUserPaused) Color(0xFFFF9800)
                                else if (user.status == "Online") Color(0xFF4CAF50)
                                else Color.Gray
                            )
                            .border(
                                dimensionResource(R.dimen.stroke_width_standard),
                                MaterialTheme.colorScheme.surfaceContainerLow, CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isCardUserAdmin) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_small)),
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.padding_tiny))
                    ) {
                        Row(
                            Modifier.padding(
                                horizontal = dimensionResource(R.dimen.padding_mini),
                                vertical = dimensionResource(R.dimen.padding_tiny)
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield, null,
                                Modifier.size(dimensionResource(R.dimen.icon_micro)),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(dimensionResource(R.dimen.padding_tiny)))
                            Text(
                                stringResource(R.string.label_admin),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                                ), color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { setExpanded(true) },
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_inside_box))
                    ) {
                        Icon(
                            Icons.Default.MoreVert, stringResource(R.string.cd_menu),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { setExpanded(false) },
                        shape = RoundedCornerShape(
                            dimensionResource(id = R.dimen.corner_menu_sheet)
                        ),
                        offset = DpOffset(
                            x = -dimensionResource(R.dimen.menu_offset_x_large),
                            y = dimensionResource(R.dimen.menu_offset_y_small)
                        ),
                        modifier = Modifier
                            .widthIn(max = dimensionResource(R.dimen.menu_max_width))
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_call), fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Call, null, Modifier.size(
                                        dimensionResource(R.dimen.icon_small)
                                    )
                                )
                            },
                            onClick = { setExpanded(false); onCallClick() },
                            contentPadding = PaddingValues(
                                horizontal = dimensionResource(R.dimen.padding_medium),
                                vertical = 0.dp
                            )
                        )

                        if (isViewerAdmin && isArrivalTrackingEnabled) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (hasArrived) "Undo reached" else "Reached dest",
                                        fontSize = 14.sp,
                                        color = if (hasArrived) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (hasArrived) Icons.AutoMirrored.Filled.Undo else Icons.Default.TaskAlt,
                                        null, Modifier.size(dimensionResource(R.dimen.icon_small)),
                                        tint = if (hasArrived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                                    )
                                },
                                onClick = { setExpanded(false); onToggleArrivedClick() },
                                contentPadding = PaddingValues(
                                    horizontal = dimensionResource(R.dimen.padding_medium),
                                    vertical = 0.dp
                                )
                            )
                        }

                        if (isViewerAdmin && !isCardUserAdmin && !hasArrived) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isUserPaused) "Resume Tracking" else "Pause User",
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isUserPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        null,
                                        Modifier.size(dimensionResource(R.dimen.icon_small)),
                                    )
                                },
                                onClick = { setExpanded(false); onTogglePauseClick() },
                                contentPadding = PaddingValues(
                                    horizontal = dimensionResource(R.dimen.padding_medium),
                                    vertical = 0.dp
                                )
                            )
                        }

                        if (isViewerAdmin && !isCardUserAdmin) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.action_remove),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PersonRemove, null,
                                        Modifier.size(dimensionResource(R.dimen.icon_small)),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { setExpanded(false); onRemoveClick() },
                                contentPadding = PaddingValues(
                                    horizontal = dimensionResource(R.dimen.padding_medium),
                                    vertical = 0.dp
                                )
                            )
                        }
                    }
                }
            }

            // INFO BLOCK
            Column(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium))
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_micro)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone, null,
                        Modifier.size(dimensionResource(R.dimen.icon_tiny)),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.padding_tiny)))
                    Text(
                        text = phoneNumber.ifEmpty { stringResource(R.string.placeholder_number) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.padding_medium),
                        vertical = dimensionResource(R.dimen.padding_small)
                    )
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.padding_small))
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Display "Arrived" vs "Distance"
                    if (hasArrived) {
                        Icon(
                            Icons.Default.TaskAlt, null, tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_stat))
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_mini)))
                        Text(
                            "Arrived at Destination",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ), color = Color(0xFF4CAF50)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.NearMe, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_stat))
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_mini)))
                        Text(
                            text = if (distanceString == "...") stringResource(
                                R.string.state_calculating
                            ) else stringResource(R.string.format_distance_to_dest, distanceString),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ), color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRadioItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.indicator_dot_size)),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.indicator_dot_size)))
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.padding_medium), vertical = 0.dp
        )
    )
}

@Composable
private fun MenuActionItem(
    text: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text, color = textColor, fontSize = 14.sp) },
        leadingIcon = {
            Icon(icon, null, Modifier.size(dimensionResource(R.dimen.icon_small)), tint = iconColor)
        },
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.padding_medium), vertical = 0.dp
        )
    )
}