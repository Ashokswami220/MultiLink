package com.example.multilink.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.multilink.repo.RealtimeRepository
import com.example.multilink.model.MultiLinkUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*


@OptIn(ExperimentalCoroutinesApi::class)
class MultiLinkViewModel(application: Application) : AndroidViewModel(application) {

    // --- REPOSITORIES ---
    private val repository = RealtimeRepository()
    private val auth = FirebaseAuth.getInstance()


    private val currentUserIdFlow = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()


    val uiState: StateFlow<MultiLinkUiState> = currentUserIdFlow
        .flatMapLatest { userId ->
            if (userId != null) {
                repository.getMySessions()
                    .map { sessions ->
                        MultiLinkUiState(
                            sessions = sessions,
                            activeSessions = sessions.filter { it.status == "Live" },
                            isLoading = false
                        )
                    }
                    .onStart { emit(MultiLinkUiState(isLoading = true)) }
            } else {
                flowOf(MultiLinkUiState(isLoading = false))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MultiLinkUiState(isLoading = true)
        )


    fun signInWithGoogle(idToken: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .await()

                onResult(null)

            } catch (e: Exception) {
                Log.e("MultiLinkAuth", "Login Failed", e)
                auth.signOut()
                onResult(e.message ?: "Unknown Error")
            }
        }
    }
}