package com.example.nobet.ui.calendar

import androidx.compose.ui.graphics.Color
import java.io.Serializable

/**
 * A shift that can be either a predefined shift type or a custom shift
 */
sealed class Shift : Serializable {
    abstract val label: String
    abstract val hours: Int
    abstract val color: Color
    
    /**
     * Predefined shift type (Morning, Night, Full)
     */
    data class Predefined(
        val type: ShiftType
    ) : Shift() {
        override val label: String get() = type.label
        override val hours: Int get() = type.hours
        override val color: Color get() = type.color
    }
    
    /**
     * Custom shift type defined by the user
     */
    data class Custom(
        val id: String,
        override val label: String,
        override val hours: Int,
        override val color: Color
    ) : Shift()
}