package com.beequeencalendar.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.beequeencalendar.MainActivity
import com.beequeencalendar.R

object NotificationHelper {

    const val CHANNEL_ID = "bee_queen_events"

//    fun createChannel(context: Context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            // Пересоздать канал чтобы применить IMPORTANCE_HIGH
//            context.getSystemService(NotificationManager::class.java)
//                .deleteNotificationChannel(CHANNEL_ID)
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                context.getString(R.string.notification_channel_name),
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                enableVibration(true)
//                vibrationPattern = longArrayOf(0, 500, 200, 500)
//                setSound(
//                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
//                    android.media.AudioAttributes.Builder()
//                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
//                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                        .build()
//                )
//            }
//            context.getSystemService(NotificationManager::class.java)
//                .createNotificationChannel(channel)
//        }
//    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // ✅ Удаляем старый канал чтобы применить новые настройки
            notificationManager.deleteNotificationChannel(CHANNEL_ID)

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH  // ✅ Важно!
            ).apply {
                description = "Уведомления о событиях улья"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)

                // ✅ Явно устанавливаем звук будильника для канала
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                setShowBadge(true)
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

//
//    fun createChannel(context: Context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.getSystemService(NotificationManager::class.java)
//                .deleteNotificationChannel(CHANNEL_ID)
//
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                context.getString(R.string.notification_channel_name),
//                NotificationManager.IMPORTANCE_HIGH // ✅ Важно для будильников
//            ).apply {
//                enableVibration(true)
//                vibrationPattern = longArrayOf(0, 500, 200, 500)
//                setSound(
//                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
//                    android.media.AudioAttributes.Builder()
//                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
//                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                        .build()
//                )
//                // ✅ Показывать как будильник в настройках
//                setShowBadge(true)
//            }
//            context.getSystemService(NotificationManager::class.java)
//                .createNotificationChannel(channel)
//        }
//    }

//    fun showNotification(context: Context, title: String, text: String, eventId: Long) {
//        val intent = Intent(context, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//        val pending = PendingIntent.getActivity(
//            context, eventId.toInt(), intent,
//            // ✅ Флаг IMMUTABLE только для API 23+
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            } else {
//                PendingIntent.FLAG_UPDATE_CURRENT
//            }        )
//        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle(title)
//            .setContentText(text)
//            .setAutoCancel(true)
//            .setContentIntent(pending)
//            .setFullScreenIntent(pending, true)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setCategory(NotificationCompat.CATEGORY_ALARM)
//            .setDefaults(NotificationCompat.DEFAULT_ALL)
//            .build()
//
//        // ✅ Безопасное получение NotificationManager
//        getNotificationManager(context)?.notify(eventId.toInt(), notification)
//    }
//    // ✅ Универсальный метод для всех версий API
//    @Suppress("DEPRECATION")
//    private fun getNotificationManager(context: Context): NotificationManager? {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            context.getSystemService(NotificationManager::class.java)
//        } else {
//            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
//        }
//    }
fun showNotification(context: Context, title: String, text: String, eventId: Long) {
    // ✅Intent для открытия приложения ПРИ КЛИКЕ на уведомление
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pending = PendingIntent.getActivity(
        context, eventId.toInt(), intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    )

    // ✅ Получаем URI звука будильника
    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVibrate(longArrayOf(0L, 500L, 200L, 500L))
        .setSound(alarmSound)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .build()

// ✅ СТАЛО (прямой вызов):
    val notificationManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(NotificationManager::class.java)
    } else {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }
    notificationManager?.notify(eventId.toInt(), notification)
}
}
