

package com.example.campustransport // ⚠️ CHANGE to your package name

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateStatusScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()

    var routes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("On Time") }
    var note by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var updated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("routes").get()
            .addOnSuccessListener { result ->
                routes = result.documents.mapNotNull { doc ->
                    doc.toObject(BusRoute::class.java)?.copy(id = doc.id)
                }
                if (routes.isNotEmpty()) {
                    selectedRoute = routes.first().name
                    selectedStatus = if (routes.first().isOnTime) "On Time" else "Delayed"
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Status", fontWeight = FontWeight.Bold) },
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
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(text = "Update live bus status", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Students see this change instantly on their Routes page",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                SingleChoiceDropdown(
                    label = "Select Route",
                    options = routes.map { it.name },
                    selected = selectedRoute,
                    onSelect = { name ->
                        selectedRoute = name
                        updated = false
                        val r = routes.find { it.name == name }
                        selectedStatus = if (r?.isOnTime == true) "On Time" else "Delayed"
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Bus Status", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStatus == "On Time",
                        onClick = { selectedStatus = "On Time"; updated = false },
                        label = { Text("✅ On Time") }
                    )
                    FilterChip(
                        selected = selectedStatus == "Delayed",
                        onClick = { selectedStatus = "Delayed"; updated = false },
                        label = { Text("⚠️ Delayed") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional, e.g. Heavy traffic near Main Gate)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val route = routes.find { it.name == selectedRoute }
                        route?.let {
                            isSaving = true
                            db.collection("routes").document(it.id)
                                .update(
                                    mapOf(
                                        "isOnTime" to (selectedStatus == "On Time"),
                                        "statusNote" to note.trim()
                                    )
                                )
                                .addOnSuccessListener {
                                    isSaving = false
                                    updated = true
                                }
                                .addOnFailureListener { e ->
                                    isSaving = false
                                    updated = false
                                    note = e.message ?: "Failed to update"
                                }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Update Status", fontSize = 16.sp)
                    }
                }

                if (updated) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = "Status updated! Students now see \"$selectedStatus\" for $selectedRoute. 🎉",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}