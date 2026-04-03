package com.beequeencalendar.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
        val isAdvance = intent.getBooleanExtra(EXTRA_IS_ADVANCE, false)
        val advanceDays = intent.getIntExtra(EXTRA_ADVANCE_DAYS, 0)

        // Формируем текст уведомления
        val notificationText = if (isAdvance) {
            "⚠️ За $advanceDays дн.: $text"
        } else {
            "📅 Сегодня: $text"
        }

        Log.d("AlarmReceiver", "🔔 Alarm received! Event ID: $eventId, Title: $title, Text: $notificationText")

        NotificationHelper.showNotification(context, title, notificationText, eventId)

        // ✅ Перепланируем следующие будильники (скользящее окно)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(500)
            AlarmScheduler.rescheduleAll(context)
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_IS_ADVANCE = "is_advance"  // ✅ Новое
        const val EXTRA_ADVANCE_DAYS = "advance_days"  // ✅ Новое
    }
}