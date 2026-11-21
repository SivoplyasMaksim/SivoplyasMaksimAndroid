package com.example.hw_3

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


object NotificationHelper {

    const val EXTRA_FROM_NOTIFICATION = "extra_from_notification"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TEXT = "extra_text"

    const val CHANNEL_MIN = "channel_importance_min"
    const val CHANNEL_DEFAULT = "channel_importance_default"
    const val CHANNEL_HIGH = "channel_importance_high"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return // на Android < 8 каналов нет

        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(CHANNEL_MIN, "Importance MIN", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Без звука и всплывания, только в шторке" },
            NotificationChannel(CHANNEL_DEFAULT, "Importance DEFAULT", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Со звуком, без всплывания" },
            NotificationChannel(CHANNEL_HIGH, "Importance HIGH", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Со звуком и всплывающим баннером (heads-up)" }
        )
        manager.createNotificationChannels(channels)
    }

    fun notificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun channelIdFor(importance: Int): String = when (importance) {
        NotificationManager.IMPORTANCE_MIN -> CHANNEL_MIN
        NotificationManager.IMPORTANCE_HIGH -> CHANNEL_HIGH
        else -> CHANNEL_DEFAULT
    }

    private fun priorityFor(importance: Int): Int = when (importance) {
        NotificationManager.IMPORTANCE_MIN -> NotificationCompat.PRIORITY_MIN
        NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

    fun importanceOf(notification: Notification): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (notification.channelId) {
                CHANNEL_MIN -> NotificationManager.IMPORTANCE_MIN
                CHANNEL_HIGH -> NotificationManager.IMPORTANCE_HIGH
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
        } else {
            when (notification.priority) {
                NotificationCompat.PRIORITY_MIN -> NotificationManager.IMPORTANCE_MIN
                NotificationCompat.PRIORITY_HIGH, NotificationCompat.PRIORITY_MAX -> NotificationManager.IMPORTANCE_HIGH
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
        }

    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        id: Int,
        title: String,
        text: String,
        nonCancellable: Boolean,
        importance: Int
    ): Boolean {
        if (!notificationsEnabled(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // IMMUTABLE обязателен с Android 12
        )

        val notification = NotificationCompat.Builder(context, channelIdFor(importance))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priorityFor(importance))       // Android < 8
            .setOngoing(nonCancellable)                 // true = нельзя смахнуть
            .setAutoCancel(!nonCancellable)             // убираем из шторки по тапу
            .setContentIntent(contentIntent)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
        return true
    }

    fun activeNotifications(context: Context): List<StatusBarNotification> {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.activeNotifications?.toList().orEmpty()
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    fun cancelAll(context: Context) = NotificationManagerCompat.from(context).cancelAll()
}