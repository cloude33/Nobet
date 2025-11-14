package com.example.nobet.utils

import java.time.DayOfWeek
import java.time.LocalDate
import com.example.nobet.utils.TurkishHolidays
import com.example.nobet.ui.calendar.ShiftType

/**
 * Bir tarihin zorunlu çalışma saatini hesaplayan yardımcı nesne.
 * Bu, iş mantığını TurkishHolidays'ten ayırır.
 */
object WorkHourCalculator {

    private const val FULL_DAY_HOURS = 8
    // 28 Ekim'de 5 saat zorunlu mesai (partial holiday rule)
    private const val OCTOBER_28_HOURS = 5
    private const val NO_WORK_HOURS = 0

    fun getExpectedWorkHours(date: LocalDate): Int {
        val holidayInfo = TurkishHolidays.getHoliday(date)

        return when {
            // Eğer gün tam gün resmi tatilse (dini veya milli)
            holidayInfo != null && (holidayInfo.type == TurkishHolidays.HolidayType.OFFICIAL || holidayInfo.type == TurkishHolidays.HolidayType.RELIGIOUS) -> NO_WORK_HOURS

            // 28 Ekim özel durumu - partial holiday with 5 required hours
            date.monthValue == 10 && date.dayOfMonth == 28 -> OCTOBER_28_HOURS

            // Hafta sonu ise (ve resmi tatil değilse)
            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> NO_WORK_HOURS

            // Diğer tüm hafta içi günler
            else -> FULL_DAY_HOURS
        }
    }
    
    /**
     * Calculates the expected work hours considering shift types
     * For annual leave and report days on working days, deducts from expected hours
     */
    fun getExpectedWorkHoursWithShift(date: LocalDate, shiftType: ShiftType?): Int {
        // If no shift is assigned, use the default calculation
        if (shiftType == null) {
            return getExpectedWorkHours(date)
        }
        
        // For annual leave and report days on working days, return 0 expected hours
        return when (shiftType) {
            ShiftType.ANNUAL_LEAVE, ShiftType.REPORT -> {
                // Only deduct if it's a working day
                if (getExpectedWorkHours(date) > 0) {
                    NO_WORK_HOURS
                } else {
                    getExpectedWorkHours(date)
                }
            }
            else -> getExpectedWorkHours(date)
        }
    }
}