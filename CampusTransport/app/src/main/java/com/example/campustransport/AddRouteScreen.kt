package com.example.campustransport

import androidx.activity.compose.BackHandler
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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

// ✅ Helper: generates ["07:00", "08:00", ... "20:00"]
fun hourlyTimes(startHour: Int, endHour: Int, step: Int = 1): List<String> =
    (startHour..endHour step step).map { String.format(Locale.getDefault(), "%02d:00", it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRouteScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("UMP Campus") }
    var stopsText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Hourly") }
    var useHourlySchedule by remember { mutableStateOf(true) }
    var weekdayText by remember { mutableStateOf("") }
    var weekendText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    // ✅ DYNAMIC RESIDENCE STATES
    var residences by remember { mutableStateOf(RESIDENCE_ZONES) }
    var zone by remember { mutableStateOf(RESIDENCE_ZONES.first()) }
    var newResidence by remember { mutableStateOf("") }

    // ✅ Live-load residences from Firestore
    LaunchedEffect(Unit) {
        db.collection("residences")
            .addSnapshotListener { snap, _ ->
                val extra = snap?.documents?.mapNotNull { it.getString("name") } ?: emptyList()
                residences = (RESIDENCE_ZONES + extra).distinct()
                if (zone !in residences && residences.isNotEmpty()) {
                    zone = residences.first()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Route", fontWeight = FontWeight.Bold) },
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
        if (saved) {
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
                Text(text = "Route Added!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$name is now live for all students! 🚌",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            name = ""; from = ""; stopsText = ""
                            weekdayText = ""; weekendText = ""
                            newResidence = ""
                            saved = false
                        }
                    ) { Text("Add Another") }
                    Button(onClick = onBack) { Text("Done") }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(text = "Create a new bus route", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "It appears instantly on every student's app",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Route Name (e.g. R2 - Kamagugu Route)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = from,
                        onValueChange = { from = it },
                        label = { Text("From (Residence)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = to,
                        onValueChange = { to = it },
                        label = { Text("To") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ DYNAMIC RESIDENCE DROPDOWN
                var residenceExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = residenceExpanded,
                    onExpandedChange = { residenceExpanded = !residenceExpanded }
                ) {
                    OutlinedTextField(
                        value = zone,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Residence Zone") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = residenceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = residenceExpanded,
                        onDismissRequest = { residenceExpanded = false }
                    ) {
                        residences.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    zone = selectionOption
                                    residenceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ ADD NEW RESIDENCE FIELD
                OutlinedTextField(
                    value = newResidence,
                    onValueChange = { newResidence = it },
                    label = { Text("＋ Add new residence (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = stopsText,
                    onValueChange = { stopsText = it },
                    label = { Text("Stops (separate with commas)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Text(
                    text = "e.g. Mbombela CBD, Kamagugu, Phase 1, Phase 2, UMP Campus",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency (e.g. Hourly)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Auto hourly schedule switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto hourly schedule", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Weekday 07:00–20:00 hourly • Weekend 08:00–20:00 every 2h",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = useHourlySchedule, onCheckedChange = { useHourlySchedule = it })
                }

                if (!useHourlySchedule) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = weekdayText,
                        onValueChange = { weekdayText = it },
                        label = { Text("Weekday times (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = weekendText,
                        onValueChange = { weekendText = it },
                        label = { Text("Weekend times (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

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
                        val stopsList = stopsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val weekdayTimes = if (useHourlySchedule) hourlyTimes(7, 20)
                        else weekdayText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val weekendTimes = if (useHourlySchedule) hourlyTimes(8, 20, 2)
                        else weekendText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                        // ✅ Determine final residence
                        val finalResidence = newResidence.trim().ifBlank { zone }

                        when {
                            name.isBlank() || from.isBlank() || to.isBlank() ->
                                errorMessage = "Please fill route name, from and to"
                            stopsList.isEmpty() ->
                                errorMessage = "Please add at least one stop"
                            weekdayTimes.isEmpty() || weekendTimes.isEmpty() ->
                                errorMessage = "Please add schedule times or enable auto hourly schedule"
                            else -> {
                                errorMessage = ""
                                isSaving = true

                                // ✅ Save new residence to Firestore if provided
                                if (newResidence.trim().isNotEmpty()) {
                                    db.collection("residences")
                                        .document(newResidence.trim())
                                        .set(mapOf("name" to newResidence.trim()))
                                }

                                val route = hashMapOf(
                                    "name" to name.trim(),
                                    "from" to from.trim(),
                                    "to" to to.trim(),
                                    "zone" to finalResidence, // ✅ Use final residence
                                    "stops" to stopsList,
                                    "firstBus" to weekdayTimes.first(),
                                    "lastBus" to weekdayTimes.last(),
                                    "frequency" to frequency.trim(),
                                    "isOnTime" to true,
                                    "weekdaySchedule" to weekdayTimes,
                                    "weekendSchedule" to weekendTimes
                                )

                                db.collection("routes").add(route)
                                    .addOnSuccessListener {
                                        isSaving = false
                                        saved = true
                                    }
                                    .addOnFailureListener { e ->
                                        isSaving = false
                                        errorMessage = e.message ?: "Failed to save route"
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
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Route", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}