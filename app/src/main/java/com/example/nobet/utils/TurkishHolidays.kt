package com.example.nobet.utils

import java.time.LocalDate
import java.time.Month
import java.time.Year

/**
 * Turkish Official Holidays and Religious Holidays Helper
 * Manages calculation of official holidays, religious holidays, and half-days
 */
object TurkishHolidays {
    
    data class Holiday(
        val date: LocalDate,
        val name: String,
        val type: HolidayType,
        val workingHours: Int = 0 // Hours counted as work (0 = full holiday, 5 = half-day, 3 = partial)
    )
    
    enum class HolidayType {
        OFFICIAL, // Official public holidays
        RELIGIOUS, // Religious holidays (Bayram)
        HALF_DAY, // Half-day holidays (like holiday eves)
        PARTIAL_DAY // Partial holidays (like October 28 - partial holiday from 13:00)
    }
    
    /**
     * Get all Turkish holidays for a specific year
     */
    fun getHolidaysForYear(year: Int): List<Holiday> {
        val holidays = mutableListOf<Holiday>()
        
        // Add official holidays
        holidays.addAll(getOfficialHolidays(year))
        
        // Add religious holidays (approximate dates - should be updated yearly)
        holidays.addAll(getReligiousHolidays(year))
        
        // Add half-day holidays (holiday eves)
        holidays.addAll(getHalfDayHolidays(year))
        
        // Add special partial holidays
        holidays.addAll(getPartialDayHolidays(year))
        
        return holidays.sortedBy { it.date }
    }
    
    /**
     * Official Turkish Public Holidays (Fixed dates)
     */
    private fun getOfficialHolidays(year: Int): List<Holiday> {
        return listOf(
            Holiday(
                LocalDate.of(year, Month.JANUARY, 1),
                "Yılbaşı",
                HolidayType.OFFICIAL
            ),
            Holiday(
                LocalDate.of(year, Month.APRIL, 23),
                "Ulusal Egemenlik ve Çocuk Bayramı",
                HolidayType.OFFICIAL
            ),
            Holiday(
                LocalDate.of(year, Month.MAY, 1),
                "Emek ve Dayanışma Günü",
                HolidayType.OFFICIAL
            ),
            Holiday(
                LocalDate.of(year, Month.MAY, 19),
                "Atatürk'ü Anma, Gençlik ve Spor Bayramı",
                HolidayType.OFFICIAL
            ),
            Holiday(
                LocalDate.of(year, Month.JULY, 15),
                "Demokrasi ve Milli Birlik Günü",
                HolidayType.OFFICIAL
            ),
            Holiday(
                LocalDate.of(year, Month.AUGUST, 30),
                "Zafer Bayramı",
                HolidayType.OFFICIAL
            ),
            Holiday(
                LocalDate.of(year, Month.OCTOBER, 29),
                "Cumhuriyet Bayramı",
                HolidayType.OFFICIAL
            ),
            Holiday(
                LocalDate.of(year, Month.DECEMBER, 31),
                "Yılbaşı Gecesi",
                HolidayType.OFFICIAL
            )
        )
    }
    
