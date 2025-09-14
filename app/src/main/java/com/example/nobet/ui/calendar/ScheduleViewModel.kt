package com.example.nobet.ui.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.nobet.notification.NotificationScheduler
import com.example.nobet.utils.TurkishHolidays
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class OvertimeResult(
    val workedHours: Int,
    val overtimeHours: Int,
    val workingDays: Int,
    val expectedHours: Int,
    val holidayHours: Int, // Hours from holidays (e.g., arife = 5 hours)
    val totalExpectedHours: Int // expectedHours + holidayHours
)

data class MonthlyStatistics(
    val month: YearMonth,
    val totalHours: Int,
    val overtimeResult: OvertimeResult,
    val shiftCounts: Map<ShiftType, Int>,
    val workingDayDistribution: Map<DayOfWeek, Int>
)

data class YearlyStatistics(
    val year: Int,
    val totalWorkedHours: Int,
    val totalOvertimeHours: Int,
    val totalExpectedHours: Int,
    val monthlyData: List<MonthlyStatistics>,
    val yearlyShiftCounts: Map<ShiftType, Int>,
    val yearlyWorkingDayDistribution: Map<DayOfWeek, Int>,
    val yearlyDayShiftDistribution: Map<DayOfWeek, Int>,
    val yearlyNightShiftDistribution: Map<DayOfWeek, Int>
)

class ScheduleViewModel : ViewModel() {
    private val gson = Gson()
    val schedule = mutableStateMapOf<LocalDate, ShiftType>()
    private var sharedPreferences: SharedPreferences? = null
    private var notificationScheduler: NotificationScheduler? = null
    private var shiftTypeConfig: ShiftTypeConfig? = null
    
    companion object {
        private const val PREFS_NAME = "nobet_schedule_prefs"
        private const val SCHEDULE_KEY = "schedule_data"
    }
    
    fun initializeWithContext(context: Context) {
        try {
            if (sharedPreferences == null) {
                sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            if (notificationScheduler == null) {
                notificationScheduler = NotificationScheduler(context)
            }
            if (shiftTypeConfig == null) {
                shiftTypeConfig = ShiftTypeConfig(context)
            }
            loadScheduleFromPrefs()
        } catch (e: Exception) {
            // Handle initialization error gracefully
            e.printStackTrace()
        }
    }
    
    private fun loadScheduleFromPrefs() {
        try {
            sharedPreferences?.getString(SCHEDULE_KEY, null)?.let { json ->
                fromJson(json)
            }
        } catch (e: Exception) {
            // Handle load error gracefully - keep existing schedule if any
            e.printStackTrace()
        }
    }
    
    private fun saveScheduleToPrefs() {
        try {
            sharedPreferences?.edit()?.putString(SCHEDULE_KEY, toJson())?.apply()
        } catch (e: Exception) {
            // Handle save error gracefully
            e.printStackTrace()
        }
    }

    fun set(date: LocalDate, type: ShiftType?) {
        try {
            val oldType = schedule[date]
            
            if (type == null) {
                schedule.remove(date)
                // Cancel notifications for removed shift
                oldType?.let { 
                    try {
                        notificationScheduler?.cancelAllRemindersForShift(date, it)
                    } catch (e: Exception) {
                        // Handle notification cancellation error gracefully
                    }
                }
            } else {
                schedule[date] = type
                // Cancel old notifications and schedule new ones
                oldType?.let { 
                    try {
                        notificationScheduler?.cancelAllRemindersForShift(date, it)
                    } catch (e: Exception) {
                        // Handle notification cancellation error gracefully
                    }
                }
                try {
                    notificationScheduler?.scheduleAllRemindersForShift(date, type)
                } catch (e: Exception) {
                    // Handle notification scheduling error gracefully
                }
            }
            
            saveScheduleToPrefs() // Auto-save after each change
        } catch (e: Exception) {
            // Handle general set operation error gracefully
            e.printStackTrace()
        }
    }

    fun toJson(): String {
        val map = schedule.mapKeys { it.key.toString() }
        return gson.toJson(map)
    }

    fun fromJson(json: String) {
        try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val parsed: Map<String, String> = gson.fromJson(json, type)
            val mapped = mutableMapOf<LocalDate, ShiftType>()

            parsed.forEach { (key, value) ->
                try {
                    val date = LocalDate.parse(key)
                    val shiftType = when (value) {
                        "MORNING" -> ShiftType.MORNING
                        "NIGHT" -> ShiftType.NIGHT
                        "FULL" -> ShiftType.FULL
                        else -> null
                    }
                    if (shiftType != null) {
                        mapped[date] = shiftType
                    }
                } catch (e: Exception) {
                    // Geçersiz veri varsa yoksay
                }
            }

            schedule.clear()
            schedule.putAll(mapped)
            saveScheduleToPrefs() // Save after importing
        } catch (e: Exception) {
            // JSON parse hatası durumunda hiçbir şey yapma
        }
    }

