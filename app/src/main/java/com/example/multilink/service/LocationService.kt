package com.example.multilink.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.multilink.MainActivity
import com.example.multilink.R
import com.example.multilink.repo.RealtimeRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val repository = RealtimeRepository()
    private val auth = FirebaseAuth.getInstance()

    private var currentSessionId: String? = null

    @Volatile
    private var isServiceActive = false

    @Volatile
    private var shouldUpdateStatusOnStop = true

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"

        // Extra to tell service if this is a permanent removal
        const val EXTRA_STOP_MODE = "EXTRA_STOP_MODE"
        const val MODE_REMOVE = "REMOVE"
        const val NOTIFICATION_CHANNEL_ID = "location_channel"
        const val NOTIFICATION_ID = 1

        // Expose location to UI directly (for "My Location" blue dot)
        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        // Define what happens when we get a new GPS fix
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // 1. Update local StateFlow (for UI)
                    _currentLocation.value = location

                    currentSessionId?.let { sessionId ->
                        if (isServiceActive) {
                            uploadLocationToFirebase(sessionId, location)
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (sessionId != null) {
                    currentSessionId = sessionId
                    isServiceActive = true
                    shouldUpdateStatusOnStop = true

                    startForegroundService()
                    startLocationUpdates()

                    serviceScope.launch {
                        repository.updateUserStatus(sessionId, "Online")
                        repository.setupDisconnectHandler(sessionId)

                        repository.listenForRemoval(sessionId)
                            .collectLatest { isRemoved ->
                                if (isRemoved) {
                                    isServiceActive = false
                                    shouldUpdateStatusOnStop = false
                                    stopLocationUpdates()
                                    stopSelf()
                                }
                            }
                    }
                } else {
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                val mode = intent.getStringExtra(EXTRA_STOP_MODE)
                if (mode == MODE_REMOVE) {
                    shouldUpdateStatusOnStop = false
                }

                isServiceActive = false

                if (shouldUpdateStatusOnStop) {
                    currentSessionId?.let { sid ->
                        serviceScope.launch {
                            repository.updateUserStatus(sid, "Offline")
                        }
                    }
                }
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Update every 5s
                .setMinUpdateDistanceMeters(5f) // Or every 5 meters
                .build()

        locationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
        cancel()
    }

    private fun uploadLocationToFirebase(sessionId: String, location: Location) {
        val user = auth.currentUser ?: return

        val batteryStatus: Intent? =
            registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct =
            if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        serviceScope.launch {
            if (isServiceActive) {
                repository.updateMyLocation(
                    sessionId = sessionId,
                    lat = location.latitude,
                    lng = location.longitude,
                    heading = location.bearing,
                    battery = batteryPct,
                    isCharging = isCharging,
                    speed = location.speed
                )
            }
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Live Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Clicking notification opens the app
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop Action Button
        val stopIntent = Intent(this, LocationService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MultiLink Active")
            .setContentText("Sharing your live location...")
            .setSmallIcon(R.mipmap.ic_launcher) // Make sure this icon exists
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        isServiceActive = false

        if (shouldUpdateStatusOnStop) {
            currentSessionId?.let { sid ->
                serviceScope.launch { repository.updateUserStatus(sid, "Offline") }
            }
        }

        super.onDestroy()
        serviceScope.cancel()
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun cancel() {
        if (serviceScope.isActive) {
            serviceScope.cancel()
        }
    }
}