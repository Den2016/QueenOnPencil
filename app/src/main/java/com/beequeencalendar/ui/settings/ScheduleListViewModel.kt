package com.beequeencalendar.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.beequeencalendar.data.AppDatabase
import com.beequeencalendar.data.BreedingCalendar
import com.beequeencalendar.data.entity.NotificationRule
import com.beequeencalendar.data.entity.NotificationSchedule
import kotlinx.coroutines.launch

class ScheduleListViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)

    val schedules: LiveData<List<NotificationSchedule>> =
        db.notificationScheduleDao().getAllActive()

    fun setDefault(scheduleId: Long) {
        viewModelScope.launch {
            val all = db.notificationScheduleDao().getAllActiveList()
            for (s in all) {
                val updated = s.copy(isDefault = s.id == scheduleId)
                db.notificationScheduleDao().update(updated)
            }
        }
    }

    fun deleteSchedule(schedule: NotificationSchedule) {
        viewModelScope.launch {
            if (schedule.isDefault) return@launch
            db.notificationScheduleDao().delete(schedule)
        }
    }


    // ✅ Замените saveSchedule и getScheduleWithRules на эти:
    suspend fun getScheduleWithRules(scheduleId: Long): Pair<NotificationSchedule?, List<NotificationRule>> {
        return if (scheduleId > 0) {
            val schedule = db.notificationScheduleDao().getById(scheduleId)
            db.notificationScheduleDao().replenishDefaultRulesIfMissing(scheduleId, BreedingCalendar.getAllEventTypes()) // Оставляем для совместимости, но не обязательно
            schedule to db.notificationScheduleDao().getRulesByScheduleId(scheduleId)
        } else {
            null to emptyList() // ✅ Пустой список для нового расписания
        }
    }

    fun saveSchedule(
        scheduleId: Long,
        name: String,
        isDefault: Boolean,
        defaultHour: Int,
        defaultMinute: Int,
        rules: List<NotificationRule>
    ) {
        viewModelScope.launch {
            val finalId = if (scheduleId > 0) {
                val existing = db.notificationScheduleDao().getById(scheduleId) ?: return@launch
                db.notificationScheduleDao().update(
                    existing.copy(name = name, isDefault = isDefault, defaultHour = defaultHour, defaultMinute = defaultMinute)
                )
                scheduleId
            } else {
                db.notificationScheduleDao().insert(
                    NotificationSchedule(name = name, isDefault = isDefault, defaultHour = defaultHour, defaultMinute = defaultMinute)
                )
            }
            db.notificationScheduleDao().saveRulesAtomic(finalId, rules)
        }
    }
}