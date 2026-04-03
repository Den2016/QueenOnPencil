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
import com.beequeencalendar.data.entity.NotificationRule
import com.beequeencalendar.data.entity.NotificationSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object AlarmScheduler {

    // Добавьте этот класс внутри AlarmScheduler или отдельно
    private data class AlarmParams(
        val timeHour: Int,
        val timeMinute: Int,
        val advanceDaysList: List<Int>,
        val repeatCount: Int,
        val repeatIntervalMin: Int
    )
    private const val PREFS = "notification_prefs"
//    private const val KEY_HOUR = "notification_hour"
//    private const val KEY_MINUTE = "notification_minute"
//    private const val DEFAULT_HOUR = 8
//    private const val DEFAULT_MINUTE = 0

    // ✅ ЛИМИТ точных будильников (скользящее окно)
    private const val MAX_SCHEDULED_ALARMS = 15

    // ✅ ДОБАВИТЬ эти константы и методы:
    private const val KEY_DEFAULT_GRAFT_TYPE = "default_graft_type"
    private const val DEFAULT_GRAFT_TYPE = 0

    fun getDefaultGraftType(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_DEFAULT_GRAFT_TYPE, DEFAULT_GRAFT_TYPE)

    fun saveDefaultGraftType(context: Context, typeIndex: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putInt(KEY_DEFAULT_GRAFT_TYPE, typeIndex)
        }
    }

    // ✅ ГЛАВНЫЙ метод планирования с учётом расписаний и новой логики
    suspend fun scheduleEventsForGrafting(
        context: Context,
        graftingId: Long,
        scheduleId: Long,
        events: List<Event>
    ) {
        val db = AppDatabase.getInstance(context)
        val rules = db.notificationScheduleDao().getRulesByScheduleId(scheduleId)

        // Получаем время по умолчанию для этого расписания
        val schedule = db.notificationScheduleDao().getById(scheduleId)
        val defaultHour = schedule?.defaultHour ?: 8
        val defaultMinute = schedule?.defaultMinute ?: 0

        // Генерируем все потенциальные будильники
        val allAlarmRequests = mutableListOf<AlarmRequest>()
        val currentTimeMillis = System.currentTimeMillis()

        for (event in events) {
            val rule = rules.find { it.eventType == event.desc }

            // Определяем, нужно ли создавать будильник
            if (rule != null && !rule.isEnabled) {
                // Правило есть, но выключено — пропускаем событие полностью
                continue
            }


// Затем в коде:
            val params = if (rule != null) {
                // Правило найдено — используем его параметры
                AlarmParams(
                    rule.timeHour,
                    rule.timeMinute,
                    parseAdvanceDays(rule.advanceDays),
                    rule.repeatCount,
                    rule.repeatIntervalMin
                )
            } else {
                // Правило не найдено — используем время по умолчанию
                AlarmParams(defaultHour, defaultMinute, listOf(0), 0, 5)
            }

            // Генерируем будильники для каждого дня из списка (обычно [0] или [0,1,2,3])
            for (daysBefore in params.advanceDaysList) {
                val alarmDate = LocalDate.parse(event.dt, DateTimeFormatter.ISO_LOCAL_DATE)
                    .minusDays(daysBefore.toLong())
                val alarmTime = LocalDateTime.of(
                    alarmDate,
                    java.time.LocalTime.of(params.timeHour, params.timeMinute)
                )
                val alarmTimeMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Пропускаем будильники на прошедшее время
                if (alarmTimeMillis <= currentTimeMillis) {
                    continue
                }

                // Основной будильник
                allAlarmRequests.add(
                    AlarmRequest(
                        eventId = event.id,
                        graftingId = graftingId,
                        eventDesc = event.desc,
                        eventDate = event.dt,
                        alarmTimeMillis = alarmTimeMillis,
                        isAdvance = daysBefore > 0,
                        advanceDays = daysBefore
                    )
                )

                // Добавляем повторы, если они нужны
                for (i in 1..params.repeatCount) {
                    val repeatMillis = alarmTimeMillis + (i * params.repeatIntervalMin * 60 * 1000L)
                    // Проверяем время и для повторов
                    if (repeatMillis <= currentTimeMillis) {
                        continue
                    }
                    allAlarmRequests.add(
                        AlarmRequest(
                            eventId = event.id,
                            graftingId = graftingId,
                            eventDesc = event.desc,
                            eventDate = event.dt,
                            alarmTimeMillis = repeatMillis,
                            isAdvance = daysBefore > 0,
                            advanceDays = daysBefore,
                            repeatIndex = i
                        )
                    )
                }
            }
        }

        // ✅ Сортируем по времени и берем только ближайшие MAX_SCHEDULED_ALARMS
        val sorted = allAlarmRequests.sortedBy { it.alarmTimeMillis }
        val toSchedule = sorted.take(MAX_SCHEDULED_ALARMS)

        // Планируем будильники
        for (request in toSchedule) {
            scheduleAlarmRequest(context, request)
        }
    }

    // ✅ Вспомогательная data class для запроса будильника
    private data class AlarmRequest(
        val eventId: Long,
        val graftingId: Long,
        val eventDesc: String,
        val eventDate: String,
        val alarmTimeMillis: Long,
        val isAdvance: Boolean = false,
        val advanceDays: Int = 0,
        val repeatIndex: Int = 0
    )

    private fun scheduleAlarmRequest(context: Context, request: AlarmRequest) {
        if (request.alarmTimeMillis <= System.currentTimeMillis()) return

        val alarmManager = getAlarmManager(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
            android.util.Log.w("AlarmScheduler", "No SCHEDULE_EXACT_ALARM permission")
            return
        }

        val pending = buildPendingIntentForRequest(context, request)
        setAlarmSafe(alarmManager, request.alarmTimeMillis, pending)

        android.util.Log.d(
            "AlarmScheduler",
            "✅ Scheduled: ${request.eventDesc} at ${java.util.Date(request.alarmTimeMillis)}" +
            if (request.isAdvance) " (за ${request.advanceDays} дн.)" else "" +
            if (request.repeatIndex > 0) " (повтор ${request.repeatIndex})" else ""
        )
    }

    private fun buildPendingIntentForRequest(context: Context, request: AlarmRequest): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, request.eventId)
            putExtra(AlarmReceiver.EXTRA_TITLE, request.eventDesc)
            putExtra(AlarmReceiver.EXTRA_TEXT, request.eventDate)
            putExtra(AlarmReceiver.EXTRA_IS_ADVANCE, request.isAdvance)
            putExtra(AlarmReceiver.EXTRA_ADVANCE_DAYS, request.advanceDays)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return buildPendingIntentLegacy(context, request.eventId.toInt(), intent)
    }

    // ✅ Парсинг JSON массива дней
    fun parseAdvanceDays(json: String): List<Int> {
        return try {
            val array = JSONArray(json)
            List(array.length()) { array.getInt(it) }
        } catch (e: Exception) {
            listOf(0) // По умолчанию только день события
        }
    }

    // ✅ Метод для перепланирования всех будущих событий (скользящее окно)
    suspend fun rescheduleAll(context: Context) {
        val db = AppDatabase.getInstance(context)
        val events = withContext(Dispatchers.IO) {
            db.eventDao().getFutureEvents()
        }

        // Группируем по прививкам
        val eventsByGrafting = events.groupBy { it.graftingId }

        for ((graftingId, graftEvents) in eventsByGrafting) {
            val grafting = db.graftingDao().getById(graftingId) ?: continue
            scheduleEventsForGrafting(context, graftingId, grafting.scheduleId, graftEvents)
        }
    }

    // ✅ Отмена всех будильников для прививки
    suspend fun cancelEventsForGrafting(context: Context, graftingId: Long) {
        val db = AppDatabase.getInstance(context)
        val eventIds = db.eventDao().getIdsByGraftingId(graftingId)
        eventIds.forEach { eventId ->
            cancelEvent(context, eventId)
        }
    }

    // ✅ Существующие методы (canScheduleExactAlarms, requestExactAlarmPermission, etc.)
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




    // ✅ Вспомогательный метод для получения AlarmManager (совместим с API 21)
    @Suppress("DEPRECATION")
    private fun getAlarmManager(context: Context): AlarmManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(AlarmManager::class.java)
        } else {
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        }
    }


    // ✅ УБЕДИТЕСЬ, что этот метод только ОДИН в файле
    fun cancelEvent(context: Context, eventId: Long) {
        val alarmManager = getAlarmManager(context) ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = buildPendingIntentLegacy(context, eventId.toInt(), intent)
        alarmManager.cancel(pending)
    }


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

    // ✅ ЗАМЕНИТЬ весь метод getScheduledAlarms на этот:
    suspend fun getScheduledAlarms(context: Context): List<ScheduledAlarmInfo> {
        val db = AppDatabase.getInstance(context)
        val events = withContext(Dispatchers.IO) {
            db.eventDao().getFutureEvents()
        }

        return events.map { event ->
            val grafting = db.graftingDao().getById(event.graftingId)
            val scheduleId = grafting?.scheduleId ?: 1L

            // Получаем правила этого расписания
            val rules = db.notificationScheduleDao().getRulesByScheduleId(scheduleId)
            // Ищем правило, соответствующее типу события
            val rule = rules.find { it.eventType == event.desc }

            val hour = rule?.timeHour ?: 8
            val minute = rule?.timeMinute ?: 0

            val eventDate = LocalDate.parse(event.dt, DateTimeFormatter.ISO_LOCAL_DATE)
            val alarmTime = LocalDateTime.of(eventDate, java.time.LocalTime.of(hour, minute))
            val millis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            ScheduledAlarmInfo(
                eventId = event.id,
                graftingId = event.graftingId,
                eventDesc = event.desc,
                eventDate = event.dt,
                alarmTime = millis,
                isExactAlarm = canScheduleExactAlarms(context),
                scheduleName = grafting?.let {
                    db.notificationScheduleDao().getById(it.scheduleId)?.name
                } ?: "Базовый"
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
        val isExactAlarm: Boolean,
        val scheduleName: String = "Базовый"
    )
}