    /**
     * Religious Holidays (Variable dates - approximate)
     * Note: These dates should be updated annually as they follow lunar calendar
     */
    private fun getReligiousHolidays(year: Int): List<Holiday> {
        return when (year) {
            2024 -> listOf(
                // Ramazan Bayramı 2024
                Holiday(LocalDate.of(2024, 4, 10), "Ramazan Bayramı 1. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2024, 4, 11), "Ramazan Bayramı 2. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2024, 4, 12), "Ramazan Bayramı 3. Gün", HolidayType.RELIGIOUS),
                
                // Kurban Bayramı 2024
                Holiday(LocalDate.of(2024, 6, 16), "Kurban Bayramı 1. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2024, 6, 17), "Kurban Bayramı 2. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2024, 6, 18), "Kurban Bayramı 3. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2024, 6, 19), "Kurban Bayramı 4. Gün", HolidayType.RELIGIOUS)
            )
            2025 -> listOf(
                // Ramazan Bayramı 2025 (approximate)
                Holiday(LocalDate.of(2025, 3, 31), "Ramazan Bayramı 1. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2025, 4, 1), "Ramazan Bayramı 2. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2025, 4, 2), "Ramazan Bayramı 3. Gün", HolidayType.RELIGIOUS),
                
                // Kurban Bayramı 2025 (approximate)
                Holiday(LocalDate.of(2025, 6, 7), "Kurban Bayramı 1. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2025, 6, 8), "Kurban Bayramı 2. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2025, 6, 9), "Kurban Bayramı 3. Gün", HolidayType.RELIGIOUS),
                Holiday(LocalDate.of(2025, 6, 10), "Kurban Bayramı 4. Gün", HolidayType.RELIGIOUS)
            )
            else -> emptyList() // For other years, no religious holidays defined
        }
    }
    
    /**
     * Half-day holidays (Holiday eves that count as 5 working hours)
     */
    private fun getHalfDayHolidays(year: Int): List<Holiday> {
        val halfDays = mutableListOf<Holiday>()
        
        // Official holiday eves (if they fall on weekdays)
        val officialHolidays = getOfficialHolidays(year)
        officialHolidays.forEach { holiday ->
            val eveDate = holiday.date.minusDays(1)
            // Only add if eve falls on weekday (Monday-Friday)
            // Exclude August 29 as it's not an arife day for August 30 (Zafer Bayramı)
            if (eveDate.dayOfWeek.value in 1..5 && 
                !(eveDate.monthValue == 8 && eveDate.dayOfMonth == 29)) {
                halfDays.add(
                    Holiday(
                        eveDate,
                        "${holiday.name} Arifesi",
                        HolidayType.HALF_DAY,
                        5 // 5 hours of work
                    )
                )
            }
        }
        
        // Religious holiday eves
        val religiousHolidays = getReligiousHolidays(year)
        if (religiousHolidays.isNotEmpty()) {
            // Add eve of first Ramazan Bayramı day
            val ramazanStart = religiousHolidays.firstOrNull { it.name.contains("Ramazan Bayramı 1") }
            ramazanStart?.let {
                val eveDate = it.date.minusDays(1)
                if (eveDate.dayOfWeek.value in 1..5) {
                    halfDays.add(
                        Holiday(
                            eveDate,
                            "Ramazan Bayramı Arifesi",
                            HolidayType.HALF_DAY,
                            5
                        )
                    )
                }
            }
            
            // Add eve of first Kurban Bayramı day
            val kurbanStart = religiousHolidays.firstOrNull { it.name.contains("Kurban Bayramı 1") }
            kurbanStart?.let {
                val eveDate = it.date.minusDays(1)
                if (eveDate.dayOfWeek.value in 1..5) {
                    halfDays.add(
                        Holiday(
                            eveDate,
                            "Kurban Bayramı Arifesi",
                            HolidayType.HALF_DAY,
                            5
                        )
                    )
                }
            }
        }
        
        return halfDays
    }
    
    /**
     * Partial holidays (like October 28 - partial holiday from 13:00)
     */
    private fun getPartialDayHolidays(year: Int): List<Holiday> {
        val partialDays = mutableListOf<Holiday>()
        
        // October 28 is partially a holiday (from 13:00 onwards)
        // Only add if it falls on weekday (Monday-Friday)
        val oct28 = LocalDate.of(year, Month.OCTOBER, 28)
        if (oct28.dayOfWeek.value in 1..5) {
            partialDays.add(
                Holiday(
                    oct28,
                    "Cumhuriyet Bayramı Arifesi (Kısmi Tatil)",
                    HolidayType.PARTIAL_DAY,
                    0 // No automatic working hours - handled in calculation logic
                )
            )
        }
        
        return partialDays
    }
    
    /**
     * Check if a date is a holiday
     */
    fun isHoliday(date: LocalDate): Boolean {
        val holidays = getHolidaysForYear(date.year)
        return holidays.any { it.date == date }
    }
    
    /**
     * Get holiday info for a specific date
     */
    fun getHoliday(date: LocalDate): Holiday? {
        val holidays = getHolidaysForYear(date.year)
        return holidays.firstOrNull { it.date == date }
    }
    
    /**
     * Get working hours for a specific date considering holidays
     * Returns:
     * - 0 hours for full holidays and weekends
     * - 5 hours for half-day holidays (arife)
     * - 5 hours for partial holidays (like October 28)
     * - 8 hours for regular working days
     */
    fun getWorkingHoursForDate(date: LocalDate): Int {
        // Check if weekend
        if (date.dayOfWeek.value in 6..7) { // Saturday = 6, Sunday = 7
            return 0
        }
        
        // Check if holiday
        val holiday = getHoliday(date)
        return when (holiday?.type) {
            HolidayType.OFFICIAL, HolidayType.RELIGIOUS -> 0 // Full holiday
            HolidayType.HALF_DAY -> holiday.workingHours // 5 hours for arife days
            HolidayType.PARTIAL_DAY -> {
                // For October 28, return 8 hours (normal working day)
                // The overtime calculation will handle the 3-hour deduction
                8
            }
            null -> 8 // Regular working day
        }
    }
    
    /**
     * Get all holidays in a specific month
     */
    fun getHolidaysForMonth(year: Int, month: Month): List<Holiday> {
        return getHolidaysForYear(year).filter { 
            it.date.year == year && it.date.month == month 
        }
    }
}