package com.example.campustransport

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLocationScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var driverName by remember { mutableStateOf("Driver") }
    var assignedRouteName by remember { mutableStateOf<String?>(null) }
    var allRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<BusRoute?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    // Effective Route details
    val effectiveRouteId = selectedRoute?.id?.ifEmpty { null } ?: uid.ifEmpty { "driver_shuttle" }
    val effectiveRouteName = selectedRoute?.name?.ifEmpty { null }
        ?: assignedRouteName?.ifEmpty { null }
        ?: "UMP Campus Shuttle ($driverName)"

    // Load Driver Info & Available Routes
    DisposableEffect(uid) {
        if (uid.isEmpty()) return@DisposableEffect onDispose { }
        val userListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                val first = doc?.getString("firstName") ?: doc?.getString("name") ?: ""
                if (first.isNotBlank()) driverName = first
                assignedRouteName = doc?.getString("assignedRoute")
            }

        val routesListener = db.collection("routes")
            .addSnapshotListener { snapshot, _ ->
                val routes = snapshot?.documents?.map { it.toBusRoute() } ?: emptyList()
                allRoutes = routes
                if (selectedRoute == null && routes.isNotEmpty()) {
                    val matching = routes.find { it.name == assignedRouteName }
                    selectedRoute = matching ?: routes.first()
                }
            }

        onDispose {
            userListener.remove()
            routesListener.remove()
        }
    }

    // Sync Live Sharing state from Firestore
    DisposableEffect(effectiveRouteId) {
        if (effectiveRouteId.isEmpty()) return@DisposableEffect onDispose { }
        val listener = db.collection("liveRoutes").document(effectiveRouteId)
            .addSnapshotListener { doc, _ ->
                isSharing = doc?.getBoolean("active") ?: false
            }
        onDispose { listener.remove() }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            startLocationSharing(context, effectiveRouteId, effectiveRouteName, driverName, uid)
            isSharing = true
        }
    }

    val launchOrStartSharing = {
        val finePermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (finePermission || coarsePermission) {
            startLocationSharing(context, effectiveRouteId, effectiveRouteName, driverName, uid)
            isSharing = true
        } else {
            launcher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Location", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ShareLocation,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = if (isSharing) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSharing) "You are LIVE on the map! 🛰️" else "Share your live location",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Driver: $driverName",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Route Selector / Display
            if (allRoutes.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = effectiveRouteName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Route to Share") },
                        leadingIcon = { Icon(Icons.Filled.DirectionsBus, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true },
                        enabled = !isSharing
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        allRoutes.forEach { route ->
                            DropdownMenuItem(
                                text = { Text("${route.name} (${route.from} → ${route.to})") },
                                onClick = {
                                    selectedRoute = route
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DirectionsBus, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Active Route", fontWeight = FontWeight.SemiBold)
                            Text(effectiveRouteName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isSharing) {
                Button(
                    onClick = {
                        stopLocationSharing(context, effectiveRouteId)
                        isSharing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Sharing", fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = launchOrStartSharing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Start Sharing Live Location", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "While sharing, location updates will post in real time to the student Live Map.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun startLocationSharing(
    context: Context,
    routeId: String,
    routeName: String,
    driverName: String,
    driverUid: String
) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        action = LocationTrackingService.ACTION_START
        putExtra(LocationTrackingService.EXTRA_ROUTE_ID, routeId)
        putExtra(LocationTrackingService.EXTRA_ROUTE_NAME, routeName)
        putExtra(LocationTrackingService.EXTRA_DRIVER_NAME, driverName)
        putExtra(LocationTrackingService.EXTRA_DRIVER_UID, driverUid)
    }
    ContextCompat.startForegroundService(context, intent)
}

fun stopLocationSharing(context: Context, routeId: String) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        action = LocationTrackingService.ACTION_STOP
        putExtra(LocationTrackingService.EXTRA_ROUTE_ID, routeId)
    }
    ContextCompat.startForegroundService(context, intent)
}
