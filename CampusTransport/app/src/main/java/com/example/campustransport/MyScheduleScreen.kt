

package com.example.campustransport // ⚠️ CHANGE to your package name

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScheduleScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var routes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf("") }
    var isWeekend by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // ✅ Load routes + driver's saved assigned route
    LaunchedEffect(Unit) {
        db.collection("routes").get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    doc.toObject(BusRoute::class.java)?.copy(id = doc.id)
                }
                routes = list
                val uid = auth.currentUser?.uid ?: ""
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val assigned = doc.getString("assignedRoute") ?: ""
                        selectedRoute = if (list.any { it.name == assigned }) {
                            assigned
                        } else if (list.isNotEmpty()) {
                            list.first().name
                        } else ""
                        isLoading = false
                    }
                    .addOnFailureListener { isLoading = false }
            }
            .addOnFailureListener { isLoading = false }
    }

    val route = routes.find { it.name == selectedRoute }
    val times = if (isWeekend) route?.weekendSchedule ?: emptyList()
    else route?.weekdaySchedule ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Schedule", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // ✅ Reuses dropdown from FeedbackScreen.kt
                SingleChoiceDropdown(
                    label = "My Assigned Route",
                    options = routes.map { it.name },
                    selected = selectedRoute,
                    onSelect = { newName ->
                        selectedRoute = newName
                        // ✅ Save assignment to the driver's user document
                        val uid = auth.currentUser?.uid ?: ""
                        db.collection("users").document(uid).update("assignedRoute", newName)
                    }
                )

                if (route != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${route.from} → ${route.to}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${route.stops.size} stops • ${route.frequency}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isWeekend, onClick = { isWeekend = false }, label = { Text("Weekday") })
                    FilterChip(selected = isWeekend, onClick = { isWeekend = true }, label = { Text("Weekend") })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ✅ Reuses ScheduleRow from SchedulesScreen.kt
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(times) { index, time ->
                        ScheduleRow(
                            time = time,
                            routeName = selectedRoute,
                            platform = if (index % 2 == 0) "Platform A" else "Platform B",
                            isNext = index == 0
                        )
                    }
                    if (times.isEmpty()) {
                        item {
                            Text(
                                text = "No schedule available for this selection.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}