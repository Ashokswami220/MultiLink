package com.example.multilink.repo

import android.util.Log
import com.example.multilink.model.SessionData
import com.example.multilink.model.SessionParticipant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class RealtimeRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()


    suspend fun getGlobalUserProfile(userId: String): Map<String, String>? {
        return try {
            // 1. Check "users" node first (Custom Profile)
            val snapshot = db.child("users")
                .child(userId)
                .child("profile")
                .get()
                .await()

            if (snapshot.exists()) {
                mapOf(
                    "name" to (snapshot.child("name").value as? String ?: "Unknown"),
                    "phone" to (snapshot.child("phoneNumber").value as? String ?: ""),
                    "email" to (snapshot.child("email").value as? String ?: ""),
                    "photoUrl" to (snapshot.child("photoUrl").value as? String ?: "")
                )
            } else {
                // 2. Fallback to FirebaseAuth basic info if no DB profile
                // This ensures we at least get the Google Photo if available
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
                // If snapshot does NOT exist, it means we were deleted/kicked
                if (!snapshot.exists()) {
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
        try {
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
                        val userCount = child.child("users").childrenCount.toInt()

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

    // --- UPDATED JOIN LOGIC (CLEAN DATA) ---
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

    //  UPDATED: Returns Clean SessionParticipant List
    fun getSessionUsers(sessionId: String): Flow<List<SessionParticipant>> = callbackFlow {
        val ref = db.child("sessions")
            .child(sessionId)
            .child("users")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<SessionParticipant>()
                for (child in snapshot.children) {
                    child.getValue(SessionParticipant::class.java)
                        ?.let { users.add(it) }
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


    suspend fun leaveSession(
        sessionId: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        try {
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
}