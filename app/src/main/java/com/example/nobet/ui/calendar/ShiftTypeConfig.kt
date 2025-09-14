package com.example.nobet.ui.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

/**
 * Shift Type Configuration Manager
 * Handles customizable shift types with user-defined hours
 */
class ShiftTypeConfig(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "shift_type_config"
        private const val MORNING_HOURS_KEY = "morning_hours"
        private const val NIGHT_HOURS_KEY = "night_hours"
        private const val FULL_HOURS_KEY = "full_hours"
        private const val MORNING_LABEL_KEY = "morning_label"
        private const val NIGHT_LABEL_KEY = "night_label"
        private const val FULL_LABEL_KEY = "full_label"
        
        // Default values
        const val DEFAULT_MORNING_HOURS = 8
        const val DEFAULT_NIGHT_HOURS = 16
        const val DEFAULT_FULL_HOURS = 24
        const val DEFAULT_MORNING_LABEL = "08-16"
        const val DEFAULT_NIGHT_LABEL = "16-08"
        const val DEFAULT_FULL_LABEL = "08-08"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Get current shift configurations
    fun getMorningHours(): Int = prefs.getInt(MORNING_HOURS_KEY, DEFAULT_MORNING_HOURS)
    fun getNightHours(): Int = prefs.getInt(NIGHT_HOURS_KEY, DEFAULT_NIGHT_HOURS)
    fun getFullHours(): Int = prefs.getInt(FULL_HOURS_KEY, DEFAULT_FULL_HOURS)
    
    fun getMorningLabel(): String = prefs.getString(MORNING_LABEL_KEY, DEFAULT_MORNING_LABEL) ?: DEFAULT_MORNING_LABEL
    fun getNightLabel(): String = prefs.getString(NIGHT_LABEL_KEY, DEFAULT_NIGHT_LABEL) ?: DEFAULT_NIGHT_LABEL
    fun getFullLabel(): String = prefs.getString(FULL_LABEL_KEY, DEFAULT_FULL_LABEL) ?: DEFAULT_FULL_LABEL
    
    // Update shift configurations
    fun setMorningConfig(hours: Int, label: String) {
        prefs.edit()
            .putInt(MORNING_HOURS_KEY, hours)
            .putString(MORNING_LABEL_KEY, label)
            .apply()
    }
    
    fun setNightConfig(hours: Int, label: String) {
        prefs.edit()
            .putInt(NIGHT_HOURS_KEY, hours)
            .putString(NIGHT_LABEL_KEY, label)
            .apply()
    }
    
    fun setFullConfig(hours: Int, label: String) {
        prefs.edit()
            .putInt(FULL_HOURS_KEY, hours)
            .putString(FULL_LABEL_KEY, label)
            .apply()
    }
    
    // Get dynamic ShiftType with current configurations
    fun getCurrentShiftTypes(): List<DynamicShiftType> {
        return listOf(
            DynamicShiftType(
                type = ShiftType.MORNING,
                hours = getMorningHours(),
                label = getMorningLabel(),
                color = Color(0xFF64B5F6)
            ),
            DynamicShiftType(
                type = ShiftType.NIGHT,
                hours = getNightHours(),
                label = getNightLabel(),
                color = Color(0xFF9575CD)
            ),
            DynamicShiftType(
                type = ShiftType.FULL,
                hours = getFullHours(),
                label = getFullLabel(),
                color = Color(0xFFF44336)
            )
        )
    }
}

/**
 * Dynamic Shift Type with customizable properties
 */
data class DynamicShiftType(
    val type: ShiftType,
    val hours: Int,
    val label: String,
    val color: Color
)