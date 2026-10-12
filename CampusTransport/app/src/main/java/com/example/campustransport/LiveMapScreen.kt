package com.example.campustransport

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

data class LiveBus(
    val routeId: String = "",
    val routeName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val updated: Long = 0L,
    val active: Boolean = false,
    val driverName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    var buses by remember { mutableStateOf<List<LiveBus>>(emptyList()) }
    var cameraMoved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        // 📍 Centered on Mbombela / UMP area
        position = CameraPosition.fromLatLngZoom(LatLng(-25.4961, 30.9694), 13f)
    }

    // ✅ LIVE listener — buses move in real time
    DisposableEffect(Unit) {
        val listener = FirebaseFirestore.getInstance().collection("liveRoutes")
            .addSnapshotListener { snapshot, _ ->
                buses = snapshot?.documents?.map { it.toLiveBus() }
                    ?.filter { it.active && (it.lat != 0.0 || it.lng != 0.0) }
                    ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(buses) {
        if (!cameraMoved && buses.isNotEmpty()) {
            val b = buses.first()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(b.lat, b.lng), 15f))
            cameraMoved = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Bus Map", fontWeight = FontWeight.Bold) },
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                buses.forEach { bus ->
                    val markerTitle = when {
                        bus.driverName.isNotBlank() && bus.routeName.isNotBlank() -> "${bus.routeName} (${bus.driverName})"
                        bus.driverName.isNotBlank() -> bus.driverName
                        bus.routeName.isNotBlank() -> bus.routeName
                        else -> "Campus Shuttle"
                    }
                    val updatedSeconds = maxOf(0, (System.currentTimeMillis() - bus.updated) / 1000)
                    Marker(
                        state = MarkerState(position = LatLng(bus.lat, bus.lng)),
                        title = markerTitle,
                        snippet = "Updated ${updatedSeconds}s ago"
                    )
                }
            }

            if (buses.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🚌 No buses are sharing live location right now.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    if (buses.isNotEmpty()) {
                        val b = buses.first()
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(LatLng(b.lat, b.lng), 15f)
                            )
                        }
                    } else {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(LatLng(-25.4961, 30.9694), 13f)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Re-center")
            }
        }
    }
}
