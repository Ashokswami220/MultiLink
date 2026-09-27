package com.example.multilink.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.multilink.repo.RealtimeRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = RealtimeRepository()

    // ⭐ 1. Automatically saves the new token if the device generates one
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token Generated: $token")
        serviceScope.launch {
            repository.saveFcmToken(token)
        }
    }

    // ⭐ 2. This triggers even if the app is completely closed (for Data Messages)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Check if this is our custom Wake-Up ping
        if (message.data["action"] == "WAKE_UP_GPS") {
            val sessionId = message.data["sessionId"] ?: return
            Log.d("FCM", "Wake up ping received for session: $sessionId")
            wakeUpAndSendLocation(sessionId)
        }
    }

    @SuppressLint("MissingPermission")
    private fun wakeUpAndSendLocation(sessionId: String) {
        // Double check permissions just in case
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationClient = LocationServices.getFusedLocationProviderClient(this)

        // ⭐ Use getCurrentLocation for a single, highly-accurate fresh fix (bypasses cache)
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("FCM", "Successfully grabbed background location!")

                    // Get Battery info to pass along
                    val batteryStatus: Intent? =
                        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val level: Int =
                        batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale: Int =
                        batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val batteryPct =
                        if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100
                    val status: Int =
                        batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val isCharging =
                        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                    // Upload directly to Firebase
                    serviceScope.launch {
                        repository.updateMyLocation(
                            sessionId = sessionId,
                            lat = location.latitude,
                            lng = location.longitude,
                            heading = location.bearing,
                            battery = batteryPct,
                            isCharging = isCharging,
                            speed = location.speed
                        )
                        // Make sure their status is forced to Online
                        repository.updateUserStatus(sessionId, "Online")
                    }
                } else {
                    Log.e("FCM", "Background location was null")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Failed to get background location", e)
            }
    }
}