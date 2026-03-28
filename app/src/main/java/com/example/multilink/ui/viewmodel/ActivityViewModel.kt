package com.example.multilink.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multilink.model.ActivityFeedItem
import com.example.multilink.model.RecentSession
import com.example.multilink.model.UserStats
import com.example.multilink.repo.RealtimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

class ActivityViewModel : ViewModel() {
    private val repository = RealtimeRepository()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _activityFeed = MutableStateFlow<List<ActivityFeedItem>>(emptyList())
    val activityFeed: StateFlow<List<ActivityFeedItem>> = _activityFeed.asStateFlow()

    // 3 Separate Datasets for the Interactive Graph!
    private val _sessionsGraph = MutableStateFlow<List<Float>>(List(7) { 0f })
    val sessionsGraph: StateFlow<List<Float>> = _sessionsGraph.asStateFlow()

    private val _distanceGraph = MutableStateFlow<List<Float>>(List(7) { 0f })
    val distanceGraph: StateFlow<List<Float>> = _distanceGraph.asStateFlow()

    private val _timeGraph = MutableStateFlow<List<Float>>(List(7) { 0f })
    val timeGraph: StateFlow<List<Float>> = _timeGraph.asStateFlow()

    private val _graphLabels = MutableStateFlow<List<String>>(List(7) { "" })
    val graphLabels: StateFlow<List<String>> = _graphLabels.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            repository.listenToUserStats()
                .catch { e -> Log.e("ActivityVM", "Stats Error: ", e) }
                .collect { _userStats.value = it }
        }

        viewModelScope.launch {
            repository.listenToActivityFeed()
                .catch { e -> Log.e("ActivityVM", "Feed Error: ", e) }
                .collect { _activityFeed.value = it }
        }

        viewModelScope.launch {
            repository.getRecentSessions()
                .catch { e -> Log.e("ActivityVM", "Recent Sessions Error: ", e) }
                .collect { sessions ->
                    processGraphData(sessions)
                }
        }
    }

    private fun processGraphData(sessions: List<RecentSession>) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMidnight = calendar.timeInMillis

        val dayInMillis = 24 * 60 * 60 * 1000L

        val sessionData = FloatArray(7) { 0f }
        val distData = FloatArray(7) { 0f }
        val timeData = FloatArray(7) { 0f }
        val labels = Array(7) { "" }

        val labelFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())

        for (i in 6 downTo 0) {
            val dayStart = todayMidnight - (i * dayInMillis)
            val dayEnd = dayStart + dayInMillis

            val daySessions = sessions.filter { it.completedTimestamp in dayStart until dayEnd }

            val arrayIndex = 6 - i
            sessionData[arrayIndex] = daySessions.size.toFloat()
            distData[arrayIndex] = daySessions.sumOf { parseDistance(it.totalDistance) }
                .toFloat()
            timeData[arrayIndex] = daySessions.sumOf { parseDuration(it.duration) }
                .toFloat()

            val rawLabel = if (i == 0) "Today" else labelFormat.format(java.util.Date(dayStart))
            labels[arrayIndex] = if (rawLabel == "Today") "Now" else rawLabel.take(1)
        }

        _sessionsGraph.value = sessionData.toList()
        _distanceGraph.value = distData.toList()
        _timeGraph.value = timeData.toList()
        _graphLabels.value = labels.toList()
    }

    // Safely parse "5.2 km" or "800 m" into KM floats
    private fun parseDistance(dist: String): Double {
        try {
            val num = dist.replace("[^0-9.]".toRegex(), "")
                .toDoubleOrNull() ?: 0.0
            return if (dist.contains("km", true)) num else num / 1000.0
        } catch (e: Exception) {
            return 0.0
        }
    }

    // Safely parse "1h 30m" or "45m" into Minutes floats
    private fun parseDuration(dur: String): Double {
        try {
            var mins = 0.0
            if (dur.contains("h")) {
                val parts = dur.split("h")
                mins += (parts[0].trim()
                    .toDoubleOrNull() ?: 0.0) * 60
                if (parts.size > 1 && parts[1].contains("m")) {
                    mins += parts[1].replace("m", "")
                        .trim()
                        .toDoubleOrNull() ?: 0.0
                }
            } else if (dur.contains("m")) {
                mins += dur.replace("m", "")
                    .trim()
                    .toDoubleOrNull() ?: 0.0
            }
            return mins
        } catch (e: Exception) {
            return 0.0
        }
    }

    fun markAsRead(itemId: String) = viewModelScope.launch { repository.markFeedItemRead(itemId) }
    fun deleteItem(itemId: String) = viewModelScope.launch { repository.deleteFeedItem(itemId) }
    fun acceptInvite(sessionId: String, itemId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.joinSession(sessionId)
            if (success) repository.deleteFeedItem(itemId)
            onResult(success)
        }
    }
}