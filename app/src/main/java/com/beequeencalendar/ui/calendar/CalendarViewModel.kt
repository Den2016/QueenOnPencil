package com.beequeencalendar.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beequeencalendar.data.AppDatabase
import kotlinx.coroutines.launch

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val eventDao = AppDatabase.getInstance(app).eventDao()
    val events = eventDao.getUpcomingEvents()

    fun saveNote(eventId: Long, note: String) {
        viewModelScope.launch {
            eventDao.updateNote(eventId, note)
        }
    }
}
