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
import kotlinx.coroutines.delay

class SessionViewModel(
    private val sessionId: String,
    private val repository: RealtimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<RecentSession>>(emptyList())
    val recentSessions: StateFlow<List<RecentSession>> = _recentSessions.asStateFlow()

    private var participantsJob: kotlinx.coroutines.Job? = null

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

    private fun fetchRecentSessions() {
        viewModelScope.launch {
            repository.getRecentSessions()
                .collectLatest { sessions ->
                    _recentSessions.value = sessions
                }
        }
    }

    fun deleteRecentSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteRecentSession(sessionId)
        }
    }

    private fun startOfflineTicker() {
        viewModelScope.launch {
            while (true) {
                delay(5000)

                val now = System.currentTimeMillis()
                _uiState.update { state ->
                    val refreshedParticipants = state.participants.map { user ->
                        if (user.status != "Paused" && user.lastUpdated > 0 && (now - user.lastUpdated) > 40_000L) {
                            user.copy(status = "Offline")
                        } else {
                            user
                        }
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

                    if (fullSession.status == "Paused") {
                        participantsJob?.cancel()
                        participantsJob = null

                        viewModelScope.launch {
                            try {
                                val frozenUsers = repository.getSessionUsers(sessionId)
                                    .first()
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
                                        val now = System.currentTimeMillis()
                                        val checkedUsers = users.map { user ->
                                            if (user.status != "Paused" && user.lastUpdated > 0 && (now - user.lastUpdated) > 60_000L) {
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
                            sessionData = fullSession,
                            sessionTitle = fullSession.title,
                            hostId = fullSession.hostId,
                            isCurrentUserAdmin = (state.currentUserId == fullSession.hostId),
                            startPoint = start,
                            endPoint = end,
                            endLat = eLat,
                            endLng = eLng,
                            startName = fullSession.fromLocation,
                            endName = fullSession.toLocation,
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