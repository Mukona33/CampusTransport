

package com.example.campustransport // ⚠️ CHANGE to your package name

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var routes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf("") }
    val issueTypes = listOf("Breakdown", "Traffic", "Accident", "Road Closure", "Other")
    var selectedType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("routes").get()
            .addOnSuccessListener { result ->
                routes = result.documents.mapNotNull { doc ->
                    doc.toObject(BusRoute::class.java)?.copy(id = doc.id)
                }
                if (routes.isNotEmpty()) selectedRoute = routes.first().name
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Issue", fontWeight = FontWeight.Bold) },
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
        if (submitted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Issue Reported!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The transport coordinator has been notified about $selectedRoute.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBack) { Text("Back to Dashboard") }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(text = "Report a problem", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Alert the coordinator about any issue on your route",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                SingleChoiceDropdown(
                    label = "Select Route",
                    options = routes.map { it.name },
                    selected = selectedRoute,
                    onSelect = { selectedRoute = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Issue Type", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issueTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the issue...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
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

                Button(
                    onClick = {
                        when {
                            selectedType.isBlank() -> errorMessage = "Please select an issue type"
                            description.isBlank() -> errorMessage = "Please describe the issue"
                            else -> {
                                errorMessage = ""
                                isSaving = true

                                val report = hashMapOf(
                                    "route" to selectedRoute,
                                    "issueType" to selectedType,
                                    "description" to description.trim(),
                                    "reportedBy" to (auth.currentUser?.email ?: "unknown"),
                                    "status" to "open",
                                    "timestamp" to System.currentTimeMillis()
                                )

                                db.collection("issues").add(report)
                                    .addOnSuccessListener {
                                        isSaving = false
                                        submitted = true
                                    }
                                    .addOnFailureListener { e ->
                                        isSaving = false
                                        errorMessage = e.message ?: "Failed to submit report"
                                    }
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
                        Text("Submit Report", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}