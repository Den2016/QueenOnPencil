package com.beequeencalendar.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""

        // ✅ Логируем получение будильника
        Log.d("AlarmReceiver", "🔔 Alarm received! Event ID: $eventId, Title: $title, Time: $text")

        NotificationHelper.showNotification(context, title, text, eventId)
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
