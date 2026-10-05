package com.example.multilink.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multilink.model.SessionData
import com.example.multilink.repo.RealtimeRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiEvent {
    data class ShowToast(val message: String) : HomeUiEvent()
    data class StartTrackingService(val sessionId: String) : HomeUiEvent()
    data class StopTrackingService(val isRemoval: Boolean) : HomeUiEvent()
    data class ShowTooFarDialog(val distanceMeters: Int) : HomeUiEvent()
    data class ShowArrivalConfirmDialog(val session: SessionData, val isArriving: Boolean) :
        HomeUiEvent()
}

class HomeViewModel : ViewModel() {
    private val repository = RealtimeRepository()
    private val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    private val _sortOption = MutableStateFlow("Newest")
    val sortOption = _sortOption.asStateFlow()

    private val _uiEvents = MutableSharedFlow<HomeUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var hasCheckedRestart = false

    fun updateSortOption(option: String) {
        _sortOption.value = option
    }

    fun getSortedSessions(sessions: List<SessionData>): List<SessionData> {
        return when (_sortOption.value) {
            "Newest" -> sessions.sortedByDescending { it.createdTimestamp }
            "Oldest" -> sessions.sortedBy { it.createdTimestamp }
            "A-Z" -> sessions.sortedBy { it.title.lowercase() }
            "Z-A" -> sessions.sortedByDescending { it.title.lowercase() }
            else -> sessions.reversed()
        }
    }

    fun checkAppRestartTracking(sessions: List<SessionData>) {
        if (hasCheckedRestart || currentUserId.isEmpty() || sessions.isEmpty()) return

        hasCheckedRestart = true

        viewModelScope.launch {
            sessions.forEach { session ->
                if (session.status != "Ended") {
                    if (session.hostId == currentUserId) {
                        _uiEvents.emit(HomeUiEvent.StartTrackingService(session.id))
                    } else if (session.status != "Paused") {
                        val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("sessions/${session.id}/users/$currentUserId/hasArrived")

                        userRef.get()
                            .addOnSuccessListener { snapshot ->
                                val hasArrived = snapshot.getValue(Boolean::class.java) ?: false
                                if (!hasArrived) {
                                    viewModelScope.launch {
                                        _uiEvents.emit(HomeUiEvent.StartTrackingService(session.id))
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    fun verifyTrackingActive(session: SessionData) {
        if (currentUserId.isEmpty() || session.status == "Ended") return

        viewModelScope.launch {
            if (session.hostId == currentUserId) {
                _uiEvents.emit(HomeUiEvent.StartTrackingService(session.id))
            } else if (session.status != "Paused") {
                val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("sessions/${session.id}/users/$currentUserId/hasArrived")

                userRef.get()
                    .addOnSuccessListener { snapshot ->
                        val hasArrived = snapshot.getValue(Boolean::class.java) ?: false
                        if (!hasArrived) {
                            viewModelScope.launch {
                                _uiEvents.emit(HomeUiEvent.StartTrackingService(session.id))
                            }
                        }
                    }
            }
        }
    }

    fun attemptArrival(session: SessionData, currentLoc: Location?, isArriving: Boolean) =
        viewModelScope.launch {
            if (!isArriving) {
                _uiEvents.emit(HomeUiEvent.ShowArrivalConfirmDialog(session, false))
                return@launch
            }

            val destLat = session.endLat
            val destLng = session.endLng

            if (currentLoc != null && destLat != null && destLng != null && destLat != 0.0) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    currentLoc.latitude, currentLoc.longitude, destLat, destLng, results
                )
                val dist = results[0]

                if (dist > 200f) {
                    _uiEvents.emit(HomeUiEvent.ShowTooFarDialog(dist.toInt()))
                } else {
                    _uiEvents.emit(HomeUiEvent.ShowArrivalConfirmDialog(session, true))
                }
            } else {
                _uiEvents.emit(HomeUiEvent.ShowToast("Waiting for GPS or Destination..."))
            }
        }

    fun confirmArrival(session: SessionData, isArriving: Boolean) = viewModelScope.launch {
        repository.toggleUserArrivedStatus(session.id, currentUserId, isArriving)
        _uiEvents.emit(
            HomeUiEvent.ShowToast(if (isArriving) "Marked as Arrived!" else "Arrival Undone")
        )
        if (!isArriving) {
            _uiEvents.emit(HomeUiEvent.StartTrackingService(session.id))
        }
    }

    fun createSession(session: SessionData, isSharing: Boolean) = viewModelScope.launch {
        _uiEvents.emit(HomeUiEvent.ShowToast("Creating session..."))
        val sessionId = repository.createSession(session, isSharing)
        if (sessionId != null && isSharing) {
            _uiEvents.emit(HomeUiEvent.StartTrackingService(sessionId))
        }
    }

    fun joinSession(realSessionId: String) = viewModelScope.launch {
        _uiEvents.emit(HomeUiEvent.ShowToast("Joining session..."))
        val success = repository.joinSession(realSessionId)
        if (success) {
            _uiEvents.emit(HomeUiEvent.StartTrackingService(realSessionId))
            _uiEvents.emit(HomeUiEvent.ShowToast("Joined Successfully"))
        } else {
            _uiEvents.emit(HomeUiEvent.ShowToast("Failed to join"))
        }
    }

    fun stopSession(sessionId: String) = viewModelScope.launch {
        repository.stopSession(sessionId)
        _uiEvents.emit(HomeUiEvent.StopTrackingService(isRemoval = true))
        _uiEvents.emit(HomeUiEvent.ShowToast("Session Stopped"))
    }

    fun leaveSession(sessionId: String) = viewModelScope.launch {
        repository.leaveSession(sessionId)
        _uiEvents.emit(HomeUiEvent.StopTrackingService(isRemoval = true))
        _uiEvents.emit(HomeUiEvent.ShowToast("Left Session"))
    }

    fun pauseSession(sessionId: String) = viewModelScope.launch {
        repository.updateSessionStatus(sessionId, isPaused = true)
    }

    fun resumeSession(sessionId: String) = viewModelScope.launch {
        repository.updateSessionStatus(sessionId, isPaused = false)
    }

    fun editSession(existingSession: SessionData, updatedSession: SessionData, isSharing: Boolean) =
        viewModelScope.launch {
            val finalSession = updatedSession.copy(
                id = existingSession.id, hostId = existingSession.hostId,
                status = existingSession.status, isHostSharing = isSharing
            )
            repository.updateSession(finalSession)

            if (isSharing) {
                _uiEvents.emit(HomeUiEvent.StartTrackingService(finalSession.id))
            } else {
                _uiEvents.emit(HomeUiEvent.StopTrackingService(isRemoval = true))
            }
            _uiEvents.emit(HomeUiEvent.ShowToast("Session Updated"))
        }
}