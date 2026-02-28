package com.example.multilink.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multilink.model.ActivityFeedItem
import com.example.multilink.model.UserStats
import com.example.multilink.repo.RealtimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ActivityViewModel : ViewModel() {
    private val repository = RealtimeRepository()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _activityFeed = MutableStateFlow<List<ActivityFeedItem>>(emptyList())
    val activityFeed: StateFlow<List<ActivityFeedItem>> = _activityFeed.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            repository.listenToUserStats()
                .catch { e -> Log.e("ActivityVM", "Stats Error (Check Firebase Rules): ", e) }
                .collect { stats ->
                    _userStats.value = stats
                }
        }

        viewModelScope.launch {
            repository.listenToActivityFeed()
                .catch { e -> Log.e("ActivityVM", "Feed Error (Check Firebase Rules): ", e) }
                .collect { feed ->
                    _activityFeed.value = feed
                }
        }
    }

    fun markAsRead(itemId: String) {
        viewModelScope.launch {
            repository.markFeedItemRead(itemId)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteFeedItem(itemId)
        }
    }

    fun acceptInvite(sessionId: String, itemId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.joinSession(sessionId)
            if (success) {
                repository.deleteFeedItem(itemId)
            }
            onResult(success)
        }
    }
}