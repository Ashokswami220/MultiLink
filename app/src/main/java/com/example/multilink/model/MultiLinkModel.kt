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
        startLat = this["startLat"] as? Double,
        startLng = this["startLng"] as? Double,
        endLat = this["endLat"] as? Double,
        endLng = this["endLng"] as? Double,
        status = this["status"] as? String ?: "Live",
        createdTimestamp = this["created"] as? Long ?: 0L,
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
    val id: String,
    val title: String,
    val completedDate: String,
    val duration: String,
    val participants: String,
    val startLoc: String,
    val endLoc: String,
    val totalDistance: String,
)