    fun totalFor(month: YearMonth): Int {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .sumOf { date ->
                val shiftHours = schedule[date]?.let { shiftType -> 
                    getHoursForShiftType(shiftType)
                } ?: 0
                
                // Apply special calculation for arife (holiday eve) days
                if (shiftHours > 0) {
                    val holidayInfo = getHolidayInfo(date)
                    if (holidayInfo?.type == TurkishHolidays.HolidayType.HALF_DAY) {
                        // On arife days, any shift counts as maximum 5 hours
                        minOf(shiftHours, 5)
                    } else {
                        shiftHours
                    }
                } else {
                    0
                }
            }
    }

    fun overtimeFor(month: YearMonth): Int {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()

        val normalHours = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .sumOf { date ->
                TurkishHolidays.getWorkingHoursForDate(date)
            }

        val realHours = totalFor(month)
        return (realHours - normalHours).coerceAtLeast(0)
    }

    fun calculateOvertime(month: YearMonth): OvertimeResult {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()

        // Count actual working days (excluding weekends and full holidays)
        val workingDays = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .count { date ->
                val workingHours = TurkishHolidays.getWorkingHoursForDate(date)
                workingHours == 8 // Only count full working days
            }

        // Calculate holiday hours (from arife days)
        val holidayHours = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .sumOf { date ->
                val workingHours = TurkishHolidays.getWorkingHoursForDate(date)
                if (workingHours > 0 && workingHours < 8) workingHours else 0 // Only partial work days
            }

        // Calculate total expected hours for the month (including both full and partial working days)
        val totalExpectedHours = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .sumOf { date ->
                TurkishHolidays.getWorkingHoursForDate(date)
            }

        val workedHours = totalFor(month)
        val expectedHours = workingDays * 8
        val overtimeHours = (workedHours - totalExpectedHours).coerceAtLeast(0)

        return OvertimeResult(
            workedHours = workedHours,
            overtimeHours = overtimeHours,
            workingDays = workingDays,
            expectedHours = expectedHours,
            holidayHours = holidayHours,
            totalExpectedHours = totalExpectedHours
        )
    }
    
