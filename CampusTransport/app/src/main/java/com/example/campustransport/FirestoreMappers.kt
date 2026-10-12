package com.example.campustransport

import com.google.firebase.firestore.DocumentSnapshot

// ✅ Manual mapping fixes the Kotlin "is" prefix bug (isOnTime / isImportant)
fun DocumentSnapshot.toBusRoute(): BusRoute = BusRoute(
    id = id,
    name = getString("name") ?: "",
    from = getString("from") ?: "",
    to = getString("to") ?: "",
    stops = get("stops") as? List<String> ?: emptyList(),
    firstBus = getString("firstBus") ?: "",
    lastBus = getString("lastBus") ?: "",
    frequency = getString("frequency") ?: "",
    isOnTime = getBoolean("isOnTime") ?: true,
    weekdaySchedule = get("weekdaySchedule") as? List<String> ?: emptyList(),
    weekendSchedule = get("weekendSchedule") as? List<String> ?: emptyList()
)

fun DocumentSnapshot.toNoticeItem(): NoticeItem = NoticeItem(
    id = id.toIntOrNull() ?: 0,
    title = getString("title") ?: "",
    message = getString("message") ?: "",
    date = getString("date") ?: "",
    category = getString("category") ?: "General",
    isImportant = getBoolean("isImportant") ?: false
)

fun DocumentSnapshot.toLiveBus(): LiveBus = LiveBus(
    routeId = getString("routeId") ?: id,
    routeName = getString("routeName") ?: getString("name") ?: getString("driverName") ?: "Campus Shuttle",
    lat = getDouble("lat") ?: 0.0,
    lng = getDouble("lng") ?: 0.0,
    updated = getLong("updated") ?: 0L,
    active = getBoolean("active") ?: true,
    driverName = getString("driverName") ?: ""
)
