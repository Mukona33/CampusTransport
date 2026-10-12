package com.example.campustransport // ⚠️ CHANGE to your actual package name

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NoticeTracker {
    // Stores the exact millisecond the user last opened the Notices page
    var lastViewedTimestamp by mutableStateOf(0L)
}