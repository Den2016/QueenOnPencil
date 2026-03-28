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

    // ✅ ЗАМЕНИТЬ _saved на _saveResult
    private val _saveResult = MutableLiveData<SaveResult>()
    val saveResult: LiveData<SaveResult> = _saveResult

    private val _preview = MutableLiveData<List<Pair<String, String>>>()
    val preview: LiveData<List<Pair<String, String>>> = _preview

    // ✅ ДОБАВИТЬ sealed class для результата сохранения
    sealed class SaveResult {
        object Success : SaveResult()
        data class Warning(val message: String) : SaveResult()
        object Error : SaveResult()
    }

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

    fun updatePreview(date: String, shift: Int, tp: Int) {
        try {
            _preview.value = BreedingCalendar.previewEvents(date, shift, tp)
        } catch (_: Exception) { }
    }

//    fun save(tp: Int, dt: String, shift: Int, desc: String, existingId: Long) {
//        viewModelScope.launch {
//            val id: Long
//            if (existingId > 0) {
//                val updated = Grafting(id = existingId, tp = tp, dt = dt, shift = shift, desc = desc)
//                graftingDao.update(updated)
//                eventDao.deleteByGraftingId(existingId)
//                id = existingId
//            } else {
//                id = graftingDao.insert(Grafting(tp = tp, dt = dt, shift = shift, desc = desc))
//            }
//            val events = BreedingCalendar.generateEvents(id, dt, shift, tp)
//            eventDao.insertAll(events)
//            val savedEvents = eventDao.getFutureEvents().filter { it.graftingId == id }
//            AlarmScheduler.scheduleEvents(getApplication(), savedEvents)
//            _saved.postValue(true)
//        }
//    }
fun save(tp: Int, dt: String, shift: Int, desc: String, existingId: Long) {
    viewModelScope.launch {
        try {
            val id: Long
            if (existingId > 0) {
                val updated = Grafting(id = existingId, tp = tp, dt = dt, shift = shift, desc = desc)
                graftingDao.update(updated)
                eventDao.deleteByGraftingId(existingId)
                id = existingId
            } else {
                id = graftingDao.insert(Grafting(tp = tp, dt = dt, shift = shift, desc = desc))
            }

            val events = BreedingCalendar.generateEvents(id, dt, shift, tp)
            eventDao.insertAll(events)
            val savedEvents = eventDao.getFutureEvents().filter { it.graftingId == id }

            // ✅ ПРОВЕРКА перед планированием будильников
            if (!AlarmScheduler.canScheduleExactAlarms(getApplication())) {
                // ⚠️ Будильники не будут работать — сообщаем пользователю
                _saveResult.postValue(SaveResult.Warning(
                    "⚠️ Будильники не установлены! Разрешите \"Точные будильники\" в настройках системы, чтобы получать уведомления."
                ))
            } else {
                AlarmScheduler.scheduleEvents(getApplication(), savedEvents)
                _saveResult.postValue(SaveResult.Success)
            }

        } catch (e: Exception) {
            _saveResult.postValue(SaveResult.Error)
        }
    }
}

    // ✅ Добавить метод для сброса результата (после обработки)
    fun resetSaveResult() {
        _saveResult.value = null
    }

}
