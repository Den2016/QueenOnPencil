package com.beequeencalendar.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.beequeencalendar.data.AppDatabase
import com.beequeencalendar.data.entity.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.edit

object AlarmScheduler {

    private const val PREFS = "notification_prefs"
    private const val KEY_HOUR = "notification_hour"
    private const val KEY_MINUTE = "notification_minute"
    private const val DEFAULT_HOUR = 8
    private const val DEFAULT_MINUTE = 0

    // Добавь константы в companion object:
    private const val KEY_DEFAULT_GRAFT_TYPE = "default_graft_type"
    private const val DEFAULT_GRAFT_TYPE = 0 // по умолчанию первый тип (после удаления "свежее" — это "Яйцо 1 день")



    // Добавь методы:
    fun getDefaultGraftType(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_DEFAULT_GRAFT_TYPE, DEFAULT_GRAFT_TYPE)

    fun saveDefaultGraftType(context: Context, typeIndex: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putInt(KEY_DEFAULT_GRAFT_TYPE, typeIndex)
        }
    }

    fun getHour(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_HOUR, DEFAULT_HOUR)

    fun getMinute(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MINUTE, DEFAULT_MINUTE)

    fun saveTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
        }
    }

    fun scheduleEvents(context: Context, events: List<Event>) {
        events.forEach { scheduleEvent(context, it) }
    }

    // ✅ Проверка, может ли приложение устанавливать точные будильники
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // На старых версиях разрешение не требуется
        }
    }

    // ✅ Запрос разрешения (вызывать из Activity)
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    // ✅ ОБНОВИТЬ метод scheduleEvent (вызов setAlarmSafe заменить на setAlarmClock):
    fun scheduleEvent(context: Context, event: Event) {
        val alarmManager = getAlarmManager(context) ?: return

        // Проверка разрешения для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
            android.util.Log.w("AlarmScheduler", "No SCHEDULE_EXACT_ALARM permission")
            return
        }

        val hour = getHour(context)
        val minute = getMinute(context)
        val eventDate = LocalDate.parse(event.dt, DateTimeFormatter.ISO_LOCAL_DATE)
        val alarmTime = LocalDateTime.of(eventDate, java.time.LocalTime.of(hour, minute))
        val millis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (millis <= System.currentTimeMillis()) return

        val pending = buildPendingIntent(context, event)

        // ✅ Вызываем новый метод
        setAlarmSafe(alarmManager, millis, pending)
    }
//    fun scheduleEvent(context: Context, event: Event) {
//
//        val alarmManager = getAlarmManager(context) ?: return
//
//        // Проверяем разрешение на Android 12+
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
//            // Разрешения нет — логируем или запрашиваем
//            android.util.Log.w("AlarmScheduler", "No SCHEDULE_EXACT_ALARM permission")
//            return
//        }
//
//
//        val hour = getHour(context)
//        val minute = getMinute(context)
//
//        val eventDate = LocalDate.parse(event.dt, DateTimeFormatter.ISO_LOCAL_DATE)
//        val alarmTime = LocalDateTime.of(eventDate, java.time.LocalTime.of(hour, minute))
//        val millis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
//
//        if (millis <= System.currentTimeMillis()) return
//
//        val pending = buildPendingIntent(context, event)
//
//        try {
//            setAlarmSafe(alarmManager, millis, pending)
//        } catch (e: SecurityException) {
//            android.util.Log.e("AlarmScheduler", "Failed to schedule alarm: ${e.message}")
//            // Здесь можно показать пользователю диалог с просьбой выдать разрешение
//        }
//    }

    // ✅ Вспомогательный метод для получения AlarmManager (совместим с API 21)
    @Suppress("DEPRECATION")
    private fun getAlarmManager(context: Context): AlarmManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(AlarmManager::class.java)
        } else {
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        }
    }


    fun cancelEvent(context: Context, eventId: Long) {
        val alarmManager = getAlarmManager(context) ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = buildPendingIntentLegacy(context, eventId.toInt(), intent)
        alarmManager.cancel(pending)
    }    // ✅ Безопасная установка будильника с фолбэком

