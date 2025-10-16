package com.example.nobet.ui.calendar

import androidx.compose.ui.graphics.Color

/**
 * Custom Shift Type that users can define
 */
data class CustomShiftType(
    val id: String, // Unique identifier for the custom shift type
    val label: String, // Display label (e.g., "08-13")
    val hours: Int, // Number of hours for this shift
    val color: Color // Color to display this shift type
)