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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.content.edit

enum class TrackingMode {
    IDLE, SESSION_WATCHED, USER_WATCHED
}

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val repository = RealtimeRepository()
    private val auth = FirebaseAuth.getInstance()

    // SharedPreferences to recover session ID when app is swiped away/killed
    private val prefs by lazy {
        getSharedPreferences(
            "MultiLinkServicePrefs", MODE_PRIVATE
        )
    }

    private var currentSessionId: String? = null

    @Volatile
    private var isServiceActive = false

    @Volatile
    private var shouldUpdateStatusOnStop = true

    @Volatile
    private var isSessionPaused = false

    @Volatile
    private var currentTrackingMode = TrackingMode.IDLE

    private var lastRouteLogTime = 0L
    private var lastPinLogTime = 0L
    private var isFirstPin = true

    private var lastValidRouteLoc: Location? = null

    private var heartbeatJob: Job? = null

    @Volatile
    private var isLocationHistoryEnabled = false

    @Volatile
    private var historyIntervalMins = 30

    @Volatile
    private var isRouteTracingEnabled = false

    private var sessionConfigListener: ValueEventListener? = null
    private var gpsReceiver: BroadcastReceiver? = null
    private val connectedRef = FirebaseDatabase.getInstance()
        .getReference(".info/connected")
    private var connectionListener: ValueEventListener? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_STOP_MODE = "EXTRA_STOP_MODE"
        const val MODE_REMOVE = "REMOVE"
        const val NOTIFICATION_CHANNEL_ID = "location_channel"
        const val NOTIFICATION_ID = 1

        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)

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

        connectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected && isServiceActive && !isSessionPaused) {
                    currentSessionId?.let { sid ->
                        serviceScope.launch {
                            repository.updateUserStatus(sid, "Online")
                            repository.setupDisconnectHandler(sid)

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
                            } catch (_: SecurityException) {
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        connectedRef.addValueEventListener(connectionListener!!)

        gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                    val locationManager = context.getSystemService(
                        LOCATION_SERVICE
                    ) as android.location.LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(
                        android.location.LocationManager.GPS_PROVIDER
                    )
                    val newStatus = if (isGpsEnabled) "Online" else "Location Off"

                    currentSessionId?.let { sid ->
                        serviceScope.launch { repository.updateUserStatus(sid, newStatus) }
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
                        } catch (_: SecurityException) {
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
        // App Recovery Logic: If intent is null (service restarted by OS), load ID from SharedPreferences
        var sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)

        if (sessionId == null && intent == null) {
            sessionId = prefs.getString("ACTIVE_SESSION_ID", null)
        }

        when (intent?.action) {
            ACTION_START, null -> {
                if (sessionId != null) {
                    currentSessionId = sessionId
                    isServiceActive = true
                    shouldUpdateStatusOnStop = true

                    prefs.edit {
                        putString("ACTIVE_SESSION_ID", sessionId)
                    }

                    val configRef = FirebaseDatabase.getInstance().reference.child("sessions")
                        .child(sessionId)
                    sessionConfigListener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            isLocationHistoryEnabled = snapshot.child("isLocationHistoryEnabled")
                                .getValue(Boolean::class.java) ?: false
                            historyIntervalMins = snapshot.child("historyIntervalMins")
                                .getValue(Int::class.java) ?: 30
                            isRouteTracingEnabled = snapshot.child("isRouteTracingEnabled")
                                .getValue(Boolean::class.java) ?: false
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    }
                    configRef.addValueEventListener(sessionConfigListener!!)

                    startForegroundService()
                    requestLocationUpdates(currentTrackingMode)
                    startHeartbeat()

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
                                    //
                                    // Multi-Tier Watcher Logic
                                    when {
                                        myWatchers > 0 -> TrackingMode.USER_WATCHED
                                        sessionWatchers > 0 -> TrackingMode.SESSION_WATCHED
                                        else -> TrackingMode.IDLE
                                    }
                                }
                                    .collectLatest { newMode ->
                                        if (currentTrackingMode != newMode) {
                                            currentTrackingMode = newMode
                                            if (isServiceActive && !isSessionPaused) {
                                                requestLocationUpdates(newMode)
                                            }
                                        }
                                    }
                            }
                        }

                        launch {
                            repository.listenForRemoval(sessionId)
                                .collectLatest { isRemoved ->
                                    if (isRemoved) {
                                        isServiceActive = false
                                        shouldUpdateStatusOnStop = false
                                        stopLocationUpdates()
                                        repository.deleteMyNode(sessionId)
                                        clearSavedSession()
                                        stopSelf()
                                    }
                                }
                        }

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
                                        requestLocationUpdates(currentTrackingMode)
                                    }

                                    if (status == "Ended") {
                                        isServiceActive = false
                                        shouldUpdateStatusOnStop = false
                                        stopLocationUpdates()
                                        clearSavedSession()
                                        stopSelf()
                                    }
                                }
                        }

                        launch {
                            val userId = auth.currentUser?.uid ?: return@launch
                            val userRef = FirebaseDatabase.getInstance().reference.child("sessions")
                                .child(sessionId)
                                .child("users")
                                .child(userId)
                                .child("status")

                            val userStatusListener = object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val status = snapshot.getValue(String::class.java) ?: "Online"
                                    if (status == "Arrived") {
                                        isServiceActive = false
                                        shouldUpdateStatusOnStop = false
                                        stopLocationUpdates()
                                        clearSavedSession()
                                        stopSelf()
                                        return
                                    }
                                    if (isSessionPaused) return
                                    if (status == "Paused" && isServiceActive) {
                                        stopLocationUpdates()
                                    } else if (status != "Paused" && isServiceActive) {
                                        requestLocationUpdates(currentTrackingMode)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
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
                        serviceScope.launch { repository.updateUserStatus(sid, "Offline") }
                    }
                }
                stopLocationUpdates()
                clearSavedSession()
                stopSelf()
            }
        }
        return START_STICKY
    }

    //Heartbeat loops every 25 seconds to keep user "Online" even if GPS hasn't moved
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                delay(25_000L)
                if (isServiceActive && !isSessionPaused) {
                    currentSessionId?.let { sid ->
                        try {
                            repository.updateUserStatus(sid, "Online")
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    private fun clearSavedSession() {
        prefs.edit {
            remove("ACTIVE_SESSION_ID")
        }
        heartbeatJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(mode: TrackingMode) {
        locationClient.removeLocationUpdates(locationCallback)

        //3-Tier Adaptive Speed Logic
        val request = when (mode) {
            TrackingMode.USER_WATCHED -> {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                    .setMinUpdateDistanceMeters(2f)
                    .build()
            }

            TrackingMode.SESSION_WATCHED -> {
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L)
                    .setMinUpdateDistanceMeters(10f)
                    .build()
            }

            TrackingMode.IDLE -> {
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000L)
                    .setMinUpdateDistanceMeters(30f)
                    .build()
            }
        }

        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun uploadLocationToFirebase(sessionId: String, location: Location) {
        val batteryStatus: Intent? =
            registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct =
            if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val now = System.currentTimeMillis()
        var shouldLogRoute = false
        var shouldLogPin = false

        //Race Condition: Synchronous check ensures no duplicate pins
        synchronized(this) {
            if (isRouteTracingEnabled && (now - lastRouteLogTime > 60_000L)) {
                var isValidPoint = true

                //Map Jitter: Discard points that imply impossible speed (> 144 km/h)
                lastValidRouteLoc?.let { lastLoc ->
                    val distanceMeters = location.distanceTo(lastLoc)
                    val timeSeconds = (now - lastRouteLogTime) / 1000f
                    val impliedSpeed = distanceMeters / timeSeconds
                    if (impliedSpeed > 40f) isValidPoint = false
                }

                if (isValidPoint) {
                    shouldLogRoute = true
                    lastRouteLogTime = now
                    lastValidRouteLoc = location
                }
            }

            val pinIntervalMs = historyIntervalMins * 60 * 1000L
            if (isLocationHistoryEnabled && (isFirstPin || now - lastPinLogTime > pinIntervalMs)) {
                shouldLogPin = true
                lastPinLogTime = now
                isFirstPin = false
            }
        }

        serviceScope.launch {
            if (isServiceActive && !isSessionPaused) {
                repository.updateMyLocation(
                    sessionId = sessionId, lat = location.latitude, lng = location.longitude,
                    heading = location.bearing, battery = batteryPct, isCharging = isCharging,
                    speed = location.speed
                )

                // Only log to Firebase if the synchronous check allowed it
                if (shouldLogRoute) repository.logLocationHistory(
                    sessionId, location.latitude, location.longitude, isPin = false
                )
                if (shouldLogPin) repository.logLocationHistory(
                    sessionId, location.latitude, location.longitude, isPin = true
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

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MultiLink Active")
            .setContentText("Sharing your live location...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        isServiceActive = false
        heartbeatJob?.cancel()

        if (shouldUpdateStatusOnStop) {
            currentSessionId?.let { sid ->
                serviceScope.launch { repository.updateUserStatus(sid, "Offline") }
            }
        }

        gpsReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }

        currentSessionId?.let { sid ->
            sessionConfigListener?.let {
                FirebaseDatabase.getInstance().reference.child("sessions")
                    .child(sid)
                    .removeEventListener(it)
            }
        }

        connectionListener?.let { connectedRef.removeEventListener(it) }

        super.onDestroy()
        serviceScope.cancel()
        locationClient.removeLocationUpdates(locationCallback)
    }
}