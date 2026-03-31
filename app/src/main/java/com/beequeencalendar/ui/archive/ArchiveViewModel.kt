package com.beequeencalendar.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.beequeencalendar.data.AppDatabase
import com.beequeencalendar.data.entity.Event
import com.beequeencalendar.notification.AlarmScheduler
import kotlinx.coroutines.launch

class ArchiveViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    val graftings = db.graftingDao().getAllDesc()

    fun getNotesForGrafting(graftingId: Long): LiveData<List<Event>> =
        db.eventDao().getNotedEventsByGrafting(graftingId)

    fun delete(id: Long) {
        viewModelScope.launch {
            val eventIds = db.eventDao().getIdsByGraftingId(id)
            eventIds.forEach { AlarmScheduler.cancelEvent(getApplication(), it) }
            db.graftingDao().deleteById(id)
        }
    }

    // ✅ Метод массового удаления
    fun deleteMultiple(ids: Collection<Long>) {
        viewModelScope.launch {
            ids.forEach { graftingId ->
                val eventIds = db.eventDao().getIdsByGraftingId(graftingId)
                eventIds.forEach { AlarmScheduler.cancelEvent(getApplication(), it) }
                db.graftingDao().deleteById(graftingId)
            }
        }
    }
}
