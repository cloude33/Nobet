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
                Log.w(TAG, "Notifications disabled, skipping schedule")
                return false
            }
            
            // Check notification permissions
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Notification permission not granted")
                return false
            }
            
            // Check exact alarm permission for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact alarm permission not granted")
                    return false
                }
            }
            
            val shiftDateTime = getShiftStartTime(date, shiftType)
            val reminderTime = shiftDateTime.minusDays(reminderDays.toLong())
            
            // Don't schedule if the reminder time is in the past
            if (reminderTime.isBefore(LocalDateTime.now())) {
                Log.d(TAG, "Reminder time is in the past, skipping")
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
            
            Log.d(TAG, "Scheduled reminder for $date ($shiftType) - $reminderDays days before")
            return true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for scheduling alarm", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder", e)
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
        reminderDaysList.forEach { days ->
            if (scheduleShiftReminder(date, shiftType, days)) {
                successCount++
            }
        }
        Log.d(TAG, "Scheduled $successCount/${reminderDaysList.size} reminders for $date")
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
        // Generate unique ID based on date, shift type, and reminder days
        return (date.hashCode() + shiftType.hashCode() + reminderDays).let {
            if (it < 0) -it else it
        } % 100000 + NotificationHelper.NOTIFICATION_ID_BASE
    }
    
    private fun areNotificationsEnabled(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    private fun getReminderDaysList(): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderDays = prefs.getInt(KEY_REMINDER_DAYS, 1)
        return listOf(reminderDays) // For simplicity, using single reminder time for now
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