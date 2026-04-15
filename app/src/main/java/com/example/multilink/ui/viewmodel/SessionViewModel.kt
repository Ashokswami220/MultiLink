package com.example.multilink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multilink.model.RecentSession
import com.example.multilink.model.toSessionData
import com.example.multilink.repo.RealtimeRepository
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.multilink.model.SessionUiState
import com.example.multilink.model.SessionParticipant
import com.example.multilink.repo.RouteResult
import kotlinx.coroutines.delay

// --- 1. UI Event (One-time actions) ---
sealed class SessionUiEvent {
    data class ShowToast(val message: String) : SessionUiEvent()
    object NavigateBack : SessionUiEvent()
    data class ShowTooFarDialog(val distanceMeters: Int) : SessionUiEvent()
    data class ShowArrivedToggleDialog(val isArriving: Boolean) : SessionUiEvent()
}

// --- 2. Combined Model for the UI ---
data class ParticipantUiModel(
    val participant: SessionParticipant,
    val phoneNumber: String,
    val email: String,
    val photoUrl: String?
)

class SessionViewModel(
    private val sessionId: String,
    private val repository: RealtimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("All")
    val filterType = _filterType.asStateFlow()

    private val _sortType = MutableStateFlow("JOINED_FIRST")
    val sortType = _sortType.asStateFlow()

    private val userProfileCache = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())

    // --- NEW: Hold the Mapbox Route in the ViewModel ---
    private val _routePoints = MutableStateFlow<List<Point>>(emptyList())
    val routePoints: StateFlow<List<Point>> = _routePoints.asStateFlow()

    // Hold the full turn-by-turn navigation route
    private val _navigationRoute = MutableStateFlow<RouteResult?>(null)
    val navigationRoute: StateFlow<RouteResult?> = _navigationRoute.asStateFlow()

    // Observing _uiState directly so we can access the hostId for sorting
    val processedParticipants: StateFlow<List<ParticipantUiModel>> = combine(
        _uiState, userProfileCache, _searchQuery, _filterType, _sortType
    ) { state, profiles, query, filter, sort ->
        var list = state.participants.map { p ->
            val profile = profiles[p.id]
            ParticipantUiModel(
                participant = p,
                phoneNumber = profile?.get("phone") ?: "",
                email = profile?.get("email") ?: "",
                photoUrl = profile?.get("photoUrl")
                    ?.takeIf { it.isNotEmpty() }
            )
        }

        if (query.isNotBlank()) list =
            list.filter { it.participant.name.contains(query, ignoreCase = true) }

        list = when (filter) {
            "Active" -> list.filter { it.participant.status != "Paused" }
            "Paused" -> list.filter { it.participant.status == "Paused" }
            else -> list
        }

        list = when (sort) {
            "A_Z" -> list.sortedBy { it.participant.name.lowercase() }
            "Z_A" -> list.sortedByDescending { it.participant.name.lowercase() }
            "JOINED_FIRST" -> list
            "JOINED_LATE" -> list.reversed()
            else -> list
        }

        val hostNode = list.find { it.participant.id == state.hostId }
        if (hostNode != null) {
            val mutableList = list.toMutableList()
            mutableList.remove(hostNode)
            mutableList.add(0, hostNode)
            list = mutableList
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvents = MutableSharedFlow<SessionUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _recentSessions = MutableStateFlow<List<RecentSession>>(emptyList())
    val recentSessions: StateFlow<List<RecentSession>> = _recentSessions.asStateFlow()

    private var participantsJob: kotlinx.coroutines.Job? = null
    private var isWatchingSession = false

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        _uiState.update { it.copy(currentUserId = currentUser) }

        if (sessionId.isNotEmpty()) {
            monitorSessionDetails()
            monitorSessionStatus()
            monitorSelfRemoval()
            startOfflineTicker()
        }
        fetchRecentSessions()
    }

    // --- ACTIONS EXPOSED TO UI ---

    fun setSpecificUserWatching(userId: String, isWatching: Boolean) {
        if (isWatching) {
            repository.incrementUserWatchers(sessionId, userId)
        } else {
            repository.decrementUserWatchers(sessionId, userId)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterType(type: String) {
        _filterType.value = type
    }

    fun updateSortType(type: String) {
        _sortType.value = type
    }

    fun setSessionWatching(isWatching: Boolean) {
        if (isWatching && !isWatchingSession) {
            repository.incrementSessionWatchers(sessionId)
            isWatchingSession = true
        } else if (!isWatching && isWatchingSession) {
            repository.decrementSessionWatchers(sessionId)
            isWatchingSession = false
        }
    }

    fun toggleSessionPause(isCurrentlyPaused: Boolean) = viewModelScope.launch {
        repository.updateSessionStatus(sessionId, !isCurrentlyPaused)
        val msg = if (!isCurrentlyPaused) "Session is paused" else "Session is resumed"
        _uiEvents.emit(SessionUiEvent.ShowToast(msg))
    }

    fun deleteSession() = viewModelScope.launch {
        repository.stopSession(sessionId)
        _uiEvents.emit(SessionUiEvent.NavigateBack)
    }

    fun removeUser(userId: String, userName: String) = viewModelScope.launch {
        repository.removeUser(sessionId, userId)
        _uiEvents.emit(SessionUiEvent.ShowToast("Removed $userName"))
    }

    fun toggleUserPause(userId: String, currentStatus: String, userName: String) =
        viewModelScope.launch {
            val newStatus = if (currentStatus == "Paused") "Online" else "Paused"
            repository.updateUserStatusForAdmin(sessionId, userId, newStatus)
            _uiEvents.emit(SessionUiEvent.ShowToast("$userName is now $newStatus"))
        }

    fun toggleUserArrived(userId: String, isArriving: Boolean) = viewModelScope.launch {
        repository.toggleUserArrivedStatus(sessionId, userId, isArriving)
    }

    fun fetchNavigationRoute(currentLocation: Point) {
        val dest = _uiState.value.endPoint ?: return
        viewModelScope.launch {
            val token = com.example.multilink.BuildConfig.MAPBOX_ACCESS_TOKEN
            val routeRepo = com.example.multilink.repo.RouteRepository(token)
            _navigationRoute.value = routeRepo.getNavigationRoute(currentLocation, dest)
        }
    }

    // --- NEW: Check In Logic handled purely in ViewModel ---
    fun attemptCheckIn(currentLocation: Point?, isArriving: Boolean) {
        if (!isArriving) {
            // If they are un-marking, no need to check distance! Just ask for confirmation.
            viewModelScope.launch { _uiEvents.emit(SessionUiEvent.ShowArrivedToggleDialog(false)) }
            return
        }

        val dest = _uiState.value.endPoint
        if (dest == null) {
            viewModelScope.launch {
                _uiEvents.emit(
                    SessionUiEvent.ShowToast("Destination not set by host.")
                )
            }
            return
        }
        if (currentLocation == null) {
            viewModelScope.launch {
                _uiEvents.emit(
                    SessionUiEvent.ShowToast("Waiting for your GPS location...")
                )
            }
            return
        }

        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            currentLocation.latitude(), currentLocation.longitude(),
            dest.latitude(), dest.longitude(), results
        )
        val dist = results[0]

        viewModelScope.launch {
            if (dist > 200f) {
                _uiEvents.emit(SessionUiEvent.ShowTooFarDialog(dist.toInt()))
            } else {
                _uiEvents.emit(SessionUiEvent.ShowArrivedToggleDialog(true))
            }
        }
    }

    // --- RECENT SESSIONS & INTERNAL LOGIC ---

    private fun fetchRecentSessions() {
        viewModelScope.launch {
            repository.getRecentSessions()
                .collectLatest { _recentSessions.value = it }
        }
    }

    fun deleteRecentSession(sessionId: String) {
        viewModelScope.launch { repository.deleteRecentSession(sessionId) }
    }

    private fun checkAndFetchMissingProfiles(participants: List<SessionParticipant>) {
        val currentCache = userProfileCache.value
        val missingIds = participants.map { it.id }
            .filter { !currentCache.containsKey(it) }
        if (missingIds.isNotEmpty()) {
            viewModelScope.launch {
                val newProfiles = missingIds.associateWith { id ->
                    repository.getGlobalUserProfile(id) ?: emptyMap()
                }
                userProfileCache.update { it + newProfiles }
            }
        }
    }

    private fun startOfflineTicker() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                val now = System.currentTimeMillis()
                _uiState.update { state ->
                    val refreshedParticipants = state.participants.map { user ->
                        if (user.status != "Paused" && user.status != "Arrived" && user.lastUpdated > 0 && (now - user.lastUpdated) > 40_000L) {
                            user.copy(status = "Offline")
                        } else user
                    }
                    state.copy(participants = refreshedParticipants)
                }
            }
        }
    }

    private fun monitorSessionDetails() {
        viewModelScope.launch {
            repository.getSessionDetails(sessionId)
                .collectLatest { dataMap ->
                    if (dataMap.isEmpty()) return@collectLatest

                    val fullSession = dataMap.toSessionData(sessionId)
                    val sLat = fullSession.startLat ?: 0.0
                    val sLng = fullSession.startLng ?: 0.0
                    val eLat = fullSession.endLat ?: 0.0
                    val eLng = fullSession.endLng ?: 0.0

                    val start = if (sLat != 0.0) Point.fromLngLat(sLng, sLat) else null
                    val end = if (eLat != 0.0) Point.fromLngLat(eLng, eLat) else null

                    // --- NEW: Fetch Route only if we haven't already ---
                    if (start != null && end != null && _routePoints.value.isEmpty()) {
                        viewModelScope.launch {
                            val token = com.example.multilink.BuildConfig.MAPBOX_ACCESS_TOKEN
                            val routeRepo = com.example.multilink.repo.RouteRepository(token)
                            val path = routeRepo.getRoute(start, end)
                            _routePoints.value = path.ifEmpty { listOf(start, end) }

                        }
                    }

                    if (fullSession.status == "Paused") {
                        participantsJob?.cancel(); participantsJob = null
                        viewModelScope.launch {
                            try {
                                val frozenUsers = repository.getSessionUsers(sessionId)
                                    .first()
                                checkAndFetchMissingProfiles(frozenUsers)
                                _uiState.update { it.copy(participants = frozenUsers) }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else if (fullSession.status == "Live") {
                        if (participantsJob == null || participantsJob?.isActive == false) {
                            participantsJob = viewModelScope.launch {
                                repository.getSessionUsers(sessionId)
                                    .collectLatest { users ->
                                        checkAndFetchMissingProfiles(users)
                                        val now = System.currentTimeMillis()
                                        val checkedUsers = users.map { user ->
                                            if (user.status != "Paused" && user.status != "Arrived" && user.lastUpdated > 0 && (now - user.lastUpdated) > 60_000L) {
                                                user.copy(status = "Offline")
                                            } else user
                                        }
                                        _uiState.update { it.copy(participants = checkedUsers) }
                                    }
                            }
                        }
                    }

                    _uiState.update { state ->
                        state.copy(
                            sessionData = fullSession, sessionTitle = fullSession.title,
                            hostId = fullSession.hostId,
                            isCurrentUserAdmin = (state.currentUserId == fullSession.hostId),
                            startPoint = start, endPoint = end, endLat = eLat, endLng = eLng,
                            startName = fullSession.fromLocation, endName = fullSession.toLocation,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun monitorSessionStatus() {
        viewModelScope.launch {
            repository.checkSessionActive(sessionId)
                .collectLatest { isActive ->
                    _uiState.update { it.copy(isSessionActive = isActive) }
                }
        }
    }

    private fun monitorSelfRemoval() {
        viewModelScope.launch {
            repository.listenForRemoval(sessionId)
                .collectLatest { removed ->
                    if (removed && _uiState.value.hostId.isNotEmpty() && _uiState.value.currentUserId != _uiState.value.hostId) {
                        _uiState.update { it.copy(isRemoved = true) }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isWatchingSession) repository.decrementSessionWatchers(sessionId)
    }
}

class SessionViewModelFactory(private val sessionId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(sessionId, RealtimeRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}