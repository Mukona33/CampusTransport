package com.example.campustransport

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore

class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_ROUTE_ID = "routeId"
        const val EXTRA_ROUTE_NAME = "routeName"
        const val EXTRA_DRIVER_NAME = "driverName"
        const val EXTRA_DRIVER_UID = "driverUid"
        private const val CHANNEL_ID = "live_location_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var routeId = ""
    private var routeName = ""
    private var driverName = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val inputRouteId = intent.getStringExtra(EXTRA_ROUTE_ID) ?: ""
                val inputDriverUid = intent.getStringExtra(EXTRA_DRIVER_UID) ?: ""
                val inputDriverName = intent.getStringExtra(EXTRA_DRIVER_NAME) ?: ""
                val inputRouteName = intent.getStringExtra(EXTRA_ROUTE_NAME) ?: ""

                // Ensure routeId is NEVER empty so Firestore document reference is valid
                routeId = inputRouteId.ifEmpty { inputDriverUid }.ifEmpty { "driver_shuttle" }
                driverName = inputDriverName
                routeName = inputRouteName.ifEmpty { driverName }.ifEmpty { "Campus Shuttle" }

                createChannel()
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startLocationUpdates()
            }
            ACTION_STOP -> {
                val id = intent.getStringExtra(EXTRA_ROUTE_ID) ?: routeId
                stopLocationUpdates()
                if (id.isNotEmpty()) {
                    FirebaseFirestore.getInstance().collection("liveRoutes").document(id)
                        .set(mapOf("active" to false, "updated" to System.currentTimeMillis()))
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live Location Sharing",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val titleText = if (driverName.isNotBlank()) "UMP Shuttle ($driverName) 🚌" else "UMP Live Sharing 🚌"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText("Sharing live location for $routeName")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startLocationUpdates() {
        val fineGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val data = mapOf(
                        "routeId" to routeId,
                        "routeName" to routeName,
                        "driverName" to driverName,
                        "lat" to loc.latitude,
                        "lng" to loc.longitude,
                        "updated" to System.currentTimeMillis(),
                        "active" to true
                    )
                    FirebaseFirestore.getInstance().collection("liveRoutes").document(routeId)
                        .set(data)
                }
            }
        }

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }
}
