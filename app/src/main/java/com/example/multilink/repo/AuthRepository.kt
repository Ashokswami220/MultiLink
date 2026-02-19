package com.example.multilink.repo

import com.example.multilink.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Points to "users" node
    private val db = FirebaseDatabase.getInstance()
        .getReference("users")

    suspend fun checkUserExists(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val snapshot = db.child(uid)
                .child("profile")
                .get()
                .await()

            if (snapshot.exists()) {
                val phone = snapshot.child("phoneNumber")
                    .getValue(String::class.java)
                val name = snapshot.child("name")
                    .getValue(String::class.java)
                !phone.isNullOrBlank() && !name.isNullOrBlank()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveUserProfile(name: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        val photoUrl = auth.currentUser?.photoUrl?.toString() ?: ""

        val userProfile = UserProfile(
            id = uid,
            name = name,
            phoneNumber = phone,
            email = email,
            photoUrl = photoUrl,
            createdAt = System.currentTimeMillis()
        )

        // Save to users/{uid}/profile
        db.child(uid)
            .child("profile")
            .setValue(userProfile)
            .await()
    }
}