//    private fun setAlarmSafe(alarmManager: AlarmManager, millis: Long, pending: PendingIntent) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            try {
//                // API 23+: точный будильник
//                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
//            } catch (e: SecurityException) {
//                // Если разрешения нет — пробуем менее точный будильник
//                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
//            }
//        } else
//            // API 19-22
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pending)
//    }

    // ✅ ЗАМЕНИТЬ метод setAlarmSafe на этот:
    private fun setAlarmSafe(alarmManager: AlarmManager, millis: Long, pending: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // setAlarmClock — самый надёжный метод!
                // Система выводит устройство из Doze заранее
                val alarmClockInfo = AlarmManager.AlarmClockInfo(millis, pending)
                alarmManager.setAlarmClock(alarmClockInfo, pending)
                android.util.Log.d("AlarmScheduler", "✅ Alarm clock set for: ${java.util.Date(millis)}")
            } catch (e: SecurityException) {
                android.util.Log.e("AlarmScheduler", "❌ SecurityException: ${e.message}")
                // Фолбэк на setExactAndAllowWhileIdle
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pending)
                }
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

//    // ✅ Создание PendingIntent с проверкой флагов
//    private fun buildPendingIntent(context: Context, event: Event): PendingIntent {
//        val intent = Intent(context, AlarmReceiver::class.java).apply {
//            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
//            putExtra(AlarmReceiver.EXTRA_TITLE, event.desc)
//            putExtra(AlarmReceiver.EXTRA_TEXT, event.dt)
//        }
//        return buildPendingIntentLegacy(context, event.id.toInt(), intent)
//    }

    // ✅ Убедитесь что requestCode уникален для каждого события
    private fun buildPendingIntent(context: Context, event: Event): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, event.desc)
            putExtra(AlarmReceiver.EXTRA_TEXT, event.dt)
            // ✅ Важно: установите флаг, чтобы каждый будильник был уникален
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // ✅ Используем event.id как requestCode для уникальности
        return buildPendingIntentLegacy(context, event.id.toInt(), intent)
    }

    // ✅ Универсальный метод создания PendingIntent
    @Suppress("DEPRECATION")
    fun buildPendingIntentLegacy(
        context: Context,
        requestCode: Int,
        intent: Intent
    ): PendingIntent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23+: поддерживаем FLAG_IMMUTABLE
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            // API <23: только базовый флаг
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    suspend fun rescheduleAll(context: Context) {
        val events = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).eventDao().getFutureEvents()
        }
        events.forEach { scheduleEvent(context, it) }
    }
// ✅ ДОБАВИТЬ в конец объекта AlarmScheduler:

    /**
     * Получить все будущие события с информацией о будильниках
     */
    suspend fun getScheduledAlarms(context: Context): List<ScheduledAlarmInfo> {
        val events = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).eventDao().getFutureEvents()
        }

        val hour = getHour(context)
        val minute = getMinute(context)

        return events.map { event ->
            val eventDate = LocalDate.parse(event.dt, DateTimeFormatter.ISO_LOCAL_DATE)
            val alarmTime = LocalDateTime.of(eventDate, java.time.LocalTime.of(hour, minute))
            val millis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            ScheduledAlarmInfo(
                eventId = event.id,
                graftingId = event.graftingId,
                eventDesc = event.desc,
                eventDate = event.dt,
                alarmTime = millis,
                isExactAlarm = canScheduleExactAlarms(context)
            )
        }.sortedBy { it.alarmTime }
    }

    /**
     * Данные для отображения в списке будильников
     */
    data class ScheduledAlarmInfo(
        val eventId: Long,
        val graftingId: Long,
        val eventDesc: String,
        val eventDate: String,
        val alarmTime: Long,
        val isExactAlarm: Boolean
    )
}
