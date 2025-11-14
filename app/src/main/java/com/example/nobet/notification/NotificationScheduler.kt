package com.example.nobet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.nobet.ui.calendar.ScheduleData
import com.example.nobet.ui.calendar.ShiftType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        const val PREFS_NAME = "notification_prefs"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_REMINDER_DAYS = "reminder_days"
        private const val TAG = "NotificationScheduler"
        
        // Default reminder times in days
        val DEFAULT_REMINDER_DAYS = listOf(1, 2, 3) // 1, 2, 3 days before
        
        // Takvim verilerini SharedPreferences'dan yükleyen fonksiyon
        fun loadScheduleFromPreferences(context: Context): Map<LocalDate, ShiftType> {
            val prefs: SharedPreferences = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
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
    
    fun scheduleShiftReminder(
        date: LocalDate,
        shiftType: ShiftType,
        reminderDays: Int = 1
    ): Boolean {
        try {
            if (!areNotificationsEnabled()) {
                Log.w(TAG, "Notifications disabled, skipping schedule for $date ($shiftType)")
                return false
            }
            
            // Check notification permissions
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Notification permission not granted for $date ($shiftType)")
                return false
            }
            
            // Check exact alarm permission for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact alarm permission not granted for $date ($shiftType)")
                    return false
                }
            }
            
            val shiftDateTime = getShiftStartTime(date, shiftType)
            val reminderTime = shiftDateTime.minusDays(reminderDays.toLong())
            
            // Don't schedule if the reminder time is in the past
            if (reminderTime.isBefore(LocalDateTime.now())) {
                Log.d(TAG, "Reminder time is in the past for $date ($shiftType) - $reminderDays days before, skipping")
                return false
            }
            
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_SHIFT_REMINDER
                putExtra(NotificationReceiver.EXTRA_SHIFT_TYPE, shiftType.name)
                putExtra(NotificationReceiver.EXTRA_REMINDER_DAYS, reminderDays)
                putExtra(NotificationReceiver.EXTRA_SHIFT_DATE, date.toString())
                putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, generateNotificationId(date, shiftType, reminderDays))
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                generateNotificationId(date, shiftType, reminderDays),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val triggerTime = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled reminder for $date ($shiftType) - $reminderDays days before at $reminderTime")
            return true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for scheduling alarm for $date ($shiftType)", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder for $date ($shiftType)", e)
            return false
        }
    }
    
    fun cancelShiftReminder(date: LocalDate, shiftType: ShiftType, reminderDays: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            generateNotificationId(date, shiftType, reminderDays),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
    
    fun scheduleAllRemindersForShift(date: LocalDate, shiftType: ShiftType): Boolean {
        // Skip notifications for annual leave and report days
        if (shiftType == ShiftType.ANNUAL_LEAVE || shiftType == ShiftType.REPORT) {
            return true // Consider as successful since no notifications needed
        }
        
        val reminderDaysList = getReminderDaysList()
        var successCount = 0
        val errors = mutableListOf<Exception>()
        
        for (days in reminderDaysList) {
            try {
                if (scheduleShiftReminder(date, shiftType, days)) {
                    successCount++
                }
            } catch (e: Exception) {
                errors.add(e)
                Log.e(TAG, "Failed to schedule reminder for $date ($shiftType) - $days days before", e)
            }
        }
        
        if (errors.isNotEmpty()) {
            Log.w(TAG, "Scheduled $successCount/${reminderDaysList.size} reminders for $date with ${errors.size} errors")
        } else {
            Log.d(TAG, "Scheduled $successCount/${reminderDaysList.size} reminders for $date")
        }
        
        return successCount > 0
    }
    
    fun cancelAllRemindersForShift(date: LocalDate, shiftType: ShiftType) {
        // Skip cancellation for annual leave and report days since they don't have notifications
        if (shiftType == ShiftType.ANNUAL_LEAVE || shiftType == ShiftType.REPORT) {
            return
        }
        
        for (days in DEFAULT_REMINDER_DAYS) {
            cancelShiftReminder(date, shiftType, days)
        }
    }
    
    fun rescheduleAllNotifications() {
        try {
            // Load schedule data and reschedule all active notifications
            val schedule = loadScheduleFromPreferences(context)
            
            // Cancel all existing notifications first
            for ((date, type) in schedule) {
                cancelAllRemindersForShift(date, type)
            }
            
            // Reschedule notifications if enabled
            if (areNotificationsEnabled()) {
                for ((date, type) in schedule) {
                    try {
                        scheduleAllRemindersForShift(date, type)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reschedule notification for $date ($type)", e)
                    }
                }
                Log.d(TAG, "Rescheduled ${schedule.size} shift notifications after boot")
            } else {
                Log.d(TAG, "Notifications disabled, skipping rescheduling after boot")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule notifications after boot", e)
        }
    }
    
    private fun getShiftStartTime(date: LocalDate, shiftType: ShiftType): LocalDateTime {
        return when (shiftType) {
            ShiftType.MORNING -> date.atTime(8, 0)  // 08:00
            ShiftType.NIGHT -> date.atTime(16, 0)   // 16:00
            ShiftType.FULL -> date.atTime(8, 0)     // 08:00 (24-hour shift)
            // Annual leave and report days don't have specific start times
            ShiftType.ANNUAL_LEAVE, ShiftType.REPORT -> date.atTime(8, 0) // Default time, won't be used
        }
    }
    
    private fun generateNotificationId(date: LocalDate, shiftType: ShiftType, reminderDays: Int): Int {
        // Daha güvenli ve çakışma olasılığı düşük ID generation
        val baseId = (date.toEpochDay() + shiftType.ordinal * 100L + reminderDays * 10000L).toInt()
        return (baseId and 0x7FFFFFFF) // Pozitif integer garantisi
    }
    
    private fun areNotificationsEnabled(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    private fun getReminderDaysList(): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderDays = prefs.getInt(KEY_REMINDER_DAYS, 1)
        
        // Schedule reminders for all default days up to the user's setting
        // Also include the user's specific setting to ensure at least one reminder
        val defaultReminders = DEFAULT_REMINDER_DAYS.filter { it <= reminderDays }
        return if (defaultReminders.isEmpty()) {
            listOf(reminderDays)
        } else {
            defaultReminders
        }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
    
    fun setReminderDays(days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REMINDER_DAYS, days).apply()
    }
    
    fun getReminderDays(): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REMINDER_DAYS, 1)
    }
    
    fun isNotificationEnabled(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check exact alarm permission", e)
                false
            }
        } else {
            true // Not required below API 31
        }
    }
    
    fun hasNotificationPermission(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    fun getPermissionStatus(): String {
        val notificationEnabled = hasNotificationPermission()
        val exactAlarmEnabled = canScheduleExactAlarms()
        val settingsEnabled = isNotificationEnabled()
        
        return "Notifications: $notificationEnabled, ExactAlarm: $exactAlarmEnabled, Settings: $settingsEnabled"
    }
}