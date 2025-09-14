package com.example.nobet.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nobet.ui.calendar.ShiftType
import java.time.LocalDate

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_SHIFT_REMINDER = "com.example.nobet.SHIFT_REMINDER"
        const val EXTRA_SHIFT_TYPE = "shift_type"
        const val EXTRA_REMINDER_DAYS = "reminder_days"
        const val EXTRA_SHIFT_DATE = "shift_date"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SHIFT_REMINDER -> {
                handleShiftReminder(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule notifications after device restart
                val notificationScheduler = NotificationScheduler(context)
                notificationScheduler.rescheduleAllNotifications()
            }
        }
    }
    
    private fun handleShiftReminder(context: Context, intent: Intent) {
        val shiftTypeName = intent.getStringExtra(EXTRA_SHIFT_TYPE) ?: return
        val reminderDays = intent.getIntExtra(EXTRA_REMINDER_DAYS, 1)
        val shiftDateString = intent.getStringExtra(EXTRA_SHIFT_DATE) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NotificationHelper.NOTIFICATION_ID_BASE)
        
        val shiftDate = try {
            LocalDate.parse(shiftDateString)
        } catch (e: Exception) {
            return
        }
        
        val shiftType = when (shiftTypeName) {
            "MORNING" -> ShiftType.MORNING
            "NIGHT" -> ShiftType.NIGHT
            "FULL" -> ShiftType.FULL
            else -> return
        }
        
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showShiftReminder(shiftType, reminderDays, shiftDate, notificationId)
    }
}