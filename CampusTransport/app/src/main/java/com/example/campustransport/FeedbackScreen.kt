package com.example.campustransport // ⚠️ CHANGE to your actual package name

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val routes = listOf("Route 1", "Route 2", "Route 3", "Route 4")
    val categories = listOf("Service", "Driver Behaviour", "Cleanliness", "Safety", "Other")

    var selectedRoute by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Feedback", fontWeight = FontWeight.Bold) },
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
            // Success view
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
                Text(
                    text = "Thank You!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your feedback has been submitted successfully. The transport team will review it soon.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        selectedRoute = ""
                        selectedCategory = ""
                        rating = 0
                        message = ""
                        submitted = false
                    }
                ) {
                    Text("Submit Another Feedback")
                }
            }
        } else {
            // Feedback form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Share your experience",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Help us improve the campus transport service",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Route dropdown
                SingleChoiceDropdown(
                    label = "Select Route",
                    options = routes,
                    selected = selectedRoute,
                    onSelect = { selectedRoute = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category chips
                Text(
                    text = "Feedback Category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Star rating
                Text(
                    text = "Rate Your Experience",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                StarRating(rating = rating, onRatingChange = { rating = it })

                Spacer(modifier = Modifier.height(16.dp))

                // Message box
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Describe your feedback...") },
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
                            selectedRoute.isBlank() -> errorMessage = "Please select a route"
                            selectedCategory.isBlank() -> errorMessage = "Please select a category"
                            rating == 0 -> errorMessage = "Please rate your experience"
                            message.isBlank() -> errorMessage = "Please describe your feedback"
                            else -> {
                                errorMessage = ""

                                // ✅ Save feedback to Firestore
                                val feedback = hashMapOf(
                                    "route" to selectedRoute,
                                    "category" to selectedCategory,
                                    "rating" to rating,
                                    "message" to message,
                                    "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"),
                                    "timestamp" to System.currentTimeMillis()
                                )

                                db.collection("feedback").add(feedback)
                                    .addOnSuccessListener { submitted = true }
                                    .addOnFailureListener { e -> errorMessage = e.message ?: "Failed to submit feedback" }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Submit Feedback", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- Star Rating ---
@Composable
fun StarRating(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        for (i in 1..5) {
            IconButton(onClick = { onRatingChange(i) }) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Rate $i stars",
                    tint = if (i <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// --- Reusable Dropdown ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleChoiceDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FeedbackScreenPreview() {
    MaterialTheme {
        Surface {
            FeedbackScreen(onBack = { })
        }
    }
}