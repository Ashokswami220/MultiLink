package com.example.multilink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multilink.model.SessionParticipant
import com.example.multilink.model.toSessionData
import com.example.multilink.repo.RealtimeRepository
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.multilink.model.SessionData

data class SessionUiState(
    val isLoading: Boolean = true,
    val sessionTitle: String = "Loading...",
    val isSessionActive: Boolean = true,
    val isRemoved: Boolean = false,
    val hostId: String = "",
    val currentUserId: String = "",
    val participants: List<SessionParticipant> = emptyList(),
    val startPoint: Point? = null,
    val endPoint: Point? = null,
    val startName: String = "Start",
    val endName: String = "End",
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val isCurrentUserAdmin: Boolean = false,
    val sessionData: SessionData? = null
)

class SessionViewModel(
    private val sessionId: String,
    private val repository: RealtimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    var isManualRemoval = false

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        _uiState.update { it.copy(currentUserId = currentUser) }

        monitorSessionDetails()
        monitorParticipants()
        monitorSessionStatus()
        monitorSelfRemoval()
    }

    private fun monitorSessionDetails() {
        viewModelScope.launch {
            repository.getSessionDetails(sessionId)
                .collectLatest { dataMap ->

                    val fullSession = dataMap.toSessionData(sessionId)

                    val sLat = fullSession.startLat ?: 0.0
                    val sLng = fullSession.startLng ?: 0.0
                    val eLat = fullSession.endLat ?: 0.0
                    val eLng = fullSession.endLng ?: 0.0

                    val start = if (sLat != 0.0) Point.fromLngLat(sLng, sLat) else null
                    val end = if (eLat != 0.0) Point.fromLngLat(eLng, eLat) else null

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

    private fun monitorParticipants() {
        viewModelScope.launch {
            repository.getSessionUsers(sessionId)
                .collectLatest { users ->
                    _uiState.update { it.copy(participants = users) }
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
                    // Only update if I am NOT the admin (Host can't be removed logic)
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