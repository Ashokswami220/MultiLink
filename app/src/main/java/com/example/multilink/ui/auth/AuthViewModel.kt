package com.example.multilink.ui.auth

import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multilink.repo.AuthRepository
import com.example.multilink.service.LocationService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Loading : AuthState
    object Authenticated : AuthState
    object NeedsProfile : AuthState
    object Unauthenticated : AuthState
}

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        _authState.value = AuthState.Loading
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                val hasProfile = authRepository.checkUserExists()
                if (hasProfile) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.NeedsProfile
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun logout(context: Context, onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                // 1. KILL THE SERVICE
                val stopIntent = Intent(context, LocationService::class.java)
                stopIntent.action = LocationService.ACTION_STOP
                context.startService(stopIntent)

                // 2. Sign out of Firebase
                auth.signOut()

                // 3. Sign out of Google using Credential Manager API
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _authState.value = AuthState.Unauthenticated
                onLoggedOut()
            }
        }
    }
}
