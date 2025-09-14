package com.example.nobet.ui.calendar

enum class ShiftType(val label: String, val hours: Int, val color: androidx.compose.ui.graphics.Color) {
    MORNING("08-16", 8, androidx.compose.ui.graphics.Color(0xFF64B5F6)), // Mavi
    NIGHT("16-08", 16, androidx.compose.ui.graphics.Color(0xFF9575CD)),  // Mor
    FULL("08-08", 24, androidx.compose.ui.graphics.Color(0xFFF44336))   // Kırmızı
}