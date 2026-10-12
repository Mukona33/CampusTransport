package com.example.campustransport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

// --- ResRide Theme Colors ---
val OrangePrimary = Color(0xFFFF9800)
val OrangeDark = Color(0xFFFF5722)

// --- Data Models for UI ---
data class TransportSchedule(
    val routeId: String,
    val routeName: String,
    val time: String,
    val destination: String,
    val direction: String
)

// --- Main Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(onBack: () -> Unit = {}) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    var routes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Live routes listener
    DisposableEffect(Unit) {
        val listener = FirebaseFirestore.getInstance().collection("routes")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    routes = snapshot.documents.mapNotNull { it.toBusRoute() }
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routes & Schedules", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F7F8))
                .padding(16.dp)
        ) {
            RoutesAndSchedulesContent(routes, isLoading)
        }
    }
}

// --- Routes & Schedules Content (Live from Firestore) ---
@Composable
private fun RoutesAndSchedulesContent(routes: List<BusRoute>, isLoading: Boolean) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangePrimary)
        }
        return
    }

    val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
    val allSchedules = routes.flatMap { route ->
        val times = if (isWeekend) route.weekendSchedule else route.weekdaySchedule
        times.map { time ->
            TransportSchedule(
                routeId = route.id,
                routeName = route.name,
                time = time,
                destination = route.to,
                direction = "To ${route.to}"
            )
        }
    }.sortedBy { it.time }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Routes Section ---
        item {
            Text("Routes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OrangeDark)
        }
        items(routes) { route ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = route.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(OrangePrimary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (route.isOnTime) "✅ On Time" else "⚠️ Delayed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (route.isOnTime) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "From: ${route.from}",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "To: ${route.to}",
                        fontSize = 14.sp,
                        color = OrangeDark,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Frequency: ${route.frequency}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Stops:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    route.stops.forEachIndexed { index, stop ->
                        Text(
                            text = "${index + 1}. $stop",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        }

        // --- Schedules Section ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isWeekend) "Weekend Schedules" else "Weekday Schedules",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OrangeDark
            )
        }

        if (allSchedules.isEmpty()) {
            item {
                Text(
                    "No schedules available for today",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(allSchedules) { schedule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = schedule.routeName,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = schedule.direction,
                                fontSize = 13.sp,
                                color = OrangeDark
                            )
                        }
                        Text(
                            text = schedule.time,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = OrangePrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleRow(
    time: String,
    routeName: String,
    platform: String,
    isNext: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isNext) Color(0xFFFFF3E0) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = routeName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OrangeDark)
                Text(text = platform, fontSize = 13.sp, color = Color.Gray)
            }
            Text(text = time, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrangePrimary)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ScheduleScreenPreview() {
    ScheduleScreen()
}
