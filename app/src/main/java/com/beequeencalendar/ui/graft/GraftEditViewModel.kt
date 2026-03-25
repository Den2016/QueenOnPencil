package com.beequeencalendar.ui.graft

import android.app.Application
import androidx.lifecycle.*
import com.beequeencalendar.data.AppDatabase
import com.beequeencalendar.data.BreedingCalendar
import com.beequeencalendar.data.entity.Grafting
import com.beequeencalendar.notification.AlarmScheduler
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GraftEditViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val graftingDao = db.graftingDao()
    private val eventDao = db.eventDao()

    private val _grafting = MutableLiveData<Grafting>()
    val grafting: LiveData<Grafting> = _grafting

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private val _preview = MutableLiveData<List<Pair<String, String>>>()
    val preview: LiveData<List<Pair<String, String>>> = _preview

    fun load(graftId: Long) {
        if (graftId > 0) {
            viewModelScope.launch {
                graftingDao.getById(graftId)?.let { _grafting.value = it }
            }
        } else {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            _grafting.value = Grafting(dt = today)
        }
    }

    fun updatePreview(date: String, tp: Int) {
        try {
            _preview.value = BreedingCalendar.previewEvents(date, tp)
        } catch (_: Exception) { }
    }

    fun save(tp: Int, dt: String, shift: Int, desc: String, existingId: Long) {
        viewModelScope.launch {
            val id: Long
            if (existingId > 0) {
                val updated = Grafting(id = existingId, tp = tp, dt = dt, shift = shift, desc = desc)
                graftingDao.update(updated)
                eventDao.deleteByGraftingId(existingId)
                id = existingId
            } else {
                id = graftingDao.insert(Grafting(tp = tp, dt = dt, shift = shift, desc = desc))
            }
            val events = BreedingCalendar.generateEvents(id, dt, tp)
            eventDao.insertAll(events)
            val savedEvents = eventDao.getFutureEvents().filter { it.graftingId == id }
            AlarmScheduler.scheduleEvents(getApplication(), savedEvents)
            _saved.postValue(true)
        }
    }
}
