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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(sessionId))
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RealtimeRepository() }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val actionHandler = remember { SessionActionHandler(context, repository, scope) }

    val (showMenu, setShowMenu) = remember { mutableStateOf(false) }
    val (showSortOptions, setShowSortOptions) = remember { mutableStateOf(false) }
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

    var currentSortType by remember { mutableStateOf(SeeAllSortType.JOINED_FIRST) }
    val sortedParticipants = remember(uiState.participants, currentSortType) {
        when (currentSortType) {
            SeeAllSortType.A_Z -> uiState.participants.sortedBy { it.name.lowercase() }
            SeeAllSortType.Z_A -> uiState.participants.sortedByDescending { it.name.lowercase() }
            SeeAllSortType.JOINED_FIRST -> uiState.participants
            SeeAllSortType.JOINED_LATE -> uiState.participants.reversed()
        }
    }

    fun debounceClick(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 1000) {
            lastClickTime = now
            action()
        }
    }
    LaunchedEffect(uiState.isSessionActive) {
        if (!uiState.isSessionActive && !uiState.isLoading) {
            Toast.makeText(context, "Session Ended by Host", Toast.LENGTH_LONG)
                .show()
            onBackClick()
        }
    }

    LaunchedEffect(uiState.isRemoved) {
        if (uiState.isRemoved) {
            Toast.makeText(context, "You were removed from the session", Toast.LENGTH_LONG)
                .show()
            onBackClick()
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
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = {
                                setShowMenu(true)
                                setShowSortOptions(false)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { setShowMenu(false) },
                                shape = RoundedCornerShape(
                                    dimensionResource(id = R.dimen.corner_menu_sheet)
                                ),
                                offset = DpOffset(x = (-12).dp, y = 0.dp)
                            ) {
                                if (showSortOptions) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Back", color = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowLeft, null
                                            )
                                        },
                                        onClick = { setShowSortOptions(false) }
                                    )
                                    HorizontalDivider()

                                    // A-Z
                                    DropdownMenuItem(
                                        text = { Text("Name: A - Z") },
                                        leadingIcon = {
                                            if (currentSortType == SeeAllSortType.A_Z)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SeeAllSortType.A_Z
                                            setShowMenu(false)
                                        }
                                    )
                                    // Z-A
                                    DropdownMenuItem(
                                        text = { Text("Name: Z - A") },
                                        leadingIcon = {
                                            if (currentSortType == SeeAllSortType.Z_A)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SeeAllSortType.Z_A
                                            setShowMenu(false)
                                        }
                                    )
                                    // Joined First
                                    DropdownMenuItem(
                                        text = { Text("Joined First") },
                                        leadingIcon = {
                                            if (currentSortType == SeeAllSortType.JOINED_FIRST)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SeeAllSortType.JOINED_FIRST
                                            setShowMenu(false)
                                        }
                                    )
                                    // Joined Late
                                    DropdownMenuItem(
                                        text = { Text("Joined Late") },
                                        leadingIcon = {
                                            if (currentSortType == SeeAllSortType.JOINED_LATE)
                                                Icon(
                                                    Icons.Default.FiberManualRecord, null,
                                                    modifier = Modifier.size(8.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                        },
                                        onClick = {
                                            currentSortType = SeeAllSortType.JOINED_LATE
                                            setShowMenu(false)
                                        }
                                    )

                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Session Info") },
                                        leadingIcon = { Icon(Icons.Default.Info, null) },
                                        onClick = {
                                            setShowMenu(false)
                                            setShowInfoDialog(true)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Sort by") },
                                        trailingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight, null
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

                                        val isPaused = currentSessionStatus == "Paused"
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (isPaused) "Resume Session" else "Pause Session"
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                    null
                                                )
                                            },
                                            onClick = {
                                                setShowMenu(false)
                                                setShowPauseDialog(true)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Delete Session",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete, null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                setShowMenu(false)
                                                setShowDeleteDialog(true)
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
                    thickness = 1.5.dp
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
                    start = 12.dp, end = 12.dp, top = 16.dp, bottom = 140.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.padding_large),
                        vertical = 16.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Session Lobby",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$userCount Active Participants",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onTrackClick,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading...")
                        } else {
                            Icon(Icons.Default.Map, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Track All",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Allow you to track all participants at once in one single map view.",
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
    onRemoveClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val distanceString = remember(user.lat, user.lng, destLat, destLng) {
        LocationUtils.calculateDistance(user.lat, user.lng, destLat, destLng)
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // HEADER ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Image with Status Dot
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
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
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    // Status Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(
                                if (user.status == "Online") Color(0xFF4CAF50) else Color.Gray
                            )
                            .border(
                                2.dp, MaterialTheme.colorScheme.surfaceContainerLow, CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isCardUserAdmin) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield, null, Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "ADMIN", style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                                ), color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert, "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        shape = RoundedCornerShape(
                            dimensionResource(id = R.dimen.corner_menu_sheet)
                        ),
                        offset = DpOffset(x = (-90).dp, y = (5).dp),
                        modifier = Modifier
                            .widthIn(max = 120.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Call", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Call, null, Modifier.size(18.dp)) },
                            onClick = { expanded = false; onCallClick() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        )
                        if (isViewerAdmin && !isCardUserAdmin) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Remove", color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete, null, Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { expanded = false; onRemoveClick() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            )
                        }
                    }
                }
            }

            // INFO BLOCK
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = phoneNumber.ifEmpty { "No Contact" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ⭐ FOOTER (Transparent background + Divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Reverted background to match card (transparent on top of card color)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // ⭐ ADDED DIVIDER
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.NearMe, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // ⭐ CHANGED TEXT FORMAT
                    Text(
                        text = if (distanceString == "...") "Calculating..." else "$distanceString to reach dest",
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