package com.example.nobet.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nobet.MainActivity
import com.example.nobet.R
import com.example.nobet.ui.calendar.ShiftType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "shift_reminders"
        const val CHANNEL_NAME = "Nöbet Hatırlatmaları"
        const val CHANNEL_DESCRIPTION = "Nöbet saatleriniz için hatırlatma bildirimleri"
        
        const val NOTIFICATION_ID_BASE = 1000
        private const val TAG = "NotificationHelper"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    setShowBadge(true)
                    enableLights(true)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channel", e)
            }
        }
    }
    
    fun showShiftReminder(
        shiftType: ShiftType,
        reminderDays: Int,
        shiftDate: LocalDate,
        notificationId: Int = NOTIFICATION_ID_BASE
    ): Boolean {
        try {
            // Check if notifications are enabled
            if (!areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled by user")
                return false
            }
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val dayText = when (reminderDays) {
                0 -> "bugün"
                1 -> "yarın"
                2 -> "2 gün sonra" 
                3 -> "3 gün sonra"
                else -> "${reminderDays} gün sonra"
            }
            
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("tr", "TR"))
            val formattedDate = shiftDate.format(dateFormatter)
            
            val title = "🏥 Nöbet Hatırlatması"
            val content = if (reminderDays == 0) {
                "Bugün ${shiftType.label} nöbetiniz var!"
            } else {
                "$formattedDate tarihinde ${shiftType.label} nöbetiniz var! ($dayText)"
            }
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(shiftType.color.value.toInt())
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Notification sent successfully for shift on $formattedDate")
            return true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission denied", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
            return false
        }
    }
    
    fun cancelNotification(notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
            Log.d(TAG, "Notification $notificationId cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification $notificationId", e)
        }
    }
    
    fun areNotificationsEnabled(): Boolean {
        return try {
            val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.d(TAG, "Notifications enabled: $enabled")
            enabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check notification permissions", e)
            false
        }
    }
    
    fun isChannelEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
                val enabled = channel?.importance != NotificationManager.IMPORTANCE_NONE
                Log.d(TAG, "Channel enabled: $enabled")
                enabled
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check channel status", e)
                false
            }
        } else {
            true // Channels don't exist below API 26
        }
    }
    
    fun testNotification() {
        showShiftReminder(
            ShiftType.MORNING,
            0,
            LocalDate.now(),
            NOTIFICATION_ID_BASE + 999
        )
    }
}