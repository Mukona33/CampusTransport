package com.example.campustransport // ⚠️ your package

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class DriverInfo(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val assignedRoute: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDriversScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var myResidence by remember { mutableStateOf("") }
    var drivers by remember { mutableStateOf<List<DriverInfo>>(emptyList()) }
    var routeZones by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var routeStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var routeIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var liveRouteIds by remember { mutableStateOf<Set<String>>(emptyList().toSet()) }

    // My residence
    DisposableEffect(uid) {
        if (uid.isEmpty()) return@DisposableEffect onDispose { }
        val l = db.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                myResidence = doc?.getString("residenceZone") ?: ""
            }
        onDispose { l.remove() }
    }

    // All drivers
    DisposableEffect(Unit) {
        val l = db.collection("users")
            .addSnapshotListener { snap, _ ->
                drivers = snap?.documents?.mapNotNull { d ->
                    val u = d.data() ?: return@mapNotNull null
                    if (u["userType"] != "Driver") null else DriverInfo(
                        uid = d.id,
                        firstName = u["firstName"] as? String ?: "",
                        lastName = u["lastName"] as? String ?: "",
                        email = u["email"] as? String ?: "",
                        assignedRoute = u["assignedRoute"] as? String ?: ""
                    )
                } ?: emptyList()
            }
        onDispose { l.remove() }
    }

    // Routes: zone + status + id per name
    DisposableEffect(Unit) {
        val l = db.collection("routes")
            .addSnapshotListener { snap, _ ->
                val zones = mutableMapOf<String, String>()
                val status = mutableMapOf<String, Boolean>()
                val ids = mutableMapOf<String, String>()
                snap?.documents?.forEach { d ->
                    val r = d.toBusRoute()
                    zones[r.name] = r.zone
                    status[r.name] = r.isOnTime
                    ids[r.name] = r.id
                }
                routeZones = zones
                routeStatus = status
                routeIds = ids
            }
        onDispose { l.remove() }
    }

    // Live sharing routes
    DisposableEffect(Unit) {
        val l = db.collection("liveRoutes")
            .addSnapshotListener { snap, _ ->
                liveRouteIds = snap?.documents
                    ?.filter { it.getBoolean("active") == true }
                    ?.map { it.id }?.toSet() ?: emptySet()
            }
        onDispose { l.remove() }
    }

    // ✅ ONLY drivers whose assigned route's zone == my residence
    val myDrivers = drivers.filter { d ->
        d.assignedRoute.isNotEmpty() && routeZones[d.assignedRoute] == myResidence
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Drivers", fontWeight = FontWeight.Bold) },
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
        ) {
            Text(
                text = "Drivers serving: $myResidence 🏘️",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp)
            )

            if (myDrivers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No drivers assigned yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your residence manager will assign drivers to $myResidence soon.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myDrivers) { driver ->
                        val isLive = routeIds[driver.assignedRoute] in liveRouteIds
                        val onTime = routeStatus[driver.assignedRoute] ?: true

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (driver.firstName.take(1) + driver.lastName.take(1))
                                                .ifBlank { "D" }.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${driver.firstName} ${driver.lastName}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "🚌 ${driver.assignedRoute}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (isLive) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF4CAF50)
                                        ) {
                                            Text(
                                                text = "LIVE",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (onTime) "On Time" else "Delayed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (onTime) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}