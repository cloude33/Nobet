package com.example.nobet.ui.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.nobet.notification.NotificationScheduler
import com.example.nobet.utils.TurkishHolidays
import com.example.nobet.utils.WorkHourCalculator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import com.example.nobet.ui.calendar.CustomShiftType

// Data sınıfları...
data class OvertimeResult(
    val workedHours: Int,
    val overtimeHours: Int,
    val workingDays: Int,
    val expectedHours: Int,
    val holidayHours: Int,
    val totalExpectedHours: Int
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

// New data class for annual leave tracking
data class AnnualLeaveSettings(
    val totalAnnualLeaveDays: Int = 30,
    val usedAnnualLeaveDays: Int = 0
) {
    val remainingAnnualLeaveDays: Int
        get() = totalAnnualLeaveDays - usedAnnualLeaveDays
}

data class ScheduleData(
    val schedule: Map<String, String>,
    val annualLeaveSettings: AnnualLeaveSettings
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
        private const val ANNUAL_LEAVE_TOTAL_KEY = "annual_leave_total"
        private const val ANNUAL_LEAVE_USED_KEY = "annual_leave_used"
        
        // Takvim verilerini SharedPreferences'dan yükleyen fonksiyon
        fun loadScheduleFromPreferences(context: Context): Map<LocalDate, ShiftType> {
            val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
            val scheduleJson = prefs.getString("schedule_data", null) ?: return emptyMap()
            
            try {
                val gson = Gson()
                
                // Try to parse as new ScheduleData format first
                try {
                    val scheduleData: ScheduleData = gson.fromJson(scheduleJson, ScheduleData::class.java)
                    val stringMap: Map<String, String> = scheduleData.schedule
                    
                    return stringMap.mapKeys { (dateStr, _) ->
                        LocalDate.parse(dateStr)
                    }.mapValues { (_, shiftTypeStr) ->
                        ShiftType.valueOf(shiftTypeStr)
                    }
                } catch (e: Exception) {
                    // Fallback to old format for backward compatibility
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val stringMap: Map<String, String> = gson.fromJson(scheduleJson, type)
                    
                    return stringMap.mapKeys { (dateStr, _) ->
                        LocalDate.parse(dateStr)
                    }.mapValues { (_, shiftTypeStr) ->
                        ShiftType.valueOf(shiftTypeStr)
                    }
                }
            } catch (e: Exception) {
                return emptyMap()
            }
        }
    }
    
    // Annual leave settings
    var annualLeaveSettings by mutableStateOf(AnnualLeaveSettings())
    
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
            loadAnnualLeaveSettings()
            // Automatically update used annual leave days based on schedule
            updateUsedAnnualLeaveFromSchedule()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // New function to calculate used annual leave days from schedule
    private fun updateUsedAnnualLeaveFromSchedule() {
        try {
            val annualLeaveCount = schedule.values.count { it == ShiftType.ANNUAL_LEAVE }
            // Only update if the count is different from current used days
            if (annualLeaveCount != annualLeaveSettings.usedAnnualLeaveDays) {
                annualLeaveSettings = annualLeaveSettings.copy(usedAnnualLeaveDays = annualLeaveCount)
                saveAnnualLeaveSettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadAnnualLeaveSettings() {
        try {
            sharedPreferences?.let { prefs ->
                val totalAnnualLeave = prefs.getInt(ANNUAL_LEAVE_TOTAL_KEY, 30)
                val usedAnnualLeave = prefs.getInt(ANNUAL_LEAVE_USED_KEY, 0)
                annualLeaveSettings = AnnualLeaveSettings(totalAnnualLeave, usedAnnualLeave)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveAnnualLeaveSettings() {
        try {
            sharedPreferences?.edit()
                ?.putInt(ANNUAL_LEAVE_TOTAL_KEY, annualLeaveSettings.totalAnnualLeaveDays)
                ?.putInt(ANNUAL_LEAVE_USED_KEY, annualLeaveSettings.usedAnnualLeaveDays)
                ?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    

    
    fun setTotalAnnualLeaveDays(days: Int) {
        annualLeaveSettings = annualLeaveSettings.copy(totalAnnualLeaveDays = days)
        saveAnnualLeaveSettings()
    }
    
    fun addUsedAnnualLeaveDays(days: Int) {
        annualLeaveSettings = annualLeaveSettings.copy(usedAnnualLeaveDays = annualLeaveSettings.usedAnnualLeaveDays + days)
        saveAnnualLeaveSettings()
    }
    
    fun resetUsedAnnualLeaveDays() {
        annualLeaveSettings = annualLeaveSettings.copy(usedAnnualLeaveDays = 0)
        saveAnnualLeaveSettings()
    }
    
    private fun loadScheduleFromPrefs() {
        try {
            sharedPreferences?.getString(SCHEDULE_KEY, null)?.let { json ->
                fromJson(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveScheduleToPrefs() {
        try {
            val stringMap = schedule.mapKeys { it.key.toString() }.mapValues { it.value.name }
            val scheduleData = ScheduleData(
                schedule = stringMap,
                annualLeaveSettings = annualLeaveSettings
            )
            sharedPreferences?.edit()?.putString(SCHEDULE_KEY, gson.toJson(scheduleData))?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun set(date: LocalDate, type: ShiftType?) {
        try {
            val oldType = schedule[date]
            if (type == null) {
                schedule.remove(date)
                // If we're removing an annual leave day, decrease the count
                if (oldType == ShiftType.ANNUAL_LEAVE) {
                    addUsedAnnualLeaveDays(-1)
                }
                oldType?.let { try { notificationScheduler?.cancelAllRemindersForShift(date, it) } catch (e: Exception) {} }
            } else {
                schedule[date] = type
                // If we're adding an annual leave day, increase the count
                if (type == ShiftType.ANNUAL_LEAVE && oldType != ShiftType.ANNUAL_LEAVE) {
                    addUsedAnnualLeaveDays(1)
                }
                // If we're changing from annual leave to something else, decrease the count
                if (oldType == ShiftType.ANNUAL_LEAVE && type != ShiftType.ANNUAL_LEAVE) {
                    addUsedAnnualLeaveDays(-1)
                }
                oldType?.let { try { notificationScheduler?.cancelAllRemindersForShift(date, it) } catch (e: Exception) {} }
                try { notificationScheduler?.scheduleAllRemindersForShift(date, type) } catch (e: Exception) {}
            }
            saveScheduleToPrefs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toJson(): String {
        val stringMap = schedule.mapKeys { it.key.toString() }.mapValues { it.value.name }
        val scheduleData = ScheduleData(
            schedule = stringMap,
            annualLeaveSettings = annualLeaveSettings
        )
        return gson.toJson(scheduleData)
    }

    fun fromJson(json: String) {
        try {
            val scheduleData: ScheduleData = gson.fromJson(json, ScheduleData::class.java)
            
            // Restore schedule
            val mapped = mutableMapOf<LocalDate, ShiftType>()
            scheduleData.schedule.forEach { (key, value) ->
                try {
                    val date = LocalDate.parse(key)
                    val shiftType = ShiftType.valueOf(value)
                    mapped[date] = shiftType
                } catch (e: Exception) {}
            }
            schedule.clear()
            schedule.putAll(mapped)
            
            // Restore annual leave settings
            annualLeaveSettings = scheduleData.annualLeaveSettings
            
            saveScheduleToPrefs()
            saveAnnualLeaveSettings()
            // Update used annual leave days based on restored schedule
            updateUsedAnnualLeaveFromSchedule()
        } catch (e: Exception) {
            // Fallback to old format for backward compatibility
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val parsed: Map<String, String> = gson.fromJson(json, type)
                val mapped = mutableMapOf<LocalDate, ShiftType>()
                parsed.forEach { (key, value) ->
                    try {
                        val date = LocalDate.parse(key)
                        val shiftType = ShiftType.valueOf(value)
                        mapped[date] = shiftType
                    } catch (e: Exception) {}
                }
                schedule.clear()
                schedule.putAll(mapped)
                saveScheduleToPrefs()
                // Update used annual leave days based on restored schedule
                updateUsedAnnualLeaveFromSchedule()
            } catch (e2: Exception) {}
        }
    }

    // Sadece girilen nöbetlerin ham saat toplamını döndürür.
    fun totalFor(month: YearMonth): Int {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .sumOf { date -> 
                schedule[date]?.let { shiftType ->
                    // For annual leave and report days, return 0 hours worked
                    if (shiftType == ShiftType.ANNUAL_LEAVE || shiftType == ShiftType.REPORT) {
                        0
                    } else {
                        getHoursForShiftType(shiftType, date)
                    }
                } ?: 0
            }
    }
    
    // Özel nöbet türü oluşturma ve kaydetme
    fun setCustomShift(date: LocalDate, id: String, label: String, hours: Int, color: androidx.compose.ui.graphics.Color) {
        val customShift = CustomShiftType(id, label, hours, color)
        schedule[date] = ShiftType.MORNING // Özel nöbetleri MORNING olarak kaydediyoruz
        
        // SharedPreferences'e özel nöbet bilgilerini kaydet
        val customShiftsKey = "custom_shift_$date"
        val customShiftJson = gson.toJson(customShift)
        sharedPreferences?.edit()?.putString(customShiftsKey, customShiftJson)?.apply()
        
        saveScheduleToPrefs()
    }

    // Fazla mesaiyi "aylık kota" mantığına göre hesaplayan son ve doğru fonksiyon
    fun calculateOvertime(month: YearMonth): OvertimeResult {
        // 1. Ay boyunca tutulan tüm nöbetlerin ham saat toplamını al.
        val workedHours = totalFor(month)

        // 2. O ay çalışılması gereken toplam zorunlu saati hesapla (örn: Ekim için 173).
        val totalExpectedHours = month.atDay(1).let { start ->
            val end = month.atEndOfMonth()
            generateSequence(start) { it.plusDays(1) }
                .takeWhile { !it.isAfter(end) }
                .sumOf { date -> 
                    // Use the new method that considers shift types
                    schedule[date]?.let { shiftType ->
                        WorkHourCalculator.getExpectedWorkHoursWithShift(date, shiftType)
                    } ?: WorkHourCalculator.getExpectedWorkHours(date)
                }
        }

        // 3. Fazla mesaiyi hesapla: Toplam çalışılan saat, zorunlu saati aşıyorsa aradaki farktır.
        val overtimeHours = (workedHours - totalExpectedHours).coerceAtLeast(0)

        // Tatil günlerini hesapla
        val holidayHours = 0 // Basitleştirilmiş hesaplama

        // Sonuçları doğru şekilde döndür.
        return OvertimeResult(
            workedHours = workedHours,
            overtimeHours = overtimeHours,
            workingDays = month.lengthOfMonth() - (month.atDay(1).let { start ->
                val end = month.atEndOfMonth()
                generateSequence(start) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(end) }
                    .count { 
                        // Also consider annual leave and report days as non-working days
                        it.dayOfWeek == DayOfWeek.SATURDAY || 
                        it.dayOfWeek == DayOfWeek.SUNDAY ||
                        schedule[it] == ShiftType.ANNUAL_LEAVE ||
                        schedule[it] == ShiftType.REPORT ||
                        WorkHourCalculator.getExpectedWorkHours(it) == 0
                    }
            }),
            expectedHours = totalExpectedHours,
            holidayHours = holidayHours,
            totalExpectedHours = totalExpectedHours
        )
    }
    
    fun getMonthlyStatistics(month: YearMonth): MonthlyStatistics {
        val workedHours = totalFor(month)
        val overtime = calculateOvertime(month)
        
        return MonthlyStatistics(
            month = month,
            totalHours = workedHours,
            overtimeResult = overtime,
            shiftCounts = getShiftCountsForMonth(month),
            workingDayDistribution = getWorkingDayDistributionForMonth(month)
        )
    }

    /**
     * Bir tarihin, fazla mesai hesaplamasını etkileyen kısmi veya yarım gün bir tatil olup olmadığını kontrol eder.
     * Sadece o güne bir nöbet girilmişse ve o gün zorunlu mesai varsa (0'dan büyük ama 8'den küçük) true döner.
     */
    fun isSpecialHolidayRule(date: LocalDate): Boolean {
        if (!schedule.containsKey(date)) return false
        val expectedWork = WorkHourCalculator.getExpectedWorkHours(date)
        // Eğer o gün zorunlu mesai varsa (0 < saat < 8) ve nöbet tutulmuşsa, bu özel bir kuraldır.
        return expectedWork > 0 && expectedWork < 8
    }

    /**
     * Bir tarihteki nöbetin, o günkü fazla mesaiye ne kadar katkı sağladığını hesaplar.
     * NOT: Bu fonksiyon artık ana hesaplamada kullanılmıyor, sadece UI'da detay göstermek için var.
     * Ana hesaplama aylık kotaya göre yapılır.
     */
    fun getEffectiveHoursForDate(date: LocalDate): Int {
        if (!schedule.containsKey(date)) return 0
        val shift = schedule[date] ?: return 0
        
        // For annual leave and report days, return 0 effective hours
        if (shift == ShiftType.ANNUAL_LEAVE || shift == ShiftType.REPORT) {
            return 0
        }
        
        val shiftHours = getHoursForShiftType(shift)
        val expectedWork = WorkHourCalculator.getExpectedWorkHours(date)
        // Fazla mesai = Çalışılan Saat - O Gün Beklenen Saat
        return (shiftHours - expectedWork).coerceAtLeast(0)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationScheduler?.setNotificationsEnabled(enabled)
        if (enabled) {
            schedule.forEach { (date, type) -> try { notificationScheduler?.scheduleAllRemindersForShift(date, type) } catch (e: Exception) { e.printStackTrace() } }
        } else {
            schedule.forEach { (date, type) -> try { notificationScheduler?.cancelAllRemindersForShift(date, type) } catch (e: Exception) { e.printStackTrace() } }
        }
    }

    fun areNotificationsEnabled(): Boolean = notificationScheduler?.isNotificationEnabled() ?: false

    fun setReminderDays(days: Int) {
        notificationScheduler?.setReminderDays(days)
        if (areNotificationsEnabled()) {
            schedule.forEach { (date, type) ->
                try {
                    notificationScheduler?.cancelAllRemindersForShift(date, type)
                    notificationScheduler?.scheduleAllRemindersForShift(date, type)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun getReminderDays(): Int = notificationScheduler?.getReminderDays() ?: 1
    fun getHolidaysForMonth(month: YearMonth): List<TurkishHolidays.Holiday> = TurkishHolidays.getHolidaysForMonth(month.year, month.month)
    fun isHoliday(date: LocalDate): Boolean = TurkishHolidays.isHoliday(date)
    fun getHolidayInfo(date: LocalDate): TurkishHolidays.Holiday? = TurkishHolidays.getHoliday(date)
    fun getShiftTypeConfig(): ShiftTypeConfig? = shiftTypeConfig
    fun getCurrentShiftTypes(): List<DynamicShiftType> = shiftTypeConfig?.getAllShiftTypes() ?: emptyList()
    
    // Custom shift type management methods
    fun addCustomShiftType(label: String, hours: Int, color: androidx.compose.ui.graphics.Color) {
        val id = java.util.UUID.randomUUID().toString()
        val customShift = CustomShiftType(id, label, hours, color)
        shiftTypeConfig?.addCustomShiftType(customShift)
    }
    
    fun removeCustomShiftType(id: String) {
        shiftTypeConfig?.removeCustomShiftType(id)
    }
    
    fun getHoursForShiftType(shiftType: ShiftType, date: LocalDate = LocalDate.now()): Int {
        // For annual leave and report days, return 0 hours
        if (shiftType == ShiftType.ANNUAL_LEAVE || shiftType == ShiftType.REPORT) {
            return 0
        }
        
        // Özel nöbet türü kontrolü
        if (shiftType == ShiftType.MORNING) {
            val customShiftsKey = "custom_shift_$date"
            val customShiftJson = sharedPreferences?.getString(customShiftsKey, null)
            if (customShiftJson != null) {
                try {
                    val customShift: CustomShiftType = gson.fromJson(customShiftJson, CustomShiftType::class.java)
                    return customShift.hours
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        return when (shiftType) {
            ShiftType.MORNING -> shiftTypeConfig?.getMorningHours() ?: ShiftTypeConfig.DEFAULT_MORNING_HOURS
            ShiftType.NIGHT -> shiftTypeConfig?.getNightHours() ?: ShiftTypeConfig.DEFAULT_NIGHT_HOURS
            ShiftType.FULL -> shiftTypeConfig?.getFullHours() ?: ShiftTypeConfig.DEFAULT_FULL_HOURS
            // These should never be reached due to the early return above, but added for completeness
            ShiftType.ANNUAL_LEAVE, ShiftType.REPORT -> 0
        }
    }

    fun getLabelForShiftType(shiftType: ShiftType): String {
        return when (shiftType) {
            ShiftType.MORNING -> shiftTypeConfig?.getMorningLabel() ?: ShiftTypeConfig.DEFAULT_MORNING_LABEL
            ShiftType.NIGHT -> shiftTypeConfig?.getNightLabel() ?: ShiftTypeConfig.DEFAULT_NIGHT_LABEL
            ShiftType.FULL -> shiftTypeConfig?.getFullLabel() ?: ShiftTypeConfig.DEFAULT_FULL_LABEL
            ShiftType.ANNUAL_LEAVE -> "Yıllık İzin"
            ShiftType.REPORT -> "Rapor"
        }
    }

    fun addWorkingDaysToMonth(month: YearMonth, shiftType: ShiftType) {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .filter { date ->
                val workingHours = WorkHourCalculator.getExpectedWorkHours(date)
                workingHours == 8 && !schedule.containsKey(date)
            }
            .forEach { date -> set(date, shiftType) }
    }

    fun clearAllShiftsInMonth(month: YearMonth) {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val keysToRemove = schedule.keys.filter { date -> date >= start && date <= end }
        keysToRemove.forEach { date -> set(date, null) }
    }

    fun calculateYearlyStatistics(year: Int): YearlyStatistics {
        val yearStart = YearMonth.of(year, 1)
        val yearEnd = YearMonth.of(year, 12)
        
        val monthlyStats = (1..12).map { month ->
            getMonthlyStatistics(YearMonth.of(year, month))
        }
        
        val totalWorkedHours = monthlyStats.sumOf { it.totalHours }
        val totalOvertimeHours = monthlyStats.sumOf { it.overtimeResult.overtimeHours }
        val totalExpectedHours = monthlyStats.sumOf { it.overtimeResult.expectedHours }
        
        // Yıllık vardiya dağılımı
        val yearlyShiftCounts = ShiftType.values().associateWith { shiftType ->
            monthlyStats.sumOf { it.shiftCounts[shiftType] ?: 0 }
        }
        
        // Yıllık çalışma günü dağılımı
        val yearlyWorkingDayDistribution = DayOfWeek.values().associateWith { dayOfWeek ->
            monthlyStats.sumOf { it.workingDayDistribution[dayOfWeek] ?: 0 }
        }
        
        // Gündüz vardiyalarının günlere göre dağılımı
        val yearlyDayShiftDistribution = DayOfWeek.values().associateWith { dayOfWeek ->
            yearStart.atDay(1).let { start ->
                val end = yearEnd.atEndOfMonth()
                generateSequence(start) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(end) }
                    .count { date -> date.dayOfWeek == dayOfWeek && schedule[date] == ShiftType.MORNING }
            }
        }
        
        // Gece vardiyalarının günlere göre dağılımı
        val yearlyNightShiftDistribution = DayOfWeek.values().associateWith { dayOfWeek ->
            yearStart.atDay(1).let { start ->
                val end = yearEnd.atEndOfMonth()
                generateSequence(start) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(end) }
                    .count { date -> date.dayOfWeek == dayOfWeek && (schedule[date] == ShiftType.NIGHT || schedule[date] == ShiftType.FULL) }
            }
        }
        
        return YearlyStatistics(
            year = year,
            totalWorkedHours = totalWorkedHours,
            totalOvertimeHours = totalOvertimeHours,
            totalExpectedHours = totalExpectedHours,
            monthlyData = monthlyStats,
            yearlyShiftCounts = yearlyShiftCounts,
            yearlyWorkingDayDistribution = yearlyWorkingDayDistribution,
            yearlyDayShiftDistribution = yearlyDayShiftDistribution,
            yearlyNightShiftDistribution = yearlyNightShiftDistribution
        )
    }

    private fun getShiftCountsForMonth(month: YearMonth): Map<ShiftType, Int> {
        val monthShifts = schedule.filter { (date, _) -> YearMonth.from(date) == month }
        return ShiftType.entries.associateWith { shiftType ->
            monthShifts.count { (_, type) -> type == shiftType }
        }
    }

    private fun getWorkingDayDistributionForMonth(month: YearMonth): Map<DayOfWeek, Int> {
        val monthShifts = schedule.filter { (date, _) -> YearMonth.from(date) == month }
        return DayOfWeek.entries.associateWith { dayOfWeek ->
            monthShifts.count { (date, _) -> date.dayOfWeek == dayOfWeek }
        }
    }
}