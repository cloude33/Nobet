package com.example.nobet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import com.example.nobet.ui.calendar.ShiftType

class NotificationScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        const val PREFS_NAME = "notification_prefs"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_REMINDER_DAYS = "reminder_days"
        private const val TAG = "NotificationScheduler"
        
        // Default reminder times in days
        val DEFAULT_REMINDER_DAYS = listOf(1, 2, 3) // 1, 2, 3 days before
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
        val reminderDaysList = getReminderDaysList()
        var successCount = 0
        val errors = mutableListOf<Exception>()
        
        reminderDaysList.forEach { days ->
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
        DEFAULT_REMINDER_DAYS.forEach { days ->
            cancelShiftReminder(date, shiftType, days)
        }
    }
    
    fun rescheduleAllNotifications() {
        // This would be called after boot completion
        // For now, we'll leave it empty as it requires access to the schedule data
        // In a real implementation, you would reload the schedule and reschedule active notifications
    }
    
    private fun getShiftStartTime(date: LocalDate, shiftType: ShiftType): LocalDateTime {
        return when (shiftType) {
            ShiftType.MORNING -> date.atTime(8, 0)  // 08:00
            ShiftType.NIGHT -> date.atTime(16, 0)   // 16:00
            ShiftType.FULL -> date.atTime(8, 0)     // 08:00 (24-hour shift)
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
    
    // Kullanıcı ayarına göre filtrele
    return DEFAULT_REMINDER_DAYS.filter { it <= reminderDays }
        .ifEmpty { listOf(reminderDays) } // En az bir reminder garantisi
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