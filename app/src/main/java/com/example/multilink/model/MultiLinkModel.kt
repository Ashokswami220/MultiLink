package com.example.multilink.model

import com.mapbox.geojson.Point

data class SessionData(
    val id: String = "",
    val title: String,
    val fromLocation: String,
    val toLocation: String,
    val durationVal: String,
    val durationUnit: String,
    val maxPeople: String,
    val status: String = "Live",
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val joinCode: String = "",
    val hostName: String = "",
    val hostId: String = "",
    val isUsersVisible: Boolean = true,
    val createdTimestamp: Long = 0L,
    val isSharingAllowed: Boolean = true,
    val isHostSharing: Boolean = true,
    val activeUsers: Int = 1
)

fun Map<String, Any>.toSessionData(sessionId: String): SessionData {
    return SessionData(
        id = sessionId,
        title = this["title"] as? String ?: "",
        hostId = this["hostId"] as? String ?: "",
        hostName = this["hostName"] as? String ?: "",
        joinCode = this["joinCode"] as? String ?: "",
        fromLocation = this["fromLocation"] as? String ?: "",
        toLocation = this["toLocation"] as? String ?: "",
        startLat = (this["startLat"] as? Number)?.toDouble(),
        startLng = (this["startLng"] as? Number)?.toDouble(),
        endLat = (this["endLat"] as? Number)?.toDouble(),
        endLng = (this["endLng"] as? Number)?.toDouble(),
        status = this["status"] as? String ?: "Live",
        createdTimestamp = (this["created"] as? Number)?.toLong() ?: 0L,
        durationVal = this["durationVal"] as? String ?: "2",
        durationUnit = this["durationUnit"] as? String ?: "Hrs",
        isSharingAllowed = this["isSharingAllowed"] as? Boolean ?: true,
        isUsersVisible = this["isUsersVisible"] as? Boolean ?: true,
        isHostSharing = this["isHostSharing"] as? Boolean ?: true,
        maxPeople = this["maxPeople"] as? String ?: "10",
        activeUsers = (this["activeUsers"] as? Number)?.toInt() ?: 0
    )
}

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class SessionParticipant(
    val id: String = "",
    val name: String = "User",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val heading: Float = 0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val status: String = "Online",
    val lastUpdated: Long = 0L,
    val speed: Float = 0f
)

data class SearchResult(
    val name: String,
    val address: String,
    val point: Point,
    val mapboxId: String? = null,
)

data class MultiLinkUiState(
    val sessions: List<SessionData> = emptyList(),
    val activeSessions: List<SessionData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class RecentSession(
    val id: String = "",
    val title: String = "",
    val completedDate: String = "",
    val completedTimestamp: Long = 0L,
    val duration: String = "",
    val participants: String = "",
    val startLoc: String = "",
    val endLoc: String = "",
    val totalDistance: String = "",
    val hostName: String = "",
    val hostPhone: String = "",
    val hostEmail: String = "",
    val completionReason: String = "",
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null
)

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

// Represents the user's lifetime stats
data class UserStats(
    val totalDistanceMeters: Double = 0.0,
    val totalTimeSeconds: Long = 0L,
    val totalSessions: Int = 0
)

// Represents a single notification/invite in the feed
data class ActivityFeedItem(
    val id: String = "",
    val type: String = "alert", // Can be: "invite", "alert", or "info"
    val title: String = "",
    val message: String = "",
    val sessionId: String = "", // Crucial so we can click "Accept" and join!
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)