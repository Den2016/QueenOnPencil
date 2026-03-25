package com.beequeencalendar.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.beequeencalendar.data.AppDatabase
import com.beequeencalendar.data.entity.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_DEFAULT_GRAFT_TYPE, typeIndex)
            .apply()
    }

    fun getHour(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_HOUR, DEFAULT_HOUR)

    fun getMinute(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MINUTE, DEFAULT_MINUTE)

    fun saveTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    fun scheduleEvents(context: Context, events: List<Event>) {
        events.forEach { scheduleEvent(context, it) }
    }

    fun scheduleEvent(context: Context, event: Event) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val hour = getHour(context)
        val minute = getMinute(context)

        val eventDate = LocalDate.parse(event.dt, DateTimeFormatter.ISO_LOCAL_DATE)
        val alarmTime = LocalDateTime.of(eventDate, java.time.LocalTime.of(hour, minute))
        val millis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (millis <= System.currentTimeMillis()) return

        val pending = buildPendingIntent(context, event)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    fun cancelEvent(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, eventId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    suspend fun rescheduleAll(context: Context) {
        val events = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).eventDao().getFutureEvents()
        }
        events.forEach { scheduleEvent(context, it) }
    }

    private fun buildPendingIntent(context: Context, event: Event): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, event.desc)
            putExtra(AlarmReceiver.EXTRA_TEXT, event.dt)
        }
        return PendingIntent.getBroadcast(
            context, event.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