    // Notification management methods
    fun setNotificationsEnabled(enabled: Boolean) {
        notificationScheduler?.setNotificationsEnabled(enabled)
        if (enabled) {
            // Re-schedule all existing shifts
            schedule.forEach { (date, type) ->
                try {
                    notificationScheduler?.scheduleAllRemindersForShift(date, type)
                } catch (e: Exception) {
                    // Handle notification scheduling error gracefully
                    e.printStackTrace()
                }
            }
        } else {
            // Cancel all existing notifications
            schedule.forEach { (date, type) ->
                try {
                    notificationScheduler?.cancelAllRemindersForShift(date, type)
                } catch (e: Exception) {
                    // Handle notification cancellation error gracefully
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun areNotificationsEnabled(): Boolean {
        return notificationScheduler?.isNotificationEnabled() ?: false
    }
    
    fun setReminderDays(days: Int) {
        notificationScheduler?.setReminderDays(days)
        // Reschedule all notifications with new days
        if (areNotificationsEnabled()) {
            schedule.forEach { (date, type) ->
                try {
                    notificationScheduler?.cancelAllRemindersForShift(date, type)
                    notificationScheduler?.scheduleAllRemindersForShift(date, type)
                } catch (e: Exception) {
                    // Handle notification rescheduling error gracefully
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun getReminderDays(): Int {
        return notificationScheduler?.getReminderDays() ?: 1
    }
    
    // Holiday helper methods
    fun getHolidaysForMonth(month: YearMonth): List<TurkishHolidays.Holiday> {
        return TurkishHolidays.getHolidaysForMonth(month.year, month.month)
    }
    
    fun isHoliday(date: LocalDate): Boolean {
        return TurkishHolidays.isHoliday(date)
    }
    
    fun getHolidayInfo(date: LocalDate): TurkishHolidays.Holiday? {
        return TurkishHolidays.getHoliday(date)
    }
    
    // ShiftType configuration methods
    fun getShiftTypeConfig(): ShiftTypeConfig? {
        return shiftTypeConfig
    }
    
    fun getCurrentShiftTypes(): List<DynamicShiftType> {
        return shiftTypeConfig?.getCurrentShiftTypes() ?: emptyList()
    }
    
    fun getHoursForShiftType(shiftType: ShiftType): Int {
        return when (shiftType) {
            ShiftType.MORNING -> shiftTypeConfig?.getMorningHours() ?: ShiftTypeConfig.DEFAULT_MORNING_HOURS
            ShiftType.NIGHT -> shiftTypeConfig?.getNightHours() ?: ShiftTypeConfig.DEFAULT_NIGHT_HOURS
            ShiftType.FULL -> shiftTypeConfig?.getFullHours() ?: ShiftTypeConfig.DEFAULT_FULL_HOURS
        }
    }
    
    fun getLabelForShiftType(shiftType: ShiftType): String {
        return when (shiftType) {
            ShiftType.MORNING -> shiftTypeConfig?.getMorningLabel() ?: ShiftTypeConfig.DEFAULT_MORNING_LABEL
            ShiftType.NIGHT -> shiftTypeConfig?.getNightLabel() ?: ShiftTypeConfig.DEFAULT_NIGHT_LABEL
            ShiftType.FULL -> shiftTypeConfig?.getFullLabel() ?: ShiftTypeConfig.DEFAULT_FULL_LABEL
        }
    }
    
    /**
     * Get effective working hours for a shift on a specific date
     * Applies arife day special rule: maximum 5 hours on holiday eves
     */
    fun getEffectiveHoursForDate(date: LocalDate): Int {
        val shift = schedule[date] ?: return 0
        val normalHours = getHoursForShiftType(shift)
        
        val holidayInfo = getHolidayInfo(date)
        return if (holidayInfo?.type == TurkishHolidays.HolidayType.HALF_DAY) {
            // On arife days, any shift counts as maximum 5 hours
            minOf(normalHours, 5)
        } else {
            normalHours
        }
    }
    
    /**
     * Check if a date has the arife day special rule applied
     */
    fun isArifeDayRule(date: LocalDate): Boolean {
        if (schedule[date] == null) return false
        val holidayInfo = getHolidayInfo(date)
        return holidayInfo?.type == TurkishHolidays.HolidayType.HALF_DAY && 
               getHoursForShiftType(schedule[date]!!) > 5
    }
    
    // Bulk operations for settings
    fun addWorkingDaysToMonth(month: YearMonth, shiftType: ShiftType) {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .filter { date ->
                // Only add to weekdays that are not holidays and don't already have shifts
                val workingHours = TurkishHolidays.getWorkingHoursForDate(date)
                workingHours == 8 && !schedule.containsKey(date) // Full working days without existing shifts
            }
            .forEach { date ->
                set(date, shiftType)
            }
    }
    
    fun clearAllShiftsInMonth(month: YearMonth) {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        
        val keysToRemove = schedule.keys.filter { date ->
            date >= start && date <= end
        }
        
        keysToRemove.forEach { date ->
            set(date, null)
        }
    }
    
    // Yearly statistics methods
    fun calculateYearlyStatistics(year: Int): YearlyStatistics {
        val yearMonths = (1..12).map { YearMonth.of(year, it) }
        
        val monthlyData = yearMonths.map { month ->
            MonthlyStatistics(
                month = month,
                totalHours = totalFor(month),
                overtimeResult = calculateOvertime(month),
                shiftCounts = getShiftCountsForMonth(month),
                workingDayDistribution = getWorkingDayDistributionForMonth(month)
            )
        }
        
        val totalWorkedHours = monthlyData.sumOf { it.totalHours }
        val totalOvertimeHours = monthlyData.sumOf { it.overtimeResult.overtimeHours }
        val totalExpectedHours = monthlyData.sumOf { it.overtimeResult.totalExpectedHours }
        
        val yearlyShiftCounts = mutableMapOf<ShiftType, Int>()
        ShiftType.values().forEach { shiftType ->
            yearlyShiftCounts[shiftType] = monthlyData.sumOf { it.shiftCounts[shiftType] ?: 0 }
        }
        
        val yearlyWorkingDayDistribution = mutableMapOf<DayOfWeek, Int>()
        DayOfWeek.values().forEach { dayOfWeek ->
            yearlyWorkingDayDistribution[dayOfWeek] = monthlyData.sumOf { 
                it.workingDayDistribution[dayOfWeek] ?: 0 
            }
        }
        
        // Calculate yearly day shift distribution (08-16)
        val yearlyDayShiftDistribution = mutableMapOf<DayOfWeek, Int>()
        val yearShifts = schedule.filter { (date, _) -> date.year == year }
        DayOfWeek.values().forEach { dayOfWeek ->
            yearlyDayShiftDistribution[dayOfWeek] = yearShifts.filter { (date, type) -> 
                date.dayOfWeek == dayOfWeek && type == ShiftType.MORNING 
            }.size
        }
        
        // Calculate yearly night shift distribution (16-08 & 08-08)
        val yearlyNightShiftDistribution = mutableMapOf<DayOfWeek, Int>()
        DayOfWeek.values().forEach { dayOfWeek ->
            yearlyNightShiftDistribution[dayOfWeek] = yearShifts.filter { (date, type) -> 
                date.dayOfWeek == dayOfWeek && (type == ShiftType.NIGHT || type == ShiftType.FULL)
            }.size
        }
        
        return YearlyStatistics(
            year = year,
            totalWorkedHours = totalWorkedHours,
            totalOvertimeHours = totalOvertimeHours,
            totalExpectedHours = totalExpectedHours,
            monthlyData = monthlyData,
            yearlyShiftCounts = yearlyShiftCounts,
            yearlyWorkingDayDistribution = yearlyWorkingDayDistribution,
            yearlyDayShiftDistribution = yearlyDayShiftDistribution,
            yearlyNightShiftDistribution = yearlyNightShiftDistribution
        )
    }
    
    private fun getShiftCountsForMonth(month: YearMonth): Map<ShiftType, Int> {
        val monthShifts = schedule.filter { (date, _) -> YearMonth.from(date) == month }
        return mapOf(
            ShiftType.MORNING to monthShifts.filter { (_, type) -> type == ShiftType.MORNING }.size,
            ShiftType.NIGHT to monthShifts.filter { (_, type) -> type == ShiftType.NIGHT }.size,
            ShiftType.FULL to monthShifts.filter { (_, type) -> type == ShiftType.FULL }.size
        )
    }
    
    private fun getWorkingDayDistributionForMonth(month: YearMonth): Map<DayOfWeek, Int> {
        val monthShifts = schedule.filter { (date, _) -> YearMonth.from(date) == month }
        return DayOfWeek.values().associateWith { dayOfWeek ->
            monthShifts.filter { (date, _) -> date.dayOfWeek == dayOfWeek }.size
        }
    }
}