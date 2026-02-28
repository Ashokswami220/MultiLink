package com.example.multilink.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
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

    @Volatile
    private var isSessionPaused = false

    @Volatile
    private var isCurrentlyHighAccuracy = true

    private var gpsReceiver: BroadcastReceiver? = null
    private val connectedRef = com.google.firebase.database.FirebaseDatabase.getInstance()
        .getReference(".info/connected")
    private var connectionListener: com.google.firebase.database.ValueEventListener? = null

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
                    _currentLocation.value = location
                    currentSessionId?.let { sessionId ->
                        if (isServiceActive) {
                            uploadLocationToFirebase(sessionId, location)
                        }
                    }
                }
            }
        }
        connectionListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected && isServiceActive && !isSessionPaused) {
                    // Internet is back! Update status to Online
                    currentSessionId?.let { sid ->
                        serviceScope.launch {
                            repository.updateUserStatus(sid, "Online")
                            // Must re-register disconnect handler every time we reconnect
                            repository.setupDisconnectHandler(sid)

                            // Force a quick ping since we were just offline
                            try {
                                locationClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY, null
                                )
                                    .addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            _currentLocation.value = loc
                                            uploadLocationToFirebase(sid, loc)
                                        }
                                    }
                            } catch (e: SecurityException) {
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        connectedRef.addValueEventListener(connectionListener!!)

        gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                    val locationManager = context.getSystemService(
                        Context.LOCATION_SERVICE
                    ) as android.location.LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(
                        android.location.LocationManager.GPS_PROVIDER
                    )

                    val newStatus = if (isGpsEnabled) "Online" else "Location Off"

                    currentSessionId?.let { sid ->
                        serviceScope.launch {
                            repository.updateUserStatus(sid, newStatus)
                        }
                    }

                    if (isGpsEnabled && isServiceActive && !isSessionPaused) {
                        try {
                            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { loc ->
                                    if (loc != null && currentSessionId != null) {
                                        _currentLocation.value = loc
                                        uploadLocationToFirebase(currentSessionId!!, loc)
                                    }
                                }
                        } catch (e: SecurityException) {
                        }
                    }
                }
            }
        }
        registerReceiver(
            gpsReceiver, IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        )
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
                    requestLocationUpdates(isCurrentlyHighAccuracy)

                    try {
                        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { location ->
                                if (location != null && isServiceActive) {
                                    _currentLocation.value = location
                                    uploadLocationToFirebase(sessionId, location)
                                }
                            }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }

                    serviceScope.launch {
                        repository.updateUserStatus(sessionId, "Online")
                        repository.setupDisconnectHandler(sessionId)

                        val myUserId = auth.currentUser?.uid
                        if (myUserId != null) {
                            launch {
                                kotlinx.coroutines.flow.combine(
                                    repository.listenToSessionWatchers(sessionId),
                                    repository.listenToUserWatchers(sessionId, myUserId)
                                ) { sessionWatchers, myWatchers ->
                                    // High Accuracy if ANYONE is on the LiveMap OR looking directly at ME
                                    sessionWatchers > 0 || myWatchers > 0
                                }
                                    .collectLatest { requiresHighAccuracy ->
                                        // Only restart the GPS hardware if the mode actually changed
                                        if (isCurrentlyHighAccuracy != requiresHighAccuracy) {
                                            isCurrentlyHighAccuracy = requiresHighAccuracy
                                            // Update the ongoing request only if service is active and not paused
                                            if (isServiceActive && !isSessionPaused) {
                                                requestLocationUpdates(requiresHighAccuracy)
                                            }
                                        }
                                    }
                            }
                        }

                        // 1. Listen for permanent removal/kicks
                        launch {
                            repository.listenForRemoval(sessionId)
                                .collectLatest { isRemoved ->
                                    if (isRemoved) {
                                        isServiceActive = false
                                        shouldUpdateStatusOnStop = false
                                        stopLocationUpdates()
                                        repository.deleteMyNode(sessionId)
                                        stopSelf()
                                    }
                                }
                        }

                        // 2. Listen for Global Session Pause
                        launch {
                            repository.listenToSessionStatus(sessionId)
                                .collectLatest { status ->
                                    val wasPaused = isSessionPaused
                                    isSessionPaused = (status == "Paused")

                                    if (isSessionPaused && !wasPaused) {
                                        stopLocationUpdates()
                                    } else if (!isSessionPaused && wasPaused && isServiceActive) {
                                        serviceScope.launch {
                                            repository.updateUserStatus(
                                                sessionId, "Online"
                                            )
                                        }
                                        requestLocationUpdates(isCurrentlyHighAccuracy)
                                    }

                                    if (status == "Ended") {
                                        isServiceActive = false
                                        shouldUpdateStatusOnStop = false
                                        stopLocationUpdates()
                                        stopSelf()
                                    }
                                }
                        }

                        // 3. Listen for Individual User Pause (Admin paused THIS specific user)
                        launch {
                            val userId = auth.currentUser?.uid ?: return@launch
                            val userRef =
                                com.google.firebase.database.FirebaseDatabase.getInstance().reference
                                    .child("sessions")
                                    .child(sessionId)
                                    .child("users")
                                    .child(userId)
                                    .child("status")

                            val userStatusListener =
                                object : com.google.firebase.database.ValueEventListener {
                                    override fun onDataChange(
                                        snapshot: com.google.firebase.database.DataSnapshot
                                    ) {
                                        val status =
                                            snapshot.getValue(String::class.java) ?: "Online"

                                        if (isSessionPaused) return

                                        if (status == "Paused" && isServiceActive) {
                                            stopLocationUpdates()
                                        } else if (status != "Paused" && isServiceActive) {
                                            // Resume with the adaptive accuracy
                                            requestLocationUpdates(isCurrentlyHighAccuracy)
                                        }
                                    }

                                    override fun onCancelled(
                                        error: com.google.firebase.database.DatabaseError
                                    ) {
                                    }
                                }
                            userRef.addValueEventListener(userStatusListener)
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
    private fun requestLocationUpdates(highAccuracy: Boolean) {
        locationClient.removeLocationUpdates(locationCallback)

        val request = if (highAccuracy) {
            // HIGH POWER MODE: Someone is watching!
            // Update every 5 seconds, or if they move 2 meters.
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateDistanceMeters(2f)
                .build()
        } else {
            // LOW POWER MODE: Phones are in pockets.
            // Update every 30 seconds, and only if they move 20+ meters.
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000L)
                .setMinUpdateDistanceMeters(20f)
                .build()
        }

        locationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
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
            if (isServiceActive && !isSessionPaused) {
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

        gpsReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
            }
        }

        connectionListener?.let { connectedRef.removeEventListener(it) }

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