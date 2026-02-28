package com.example.multilink.repo

import android.util.Log
import com.example.multilink.model.ActivityFeedItem
import com.example.multilink.model.RecentSession
import com.example.multilink.model.SessionData
import com.example.multilink.model.SessionParticipant
import com.example.multilink.model.UserStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import com.example.multilink.model.toSessionData
import com.example.multilink.utils.LocationUtils.calculateDistance

class RealtimeRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()


    fun listenToSessionStatus(sessionId: String): Flow<String> = callbackFlow {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(String::class.java) ?: "Live")
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getGlobalUserProfile(userId: String): Map<String, String>? {
        return try {
            // 1. Check "users" node first (Custom Profile)
            val snapshot = db.child("users")
                .child(userId)
                .child("profile")
                .get()
                .await()

            if (snapshot.exists()) {
                val dbName = snapshot.child("name").value as? String ?: "Unknown"
                var dbPhotoUrl = snapshot.child("photoUrl").value as? String ?: ""

                if (dbPhotoUrl.isEmpty()) {
                    val encodedName = URLEncoder.encode(dbName, "UTF-8")
                    dbPhotoUrl =
                        "https://ui-avatars.com/api/?name=$encodedName&background=random&color=fff&size=256"
                }

                mapOf(
                    "name" to dbName,
                    "phone" to (snapshot.child("phoneNumber").value as? String ?: ""),
                    "email" to (snapshot.child("email").value as? String ?: ""),
                    "photoUrl" to dbPhotoUrl
                )
            } else {
                val user = auth.currentUser
                if (user != null && user.uid == userId) {
                    mapOf(
                        "name" to (user.displayName ?: "Unknown"),
                        "phone" to (user.phoneNumber ?: ""),
                        "email" to (user.email ?: ""),
                        "photoUrl" to (user.photoUrl?.toString() ?: "")
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }


    // --- ADAPTIVE TRACKING (WATCHER SYSTEM) ---
    private fun adjustWatcherCount(
        ref: com.google.firebase.database.DatabaseReference, delta: Int
    ) {
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var count = currentData.getValue(Int::class.java) ?: 0
                count += delta
                if (count < 0) count = 0 // Prevent negative counts
                currentData.value = count
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?
            ) {
                if (error != null) Log.e(
                    "RealtimeRepo", "Watcher Transaction failed", error.toException()
                )
            }
        })
    }

    fun incrementSessionWatchers(sessionId: String) {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("sessionWatchers")
        adjustWatcherCount(ref, 1)
    }

    fun decrementSessionWatchers(sessionId: String) {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("sessionWatchers")
        adjustWatcherCount(ref, -1)
    }

    fun incrementUserWatchers(sessionId: String, targetUserId: String) {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("users")
            .child(targetUserId)
            .child("userWatchers")
        adjustWatcherCount(ref, 1)
    }

    fun decrementUserWatchers(sessionId: String, targetUserId: String) {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("users")
            .child(targetUserId)
            .child("userWatchers")
        adjustWatcherCount(ref, -1)
    }

    fun listenToSessionWatchers(sessionId: String): Flow<Int> = callbackFlow {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("sessionWatchers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Int::class.java) ?: 0)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun listenToUserWatchers(sessionId: String, userId: String): Flow<Int> = callbackFlow {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("users")
            .child(userId)
            .child("userWatchers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Int::class.java) ?: 0)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }


    // --- 2. ZOMBIE FIX: LISTEN FOR KICK ---
    fun listenForRemoval(sessionId: String): Flow<Boolean> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(true) // No user = Removed
            close()
            return@callbackFlow
        }

        val ref = db.child("sessions")
            .child(sessionId)
            .child("users")
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // If it doesn't exist, OR if it lost its ID (became a zombie write)
                if (!snapshot.exists() || !snapshot.child("id")
                        .exists()
                ) {
                    trySend(true)
                } else {
                    trySend(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun removeUser(sessionId: String, targetUserId: String) {
        if (targetUserId.isBlank()) return
        try {
            val snapshot = db.child("sessions")
                .child(sessionId)
                .get()
                .await()
            val sessionDataMap = snapshot.value as? Map<String, Any>
            if (sessionDataMap != null) {
                val sessionData = sessionDataMap.toSessionData(sessionId)
                val pCount = snapshot.child("users").childrenCount.toInt()
                archiveSessionForUser(targetUserId, sessionData, pCount, "Removed by Admin")

                sendFeedItem(
                    targetUserId,
                    type = "alert",
                    title = "Removed from Session",
                    message = "You were removed from '${sessionData.title}' by the host.",
                    sessionId = sessionId
                )
            }

            db.child("sessions")
                .child(sessionId)
                .child("users")
                .child(targetUserId)
                .removeValue()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun setupDisconnectHandler(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.child("sessions")
            .child(sessionId)
            .child("users")
            .child(userId)

        try {
            // Queue these operations on the server
            userRef.child("status")
                .onDisconnect()
                .setValue("Offline")
                .await()
            // Also update the timestamp so we know WHEN they went offline
            userRef.child("lastUpdated")
                .onDisconnect()
                .setValue(ServerValue.TIMESTAMP)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUserStatus(sessionId: String, status: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "status" to status,
            "lastUpdated" to System.currentTimeMillis()
        )
        try {
            db.child("sessions")
                .child(sessionId)
                .child("users")
                .child(userId)
                .updateChildren(updates)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUserStatusForAdmin(sessionId: String, targetUserId: String, status: String) {
        val updates = mapOf(
            "status" to status,
            "lastUpdated" to System.currentTimeMillis()
        )
        try {
            db.child("sessions")
                .child(sessionId)
                .child("users")
                .child(targetUserId)
                .updateChildren(updates)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 1. SESSION MANAGEMENT ---
    private suspend fun generateUniqueJoinCode(): String {
        val chars = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"
        var code = ""
        var isUnique = false
        var attempts = 0

        while (!isUnique && attempts < 5) {
            code = (1..8).map { chars.random() }
                .joinToString("")
            try {
                val result =
                    db.child("sessions")
                        .orderByChild("joinCode")
                        .equalTo(code)
                        .get()
                        .await()
                if (!result.exists()) isUnique = true
            } catch (e: Exception) {
                e.printStackTrace(); break
            }
            attempts++
        }
        return if (isUnique) code else System.currentTimeMillis()
            .toString()
            .takeLast(8)
    }

    suspend fun createSession(session: SessionData, isHostSharing: Boolean): String? {
        val userId = auth.currentUser?.uid ?: return null
        val sessionId = db.child("sessions")
            .push().key ?: return null
        val shortCode = generateUniqueJoinCode()

        val sessionData = mapOf(
            "title" to session.title,
            "hostId" to userId,
            "hostName" to (auth.currentUser?.displayName ?: "Unknown Host"),
            "joinCode" to shortCode,
            "fromLocation" to session.fromLocation,
            "toLocation" to session.toLocation,
            "startLat" to (session.startLat ?: 0.0),
            "startLng" to (session.startLng ?: 0.0),
            "endLat" to (session.endLat ?: 0.0),
            "endLng" to (session.endLng ?: 0.0),
            "isActive" to true,
            "status" to "Live",
            "isHostSharing" to isHostSharing,
            "isUsersVisible" to session.isUsersVisible,
            "created" to System.currentTimeMillis(),
            "durationVal" to session.durationVal,
            "durationUnit" to session.durationUnit,
            "maxPeople" to session.maxPeople,
            "isSharingAllowed" to session.isSharingAllowed
        )

        return try {
            db.child("sessions")
                .child(sessionId)
                .setValue(sessionData)
                .await()
            if (isHostSharing) joinSession(sessionId)
            sessionId
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    suspend fun stopSession(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            val snapshot = db.child("sessions")
                .child(sessionId)
                .get()
                .await()
            val hostId = snapshot.child("hostId")
                .getValue(String::class.java)

            if (hostId == userId) {
                val sessionDataMap = snapshot.value as? Map<String, Any>

                if (sessionDataMap != null) {
                    val sessionData = sessionDataMap.toSessionData(sessionId)
                    val usersSnapshot = snapshot.child("users")
                    val pCount = usersSnapshot.childrenCount.toInt()

                    usersSnapshot.children.forEach { userSnap ->
                        val uId = userSnap.child("id")
                            .getValue(String::class.java)
                        if (!uId.isNullOrEmpty()) {
                            val reason =
                                if (uId == userId) "You ended the session" else "Ended by Admin"
                            archiveSessionForUser(uId, sessionData, pCount, reason)
                        }
                    }
                }
                // Permanently remove the session
                db.child("sessions")
                    .child(sessionId)
                    .removeValue()
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun checkSessionActive(sessionId: String): Flow<Boolean> = callbackFlow {
        val ref = db.child("sessions")
            .child(sessionId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getMySessions(): Flow<List<SessionData>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = db.child("sessions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = mutableListOf<SessionData>()
                for (child in snapshot.children) {
                    try {
                        val sessionId = child.key ?: continue

                        val created = child.child("created")
                            .getValue(Long::class.java) ?: 0L
                        val durationVal = child.child("durationVal")
                            .getValue(String::class.java) ?: "2"
                        val durationUnit = child.child("durationUnit")
                            .getValue(String::class.java) ?: "Hrs"

                        val durationMillis = if (durationUnit == "Hrs") {
                            TimeUnit.HOURS.toMillis(durationVal.toLongOrNull() ?: 2)
                        } else {
                            TimeUnit.DAYS.toMillis(durationVal.toLongOrNull() ?: 1)
                        }

                        val endTime = created + durationMillis
                        if (System.currentTimeMillis() > endTime) {
                            // Session Expired -> Delete from DB
                            db.child("sessions")
                                .child(sessionId)
                                .removeValue()
                            continue // Skip adding to list
                        }

                        val hostId = child.child("hostId")
                            .getValue(String::class.java) ?: ""
                        val isParticipant = child.child("users")
                            .hasChild(userId)
                        var userCount = 0
                        child.child("users").children.forEach { userSnapshot ->
                            if (userSnapshot.child("id")
                                    .exists()
                            ) {
                                userCount++
                            }
                        }

                        if (hostId == userId || isParticipant) {
                            val isActive =
                                child.child("isActive")
                                    .getValue(Boolean::class.java) ?: false
                            if (isActive) {
                                sessions.add(
                                    SessionData(
                                        id = child.key ?: "",
                                        title = child.child("title")
                                            .getValue(String::class.java) ?: "Unknown",
                                        fromLocation = child.child("fromLocation")
                                            .getValue(String::class.java) ?: "",
                                        toLocation = child.child("toLocation")
                                            .getValue(String::class.java) ?: "",
                                        startLat = child.child("startLat")
                                            .getValue(Double::class.java) ?: 0.0,
                                        startLng = child.child("startLng")
                                            .getValue(Double::class.java) ?: 0.0,
                                        endLat = child.child("endLat")
                                            .getValue(Double::class.java) ?: 0.0,
                                        endLng = child.child("endLng")
                                            .getValue(Double::class.java) ?: 0.0,
                                        durationVal = child.child("durationVal")
                                            .getValue(String::class.java) ?: "2",
                                        durationUnit = child.child("durationUnit")
                                            .getValue(String::class.java) ?: "Hrs",
                                        maxPeople = child.child("maxPeople")
                                            .getValue(String::class.java) ?: "10",
                                        status = child.child("status")
                                            .getValue(String::class.java) ?: "Live",
                                        joinCode = child.child("joinCode")
                                            .getValue(String::class.java) ?: "",
                                        hostName = child.child("hostName")
                                            .getValue(String::class.java) ?: "",
                                        hostId = hostId,
                                        createdTimestamp = child.child("created")
                                            .getValue(Long::class.java) ?: 0L,
                                        isUsersVisible = child.child("isUsersVisible")
                                            .getValue(Boolean::class.java) ?: true,
                                        isSharingAllowed = child.child("isSharingAllowed")
                                            .getValue(Boolean::class.java) ?: true,
                                        isHostSharing = child.child("isHostSharing")
                                            .getValue(Boolean::class.java) ?: true,
                                        activeUsers = userCount
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                trySend(sessions)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun joinSession(sessionId: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val snapshot = db.child("sessions")
                .child(sessionId)
                .get()
                .await()
            if (!snapshot.exists()) return false

            val joinData = mapOf(
                "id" to currentUser.uid,
                "name" to (currentUser.displayName ?: "User"),
                "status" to "Online",
                "lat" to 0.0,
                "lng" to 0.0,
                "heading" to 0f,
                "batteryLevel" to 100,
                "isCharging" to false,
                "lastUpdated" to System.currentTimeMillis()
            )

            db.child("sessions")
                .child(sessionId)
                .child("users")
                .child(currentUser.uid)
                .updateChildren(joinData)
                .await()
            incrementUserStats(
                currentUser.uid, distanceMeters = 0.0, timeSeconds = 0L, sessionDelta = 1
            )
            true
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }

    suspend fun updateSessionStatus(
        sessionId: String,
        isPaused: Boolean
    ) {
        val status = if (isPaused) "Paused" else "Live"
        db.child("sessions")
            .child(sessionId)
            .child("status")
            .setValue(status)
    }

    suspend fun updateSession(session: SessionData) {
        val updates = mapOf(
            "title" to session.title,
            "fromLocation" to session.fromLocation,
            "toLocation" to session.toLocation,
            "startLat" to (session.startLat ?: 0.0),
            "startLng" to (session.startLng ?: 0.0),
            "endLat" to (session.endLat ?: 0.0),
            "endLng" to (session.endLng ?: 0.0),
            "durationVal" to session.durationVal,
            "durationUnit" to session.durationUnit,
            "maxPeople" to session.maxPeople,
            "isUsersVisible" to session.isUsersVisible,
            "isSharingAllowed" to session.isSharingAllowed,
            "isHostSharing" to session.isHostSharing
        )
        db.child("sessions")
            .child(session.id)
            .updateChildren(updates)
            .await()

        val userId = auth.currentUser?.uid ?: return
        if (session.isHostSharing) joinSession(session.id)
        else db.child("sessions")
            .child(session.id)
            .child("users")
            .child(userId)
            .removeValue()
    }


    fun getSessionDetails(sessionId: String): Flow<Map<String, Any>> = callbackFlow {
        val ref = db.child("sessions")
            .child(sessionId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (snapshot.value as? Map<String, Any>)?.let { trySend(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    //  UPDATED: Returns Clean SessionParticipant List (Filters out ghost nodes)
    fun getSessionUsers(sessionId: String): Flow<List<SessionParticipant>> = callbackFlow {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("users")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<SessionParticipant>()
                for (child in snapshot.children) {
                    child.getValue(SessionParticipant::class.java)
                        ?.let { user ->
                            // FIXED: Ignore partial zombie nodes created by dying background services
                            if (user.id.isNotEmpty()) {
                                users.add(user)
                            }
                        }
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }


    // --- 3. LIVE TRACKING ---
    suspend fun updateMyLocation(
        sessionId: String,
        lat: Double,
        lng: Double,
        heading: Float,
        battery: Int,
        isCharging: Boolean,
        speed: Float
    ) {
        val userId = auth.currentUser?.uid ?: return
        try {
            val userRef = db.child("sessions")
                .child(sessionId)
                .child("users")
                .child(userId)
            val updates = mapOf(
                "lat" to lat, "lng" to lng, "heading" to heading,
                "batteryLevel" to battery, "isCharging" to isCharging,
                "speed" to speed,
                "lastUpdated" to System.currentTimeMillis()
            )
            userRef.updateChildren(updates)
                .await()
        } catch (e: Exception) {
            Log.e("Repo", "Loc error", e)
        }
    }

    suspend fun getSessionIdFromCode(shortCode: String): String? {
        return try {
            val query =
                db.child("sessions")
                    .orderByChild("joinCode")
                    .equalTo(shortCode)
                    .limitToFirst(1)
            val snapshot = query.get()
                .await()
            snapshot.children.firstOrNull()?.key
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }


    suspend fun leaveSession(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            val snapshot = db.child("sessions")
                .child(sessionId)
                .get()
                .await()
            val sessionDataMap = snapshot.value as? Map<String, Any>

            if (sessionDataMap != null) {
                val sessionData = sessionDataMap.toSessionData(sessionId)
                val pCount = snapshot.child("users").childrenCount.toInt()
                archiveSessionForUser(userId, sessionData, pCount, "You left the session")
            }

            db.child("sessions")
                .child(sessionId)
                .child("users")
                .child(userId)
                .removeValue()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cleans up accidental zombie nodes
    suspend fun deleteMyNode(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.child("sessions")
                .child(sessionId)
                .child("users")
                .child(userId)
                .removeValue()
                .await()
        } catch (_: Exception) {
        }
    }


    // --- RECENT SESSIONS (HISTORY) ---

    private suspend fun archiveSessionForUser(
        userId: String, sessionData: SessionData, participantsCount: Int, reason: String
    ) {
        val recentRef = db.child("users")
            .child(userId)
            .child("recent_sessions")
        val now = System.currentTimeMillis()

        // 1. Calculate Real Distance
        val distanceStr = try {
            calculateDistance(
                sessionData.startLat ?: 0.0, sessionData.startLng ?: 0.0,
                sessionData.endLat ?: 0.0, sessionData.endLng ?: 0.0
            )
        } catch (_: Exception) {
            "..."
        }
        val finalDistance = if (distanceStr == "...") "N/A" else distanceStr

        val rawDistanceMeters = try {
            val num = finalDistance.replace("[^0-9.]".toRegex(), "")
                .toDoubleOrNull() ?: 0.0
            if (finalDistance.contains("km", ignoreCase = true)) {
                num * 1000.0 // Convert km back to meters
            } else {
                num // Already in meters
            }
        } catch (e: Exception) {
            0.0
        }

        // 2. Calculate Real Duration
        val durationMs =
            if (sessionData.createdTimestamp > 0) now - sessionData.createdTimestamp else 0L
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val mins = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val durationStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val dateStr = java.text.SimpleDateFormat("dd MMM • h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(now))

        val hostProfile = getGlobalUserProfile(sessionData.hostId)
        val hostPhone = hostProfile?.get("phone")
            ?.takeIf { it.isNotEmpty() } ?: "Not Shared"
        val hostEmail = hostProfile?.get("email")
            ?.takeIf { it.isNotEmpty() } ?: "Not Shared"

        val recentSessionMap = mapOf(
            "id" to sessionData.id,
            "title" to sessionData.title,
            "completedDate" to dateStr,
            "completedTimestamp" to now,
            "duration" to durationStr,
            "participants" to "$participantsCount Users",
            "startLoc" to sessionData.fromLocation,
            "endLoc" to sessionData.toLocation,
            "totalDistance" to finalDistance,
            "hostName" to sessionData.hostName,
            "hostPhone" to hostPhone,
            "hostEmail" to hostEmail,
            "completionReason" to reason,
            "startLat" to sessionData.startLat,
            "startLng" to sessionData.startLng,
            "endLat" to sessionData.endLat,
            "endLng" to sessionData.endLng
        )

        try {
            recentRef.child(sessionData.id)
                .setValue(recentSessionMap)
                .await()

            val durationSeconds = durationMs / 1000
            incrementUserStats(userId, rawDistanceMeters, durationSeconds, 0)

            // Cleanup: Max 10 Sessions Rule
            val snapshot = recentRef.get()
                .await()
            val allSessions = snapshot.children.mapNotNull { child ->
                val map = child.value as? Map<*, *>
                if (map != null) {
                    val id = map["id"] as? String ?: ""
                    val ts = (map["completedTimestamp"] as? Number)?.toLong() ?: 0L
                    Pair(id, ts)
                } else null
            }
                .sortedByDescending { it.second }

            if (allSessions.size > 10) {
                for (i in 10 until allSessions.size) {
                    recentRef.child(allSessions[i].first)
                        .removeValue()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecentSessions(): Flow<List<RecentSession>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.child("users")
            .child(userId)
            .child("recent_sessions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = mutableListOf<RecentSession>()
                val now = System.currentTimeMillis()
                val tenDaysAgo = now - (10L * 24 * 60 * 60 * 1000)

                for (child in snapshot.children) {
                    try {
                        val map = child.value as? Map<*, *> ?: continue
                        val completedTimestamp =
                            (map["completedTimestamp"] as? Number)?.toLong() ?: 0L

                        if (completedTimestamp < tenDaysAgo) {
                            child.ref.removeValue() // Self-cleaning
                        } else {
                            sessions.add(
                                RecentSession(
                                    id = map["id"] as? String ?: "",
                                    title = map["title"] as? String ?: "Unnamed Session",
                                    completedDate = map["completedDate"] as? String ?: "",
                                    completedTimestamp = completedTimestamp,
                                    duration = map["duration"] as? String ?: "",
                                    participants = map["participants"] as? String ?: "",
                                    startLoc = map["startLoc"] as? String ?: "",
                                    endLoc = map["endLoc"] as? String ?: "",
                                    totalDistance = map["totalDistance"] as? String ?: "",
                                    hostName = map["hostName"] as? String ?: "",
                                    hostPhone = map["hostPhone"] as? String ?: "",
                                    hostEmail = map["hostEmail"] as? String ?: "",
                                    completionReason = map["completionReason"] as? String ?: "",
                                    startLat = (map["startLat"] as? Number)?.toDouble(),
                                    startLng = (map["startLng"] as? Number)?.toDouble(),
                                    endLat = (map["endLat"] as? Number)?.toDouble(),
                                    endLng = (map["endLng"] as? Number)?.toDouble()
                                )
                            )
                        }
                    } catch (_: Exception) {
                        // If a node is completely broken, ignore it and let the app live!
                        child.ref.removeValue()
                    }
                }
                trySend(sessions)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun deleteRecentSession(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.child("users")
                .child(userId)
                .child("recent_sessions")
                .child(sessionId)
                .removeValue()
                .await()
        } catch (_: Exception) {
        }
    }

    private fun incrementUserStats(
        userId: String, distanceMeters: Double, timeSeconds: Long, sessionDelta: Int
    ) {
        val statsRef = db.child("users")
            .child(userId)
            .child("stats")
        statsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentDistance = currentData.child("totalDistanceMeters")
                    .getValue(Double::class.java) ?: 0.0
                val currentTime = currentData.child("totalTimeSeconds")
                    .getValue(Long::class.java) ?: 0L
                val currentSessions = currentData.child("totalSessions")
                    .getValue(Int::class.java) ?: 0

                currentData.child("totalDistanceMeters").value = currentDistance + distanceMeters
                currentData.child("totalTimeSeconds").value = currentTime + timeSeconds
                currentData.child("totalSessions").value = currentSessions + sessionDelta

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?
            ) {
                if (error != null) Log.e(
                    "RealtimeRepo", "Stats Transaction failed", error.toException()
                )
            }
        })
    }

    fun listenToUserStats(): Flow<UserStats> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(UserStats())
            close()
            return@callbackFlow
        }

        val ref = db.child("users")
            .child(userId)
            .child("stats")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // ⭐ FIXED: Manual parsing prevents Firebase Reflection crashes
                    if (!snapshot.exists()) {
                        trySend(UserStats()) // Send 0s if it doesn't exist
                        return
                    }
                    val stats = UserStats(
                        totalDistanceMeters = (snapshot.child(
                            "totalDistanceMeters"
                        ).value as? Number)?.toDouble() ?: 0.0,
                        totalTimeSeconds = (snapshot.child(
                            "totalTimeSeconds"
                        ).value as? Number)?.toLong() ?: 0L,
                        totalSessions = (snapshot.child("totalSessions").value as? Number)?.toInt()
                            ?: 0
                    )
                    trySend(stats)
                } catch (e: Exception) {
                    e.printStackTrace()
                    trySend(UserStats()) // Fallback on error
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- ACTIVITY FEED INBOX ---
    fun listenToActivityFeed(): Flow<List<ActivityFeedItem>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.child("user_activity_feed")
            .child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val feed = mutableListOf<ActivityFeedItem>()
                    // ⭐ FIXED: Manual parsing handles missing or weird data safely
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val map = child.value as? Map<*, *> ?: continue
                            val item = ActivityFeedItem(
                                id = map["id"] as? String ?: "",
                                type = map["type"] as? String ?: "alert",
                                title = map["title"] as? String ?: "",
                                message = map["message"] as? String ?: "",
                                sessionId = map["sessionId"] as? String ?: "",
                                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L,
                                isRead = map["isRead"] as? Boolean ?: false
                            )
                            feed.add(item)
                        }
                    }
                    trySend(feed.sortedByDescending { it.timestamp })
                } catch (e: Exception) {
                    e.printStackTrace()
                    trySend(emptyList()) // Fallback to empty list on error
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendFeedItem(
        targetUserId: String, type: String, title: String, message: String, sessionId: String = ""
    ) {
        try {
            val feedRef = db.child("user_activity_feed")
                .child(targetUserId)
                .push()
            val itemId = feedRef.key ?: return

            val newItem = ActivityFeedItem(
                id = itemId,
                type = type,
                title = title,
                message = message,
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            feedRef.setValue(newItem)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markFeedItemRead(itemId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.child("user_activity_feed")
                .child(userId)
                .child(itemId)
                .child("isRead")
                .setValue(true)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteFeedItem(itemId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.child("user_activity_feed")
                .child(userId)
                .child(itemId)
                .removeValue()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}