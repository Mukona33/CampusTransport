package com.example.campustransport // ⚠️ CHANGE to your actual package name

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun RoutesScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) } // Changed to String for Firestore IDs
    var expandedId by remember { mutableStateOf<String?>(null) }

    // ✅ FIREBASE STATES
    var allRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // ✅ Fetch from Firestore when screen opens
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("routes")
            .get()
            .addOnSuccessListener { result ->
                val fetchedRoutes = result.documents.mapNotNull { doc ->
                    doc.toObject(BusRoute::class.java)?.copy(id = doc.id)
                }
                allRoutes = fetchedRoutes
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = e.message ?: "Failed to load routes"
                isLoading = false
            }
    }

    val filteredRoutes = allRoutes.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.from.contains(searchQuery, ignoreCase = true) ||
                it.to.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bus Routes", fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search routes or stops...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // ✅ Show Loading Spinner
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // ✅ Show Error
            else if (errorMessage.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            // ✅ Show Routes
            else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRoutes) { route ->
                        RouteCard(
                            route = route,
                            isExpanded = expandedId == route.id,
                            isFavorite = favoriteIds.contains(route.id),
                            onExpandClick = {
                                expandedId = if (expandedId == route.id) null else route.id
                            },
                            onFavoriteClick = {
                                favoriteIds = if (favoriteIds.contains(route.id)) {
                                    favoriteIds - route.id
                                } else {
                                    favoriteIds + route.id
                                }
                            }
                        )
                    }

                    if (filteredRoutes.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "😕", fontSize = 40.sp)
                                Text("No routes found", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Route Card (Updated to use String ID) ---
@Composable
fun RouteCard(
    route: BusRoute,
    isExpanded: Boolean,
    isFavorite: Boolean,
    onExpandClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
                    Text(
                        text = route.name,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${route.from} → ${route.to}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "${route.stops.size} stops • ${route.frequency}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favourite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (route.isOnTime) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (route.isOnTime) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (route.isOnTime) "On Time" else "Delayed",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (route.isOnTime) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "First: ${route.firstBus} • Last: ${route.lastBus}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Stops:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    route.stops.forEachIndexed { index, stop ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${index + 1}.", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stop, fontSize = 14.sp)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}