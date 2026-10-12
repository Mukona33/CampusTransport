package com.example.campustransport // ⚠️ CHANGE to your actual package name

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            com.example.campustransport.ui.theme.CampusTransportTheme(darkTheme = isDarkMode) {
                CampusTransportApp(
                    isDarkMode = isDarkMode,
                    onThemeToggle = { isDarkMode = it }
                )
            }
        }
    }
}

fun dashboardFor(userType: String): String = when (userType) {
    "Driver" -> "DriverDashboard"
    "Transport Coordinator", "Residence Manager" -> "CoordinatorWeb" // ✅ Both role names work
    else -> "Dashboard"
}

@Composable
fun CampusTransportApp(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var currentScreen by remember { mutableStateOf("Loading") }
    var userType by remember { mutableStateOf("Student") }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    // ✅ If profile was deleted by coordinator → force logout
                    if (!doc.exists() || doc.getBoolean("deleted") == true) {
                        auth.signOut()
                        currentScreen = "Login"
                    } else {
                        userType = doc.getString("userType") ?: "Student"
                        currentScreen = dashboardFor(userType)
                    }
                }
                .addOnFailureListener {
                    auth.signOut()
                    currentScreen = "Login"
                }
        } else {
            currentScreen = "Login"
        }
    }

    // ✅ BOTTOM NAV TABS (role-aware)
    val tabScreens = when (userType) {
        "Driver" -> listOf("DriverDashboard", "Routes", "LiveLocation", "Profile")
        "Transport Coordinator", "Residence Manager" -> emptyList<String>()
        else -> listOf("Dashboard", "Routes", "LiveMap", "Profile")
    }
    val showBottomBar = currentScreen in tabScreens

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == tabScreens[0],
                        onClick = { currentScreen = tabScreens[0] },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == tabScreens[1],
                        onClick = { currentScreen = tabScreens[1] },
                        icon = { Icon(Icons.Filled.DirectionsBus, contentDescription = "Routes") },
                        label = { Text("Routes") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == tabScreens[2],
                        onClick = { currentScreen = tabScreens[2] },
                        icon = { Icon(Icons.Filled.NearMe, contentDescription = "Live") },
                        label = { Text("Live") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == tabScreens[3],
                        onClick = { currentScreen = tabScreens[3] },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                "Loading" -> LoadingScreen()

                "Login" -> LoginScreen(
                    onNavigateToRegister = { currentScreen = "Register" },
                    onLoginSuccess = { type ->
                        userType = type
                        currentScreen = dashboardFor(type)
                    }
                )

                "Register" -> RegisterScreen(
                    onNavigateToLogin = { currentScreen = "Login" }
                )

                // ✅ STUDENT DASHBOARD
                "Dashboard" -> DashboardScreen(
                    onLogout = { auth.signOut(); currentScreen = "Login" },
                    onActionClick = { actionName ->
                        when (actionName) {
                            "Routes" -> currentScreen = "Routes"
                            "Schedules" -> currentScreen = "Schedules"
                            "Trip Planner" -> currentScreen = "TripPlanner"
                            "Notices" -> currentScreen = "Notices"
                            "Favourites" -> currentScreen = "Favourites"
                            "Feedback" -> currentScreen = "Feedback"
                            "Live Map" -> currentScreen = "LiveMap"
                            "My Drivers" -> currentScreen = "MyDrivers" // ✅ NEW
                            else -> Toast.makeText(context, "Clicked: $actionName", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // ✅ COORDINATOR → WEB
                "CoordinatorWeb" -> CoordinatorWebScreen(
                    onLogout = { auth.signOut(); currentScreen = "Login" }
                )

                // ✅ DRIVER DASHBOARD
                "DriverDashboard" -> DriverDashboardScreen(
                    onLogout = { auth.signOut(); currentScreen = "Login" },
                    onActionClick = { actionName ->
                        when (actionName) {
                            "Add Route" -> currentScreen = "AddRoute"
                            "My Schedule" -> currentScreen = "MySchedule"
                            "Update Status" -> currentScreen = "UpdateStatus"
                            "Report Issue" -> currentScreen = "ReportIssue"
                            "Notices" -> currentScreen = "Notices"
                            "Live Location" -> currentScreen = "LiveLocation"
                            else -> Toast.makeText(context, "Clicked: $actionName", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // ✅ DRIVER SCREENS
                "AddRoute" -> AddRouteScreen(onBack = { currentScreen = "DriverDashboard" })
                "MySchedule" -> MyScheduleScreen(onBack = { currentScreen = "DriverDashboard" })
                "UpdateStatus" -> UpdateStatusScreen(onBack = { currentScreen = "DriverDashboard" })
                "ReportIssue" -> ReportIssueScreen(onBack = { currentScreen = "DriverDashboard" })
                "LiveLocation" -> LiveLocationScreen(onBack = { currentScreen = "DriverDashboard" })

                // ✅ PROFILE (both roles)
                "Profile" -> ProfileScreen(
                    isDarkMode = isDarkMode,
                    onThemeToggle = onThemeToggle,
                    onNavigate = { screen -> currentScreen = screen },
                    onLogout = { auth.signOut(); currentScreen = "Login" },
                    onBack = { currentScreen = dashboardFor(userType) }
                )

                // ✅ STUDENT SCREENS
                "MyDrivers" -> MyDriversScreen(onBack = { currentScreen = dashboardFor(userType) }) // ✅ NEW
                "LiveMap" -> LiveMapScreen(onBack = { currentScreen = dashboardFor(userType) })
                "Routes" -> RoutesScreen(onBack = { currentScreen = dashboardFor(userType) })
                "Schedules" -> ScheduleScreen(onBack = { currentScreen = dashboardFor(userType) })
                "TripPlanner" -> TripPlannerScreen(onBack = { currentScreen = dashboardFor(userType) })
                "Notices" -> NoticesScreen(onBack = { currentScreen = dashboardFor(userType) })
                "Favourites" -> FavouritesScreen(
                    onBack = { currentScreen = dashboardFor(userType) },
                    onBrowseRoutes = { currentScreen = "Routes" }
                )
                "Feedback" -> FeedbackScreen(onBack = { currentScreen = dashboardFor(userType) })
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Text(
            text = "Loading...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}