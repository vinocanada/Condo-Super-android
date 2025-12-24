package com.condosuper.app.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.condosuper.app.MainActivity

class NotificationManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: NotificationManager? = null
        
        fun getInstance(context: Context): NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val CHANNEL_ID = "condosuper_messages"
        private const val CHANNEL_NAME = "Messages"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new messages"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun requestPermission() {
        // Permission is requested at runtime in Android 13+
        android.util.Log.d("NotificationManager", "Notification permission check")
    }

    fun sendLocalNotification(title: String, body: String, userInfo: Map<String, String> = emptyMap()) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            userInfo.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            android.util.Log.d("NotificationManager", "Notification sent: $title")
        } else {
            android.util.Log.w("NotificationManager", "Notifications not enabled")
        }
    }

    fun updateBadgeCount(count: Int) {
        // Badge count is handled automatically by Android
        android.util.Log.d("NotificationManager", "Badge count updated: $count")
    }
}


