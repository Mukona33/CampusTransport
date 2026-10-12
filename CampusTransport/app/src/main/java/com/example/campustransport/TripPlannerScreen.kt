package com.example.campustransport // ⚠️ CHANGE to your actual package name

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlannerScreen(onBack: () -> Unit) {
    var fromStop by remember { mutableStateOf("") }
    var toStop by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }

    var allRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var isLoadingRoutes by remember { mutableStateOf(true) }

    // Fetch routes from Firestore
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("routes")
            .get()
            .addOnSuccessListener { result ->
                allRoutes = result.documents.mapNotNull { doc ->
                    doc.toObject(BusRoute::class.java)?.copy(id = doc.id)
                }
                isLoadingRoutes = false
            }
            .addOnFailureListener {
                isLoadingRoutes = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Planner", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Plan your journey",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Type your departure and destination stops or tap suggestions below",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Suggestion Chips
            Text(
                text = "Popular Stops & Zones:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RESIDENCE_ZONES.forEach { zone ->
                    SuggestionChip(
                        onClick = {
                            if (fromStop.isBlank()) {
                                fromStop = zone
                            } else {
                                toStop = zone
                            }
                            showResults = false
                        },
                        label = { Text(zone) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FROM text input
            OutlinedTextField(
                value = fromStop,
                onValueChange = {
                    fromStop = it
                    showResults = false
                },
                label = { Text("From (Current Stop / Area)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Swap button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = {
                    val temp = fromStop
                    fromStop = toStop
                    toStop = temp
                    showResults = false
                }) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Swap stops",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // TO text input
            OutlinedTextField(
                value = toStop,
                onValueChange = {
                    toStop = it
                    showResults = false
                },
                label = { Text("To (Destination / Area)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan button
            Button(
                onClick = {
                    when {
                        fromStop.isBlank() || toStop.isBlank() -> {
                            errorMessage = "Please enter both departure and destination stops"
                            showResults = false
                        }
                        fromStop.equals(toStop, ignoreCase = true) -> {
                            errorMessage = "Origin and destination cannot be the same"
                            showResults = false
                        }
                        else -> {
                            errorMessage = ""
                            showResults = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoadingRoutes) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Find My Route", fontSize = 16.sp)
                }
            }

            // Results
            if (showResults) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Matching Routes for $fromStop → $toStop",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val matchingRoutes = allRoutes.filter { route ->
                    val fromMatch = route.name.contains(fromStop, ignoreCase = true) ||
                            route.from.contains(fromStop, ignoreCase = true) ||
                            route.to.contains(fromStop, ignoreCase = true) ||
                            route.stops.any { it.contains(fromStop, ignoreCase = true) }
                    val toMatch = route.name.contains(toStop, ignoreCase = true) ||
                            route.from.contains(toStop, ignoreCase = true) ||
                            route.to.contains(toStop, ignoreCase = true) ||
                            route.stops.any { it.contains(toStop, ignoreCase = true) }
                    fromMatch && toMatch
                }

                if (matchingRoutes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No direct routes found matching '$fromStop' and '$toStop'.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    matchingRoutes.forEach { route ->
                        RealRouteTripCard(route = route)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- Real Route Trip Card ---
@Composable
fun RealRouteTripCard(route: BusRoute) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = route.name,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (route.isOnTime) Color(0xFF4CAF50) else Color(0xFFE53935)
                ) {
                    Text(
                        text = if (route.isOnTime) "✅ On Time" else "⚠️ Delayed",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = route.frequency,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${route.from} → ${route.to}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "First Bus: ${route.firstBus} | Last Bus: ${route.lastBus}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (route.stops.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Stops (${route.stops.size}): ${route.stops.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TripPlannerScreenPreview() {
    MaterialTheme {
        Surface {
            TripPlannerScreen(onBack = { })
        }
    }
}
