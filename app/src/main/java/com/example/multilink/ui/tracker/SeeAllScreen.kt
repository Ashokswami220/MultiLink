package com.example.multilink.ui.tracker

import android.content.Intent
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.service.LocationService
import com.example.multilink.ui.components.SessionActionHandler
import com.example.multilink.ui.components.dialogs.DeleteSessionDialog
import com.example.multilink.ui.components.dialogs.PauseSessionDialog
import com.example.multilink.ui.components.dialogs.SessionInfoDialog
import com.example.multilink.ui.viewmodel.SessionViewModel
import com.example.multilink.ui.viewmodel.SessionViewModelFactory
import com.example.multilink.utils.LocationUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class SeeAllSortType {
    A_Z, Z_A, JOINED_FIRST, JOINED_LATE
}

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RealtimeRepository() }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val actionHandler = remember { SessionActionHandler(context, repository, scope) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, sessionId) {
        var isWatching = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (!isWatching) {
                    repository.incrementSessionWatchers(sessionId)
                    isWatching = true
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                if (isWatching) {
                    repository.decrementSessionWatchers(sessionId)
                    isWatching = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isWatching) {
                repository.decrementSessionWatchers(sessionId)
                isWatching = false
            }
        }
    }

    val (showMenu, setShowMenu) = remember { mutableStateOf(false) }
    val (showSortOptions, setShowSortOptions) = remember { mutableStateOf(false) }
    val (showFilterOptions, setShowFilterOptions) = remember { mutableStateOf(false) }
    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (showPauseDialog, setShowPauseDialog) = remember { mutableStateOf(false) }
    val (showInfoDialog, setShowInfoDialog) = remember { mutableStateOf(false) }

    var currentSessionStatus by remember { mutableStateOf("Live") }
    LaunchedEffect(sessionId) {
        repository.getSessionDetails(sessionId)
            .collectLatest { data ->
                currentSessionStatus = data["status"] as? String ?: "Live"
            }
    }

    var currentUserFilter by remember { mutableStateOf("All") }
    var currentSortType by remember { mutableStateOf(SeeAllSortType.JOINED_FIRST) }

    val sortedParticipants = remember(uiState.participants, currentSortType, currentUserFilter) {
        val filteredList = when (currentUserFilter) {
            "Active" -> uiState.participants.filter { it.status != "Paused" }
            "Paused" -> uiState.participants.filter { it.status == "Paused" }
            else -> uiState.participants
        }

        when (currentSortType) {
            SeeAllSortType.A_Z -> filteredList.sortedBy { it.name.lowercase() }
            SeeAllSortType.Z_A -> filteredList.sortedByDescending { it.name.lowercase() }
            SeeAllSortType.JOINED_FIRST -> filteredList
            SeeAllSortType.JOINED_LATE -> filteredList.reversed()
        }
    }

    fun debounceClick(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 1000) {
            lastClickTime = now
            action()
        }
    }

    // --- NAVIGATION LOGIC ---
    var isNavigatingOut by remember { mutableStateOf(false) }

    LaunchedEffect(
        uiState.isSessionActive, uiState.isRemoved, uiState.isLoading, uiState.sessionData?.status
    ) {
        if (uiState.isLoading || isNavigatingOut) return@LaunchedEffect

        val isPaused = uiState.sessionData?.status == "Paused"
        val isAdmin = uiState.isCurrentUserAdmin

        if (!uiState.isSessionActive) {
            isNavigatingOut = true
            Toast.makeText(
                context, context.getString(R.string.msg_session_ended_host), Toast.LENGTH_LONG
            )
                .show()
            onSessionEnded()
        } else if (uiState.isRemoved) {
            kotlinx.coroutines.delay(100)
            isNavigatingOut = true
            Toast.makeText(
                context, context.getString(R.string.msg_removed_from_session), Toast.LENGTH_LONG
            )
                .show()
            onSessionEnded()
        } else if (isPaused && !isAdmin) {
            // ⭐ FIXED: Kick non-admins, but keep service alive
            isNavigatingOut = true
            Toast.makeText(context, "Session paused by Host", Toast.LENGTH_LONG)
                .show()
            onSessionPaused()
        }
    }

    if (showDeleteDialog) {
        DeleteSessionDialog(
            onConfirm = {
                setShowDeleteDialog(false)
                scope.launch { repository.stopSession(sessionId) }
            },
            onDismiss = { setShowDeleteDialog(false) }
        )
    }

    if (showPauseDialog) {
        val isPaused = currentSessionStatus == "Paused"
        PauseSessionDialog(
            isPaused = isPaused,
            onConfirm = {
                setShowPauseDialog(false)
                scope.launch { repository.updateSessionStatus(sessionId, !isPaused) }
            },
            onDismiss = { setShowPauseDialog(false) }
        )
    }

    if (showInfoDialog && uiState.sessionData != null) {
        SessionInfoDialog(
            session = uiState.sessionData!!,
            onDismiss = { setShowInfoDialog(false) }
        )
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = uiState.sessionTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = stringResource(R.string.cd_back_button),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { setShowFilterOptions(true); setShowMenu(false) }) {
                                Icon(
                                    Icons.Default.FilterList, "Filter",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showFilterOptions,
                                onDismissRequest = { setShowFilterOptions(false) },
                                shape = RoundedCornerShape(
                                    dimensionResource(id = R.dimen.corner_menu_sheet)
                                ),
                                offset = DpOffset(x = 0.dp, y = 0.dp)
                            ) {
                                MenuRadioItem(
                                    "Show All Users", currentUserFilter == "All"
                                ) { currentUserFilter = "All"; setShowFilterOptions(false) }
                                MenuRadioItem(
                                    "Show Active", currentUserFilter == "Active"
                                ) { currentUserFilter = "Active"; setShowFilterOptions(false) }
                                MenuRadioItem(
                                    "Show Paused", currentUserFilter == "Paused"
                                ) { currentUserFilter = "Paused"; setShowFilterOptions(false) }
                            }
                        }

                        // Existing More Options Button
                        Box {
                            IconButton(
                                onClick = {
                                    setShowMenu(true); setShowSortOptions(
                                    false
                                ); setShowFilterOptions(false)
                                }) {
                                Icon(
                                    Icons.Default.MoreVert, stringResource(R.string.cd_menu),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { setShowMenu(false) },
                                shape = RoundedCornerShape(
                                    dimensionResource(id = R.dimen.corner_menu_sheet)
                                ),
                                offset = DpOffset(
                                    x = -dimensionResource(R.dimen.padding_medium), y = 0.dp
                                )
                            ) {
                                if (showSortOptions) {
                                    // Sub-Menu: Sort
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.cd_back_button),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowLeft, null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        onClick = { setShowSortOptions(false) }
                                    )
                                    HorizontalDivider()

                                    MenuRadioItem(
                                        stringResource(R.string.sort_az),
                                        currentSortType == SeeAllSortType.A_Z
                                    ) { currentSortType = SeeAllSortType.A_Z; setShowMenu(false) }
                                    MenuRadioItem(
                                        stringResource(R.string.sort_za),
                                        currentSortType == SeeAllSortType.Z_A
                                    ) { currentSortType = SeeAllSortType.Z_A; setShowMenu(false) }
                                    MenuRadioItem(
                                        stringResource(R.string.sort_joined_first),
                                        currentSortType == SeeAllSortType.JOINED_FIRST
                                    ) {
                                        currentSortType = SeeAllSortType.JOINED_FIRST; setShowMenu(
                                        false
                                    )
                                    }
                                    MenuRadioItem(
                                        stringResource(R.string.sort_joined_late),
                                        currentSortType == SeeAllSortType.JOINED_LATE
                                    ) {
                                        currentSortType = SeeAllSortType.JOINED_LATE; setShowMenu(
                                        false
                                    )
                                    }

                                } else {
                                    // Main Menu
                                    MenuActionItem(
                                        text = stringResource(R.string.menu_session_info),
                                        icon = Icons.Default.Info,
                                        onClick = { setShowMenu(false); setShowInfoDialog(true) }
                                    )

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sort_by)) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight, null
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Sort, null, Modifier.size(
                                                    dimensionResource(
                                                        R.dimen.icon_small
                                                    )
                                                )
                                            )
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = dimensionResource(R.dimen.padding_medium),
                                            vertical = 0.dp
                                        ),
                                        onClick = { setShowSortOptions(true) }
                                    )

                                    if (uiState.isCurrentUserAdmin) {
                                        HorizontalDivider()

                                        val isPaused = currentSessionStatus == "Paused"
                                        MenuActionItem(
                                            text = if (isPaused) stringResource(
                                                R.string.menu_resume_session
                                            ) else stringResource(R.string.menu_pause_session),
                                            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            onClick = {
                                                setShowMenu(false); setShowPauseDialog(
                                                true
                                            )
                                            }
                                        )

                                        MenuActionItem(
                                            text = stringResource(R.string.menu_delete_session),
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = dimensionResource(R.dimen.divider_thickness_thick)
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
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
                contentPadding = PaddingValues(
                    start = dimensionResource(R.dimen.padding_medium),
                    end = dimensionResource(R.dimen.padding_medium),
                    top = dimensionResource(R.dimen.padding_standard),
                    bottom = dimensionResource(R.dimen.padding_list_bottom)
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.grid_spacing)
                ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_spacing)),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedParticipants, key = { it.id }) { user ->
                    var userPhone by rememberSaveable { mutableStateOf("") }
                    var userPhoto by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(user.id) {
                        val profile = repository.getGlobalUserProfile(user.id)
                        if (profile != null) {
                            userPhone = profile["phone"] ?: ""
                            userPhoto = profile["photoUrl"]?.takeIf { it.isNotEmpty() }
                        }
                    }
                    UserGridCard(
                        user = user,
                        phoneNumber = userPhone,
                        photoUrl = userPhoto,
                        isCardUserAdmin = (user.id == uiState.hostId),
                        isViewerAdmin = uiState.isCurrentUserAdmin,
                        destLat = uiState.endLat,
                        destLng = uiState.endLng,
                        onClick = { debounceClick { onUserClick(user.id) } },
                        onCallClick = { actionHandler.onCall(userPhone) },
                        onRemoveClick = {
                            actionHandler.onRemoveUser(sessionId, user.id, user.name)
                        },
                        onTogglePauseClick = {
                            actionHandler.onToggleUserPause(
                                sessionId, user.id, user.status, user.name
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
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onTogglePauseClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val distanceString = remember(user.lat, user.lng, destLat, destLng) {
        LocationUtils.calculateDistance(user.lat, user.lng, destLat, destLng)
    }

    val isUserPaused = user.status == "Paused"

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
                                if (isUserPaused) Color(
                                    0xFFFF9800
                                ) else if (user.status == "Online") Color(
                                    0xFF4CAF50
                                ) else Color.Gray
                            )
                            .border(
                                dimensionResource(R.dimen.stroke_width_standard),
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                CircleShape
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
                        onClick = { expanded = true },
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_inside_box))
                    ) {
                        Icon(
                            Icons.Default.MoreVert, stringResource(R.string.cd_menu),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
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
                            onClick = { expanded = false; onCallClick() },
                            contentPadding = PaddingValues(
                                horizontal = dimensionResource(R.dimen.padding_medium),
                                vertical = 0.dp
                            )
                        )

                        if (isViewerAdmin && !isCardUserAdmin) {
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
                                onClick = { expanded = false; onTogglePauseClick() },
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
                                        Icons.Default.Delete, null,
                                        Modifier.size(dimensionResource(R.dimen.icon_small)),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { expanded = false; onRemoveClick() },
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
                    Icon(
                        Icons.Outlined.NearMe, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_stat))
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_mini)))

                    Text(
                        text = if (distanceString == "...") stringResource(
                            R.string.state_calculating
                        ) else stringResource(R.string.format_distance_to_dest, distanceString),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
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