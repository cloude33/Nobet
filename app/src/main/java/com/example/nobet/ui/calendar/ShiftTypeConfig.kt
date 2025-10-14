package com.example.nobet.ui.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        private const val CUSTOM_SHIFTS_KEY = "custom_shifts"
        
        // Default values
        const val DEFAULT_MORNING_HOURS = 8
        const val DEFAULT_NIGHT_HOURS = 16
        const val DEFAULT_FULL_HOURS = 24
        const val DEFAULT_MORNING_LABEL = "08-16"
        const val DEFAULT_NIGHT_LABEL = "16-08"
        const val DEFAULT_FULL_LABEL = "08-08"
        
        // Predefined colors for custom shifts
        val PREDEFINED_COLORS = listOf(
            Color(0xFFE91E63), // Pink
            Color(0xFF9C27B0), // Purple
            Color(0xFF673AB7), // Deep Purple
            Color(0xFF3F51B5), // Indigo
            Color(0xFF2196F3), // Blue
            Color(0xFF03DAC6), // Teal
            Color(0xFF4CAF50), // Green
            Color(0xFF8BC34A), // Light Green
            Color(0xFFCDDC39), // Lime
            Color(0xFFFFC107), // Amber
            Color(0xFFFF9800), // Orange
            Color(0xFFFF5722)  // Deep Orange
        )
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
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
    
    // Custom shift types management
    fun getCustomShiftTypes(): List<CustomShiftType> {
        val json = prefs.getString(CUSTOM_SHIFTS_KEY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<CustomShiftType>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    fun addCustomShiftType(shift: CustomShiftType) {
        val currentShifts = getCustomShiftTypes().toMutableList()
        currentShifts.add(shift)
        saveCustomShiftTypes(currentShifts)
    }
    
    fun updateCustomShiftType(id: String, updatedShift: CustomShiftType) {
        val currentShifts = getCustomShiftTypes().toMutableList()
        val index = currentShifts.indexOfFirst { it.id == id }
        if (index != -1) {
            currentShifts[index] = updatedShift
            saveCustomShiftTypes(currentShifts)
        }
    }
    
    fun removeCustomShiftType(id: String) {
        val currentShifts = getCustomShiftTypes().toMutableList()
        currentShifts.removeAll { it.id == id }
        saveCustomShiftTypes(currentShifts)
    }
    
    private fun saveCustomShiftTypes(shifts: List<CustomShiftType>) {
        val json = gson.toJson(shifts)
        prefs.edit().putString(CUSTOM_SHIFTS_KEY, json).apply()
    }
    
    // Get all shift types (predefined + custom)
    fun getAllShiftTypes(): List<DynamicShiftType> {
        val predefined = listOf(
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
        
        val custom = getCustomShiftTypes().map { customShift ->
            DynamicShiftType(
                type = null, // Custom shifts don't have a predefined type
                id = customShift.id,
                hours = customShift.hours,
                label = customShift.label,
                color = customShift.color
            )
        }
        
        return predefined + custom
    }
}

/**
 * Dynamic Shift Type with customizable properties
 */
data class DynamicShiftType(
    val type: ShiftType?, // null for custom shifts
    val id: String? = null, // ID for custom shifts
    val hours: Int,
    val label: String,
    val color: Color
)