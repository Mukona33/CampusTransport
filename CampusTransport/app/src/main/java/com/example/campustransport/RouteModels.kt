package com.example.campustransport

// This matches the structure of our Firestore document
data class BusRoute(
    val id: String = "",
    val name: String = "",
    val from: String = "",
    val to: String = "",
    val stops: List<String> = emptyList(),
    val firstBus: String = "",
    val lastBus: String = "",
    val frequency: String = "",
    val isOnTime: Boolean = true,
    val weekdaySchedule: List<String> = emptyList(),
    val weekendSchedule: List<String> = emptyList()
)

// ✅ Real UMP Campus & Mbombela Residence Areas
val RESIDENCE_ZONES = listOf(
    "UMP Campus",
    "Belladonna",
    "Stonehenge",
    "Sonheuwel",
    "Sonpark",
    "West Acres",
    "Riverside Mall",
    "Mbombela CBD",
    "Kamagugu",
    "Phase 1",
    "Phase 2",
    "Phase 3",
    "Phase 4",
    "Phase 5",
    "Phase 6",
    "Luhambo",
    "Ngodini",
    "White River",
    "Msholozi",
    "Mataffin",
    "Talis Tower",
    "Kanyamazane",
    "Lekazi",
    "Karino"
)
