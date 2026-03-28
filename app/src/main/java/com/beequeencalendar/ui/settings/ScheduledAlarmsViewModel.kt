package com.beequeencalendar.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beequeencalendar.notification.AlarmScheduler
import kotlinx.coroutines.launch

class ScheduledAlarmsViewModel(app: Application) : AndroidViewModel(app) {

    private val _alarms = MutableLiveData<List<AlarmScheduler.ScheduledAlarmInfo>>()
    val alarms: LiveData<List<AlarmScheduler.ScheduledAlarmInfo>> = _alarms

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    fun loadAlarms() {
        viewModelScope.launch {
            val alarmList = AlarmScheduler.getScheduledAlarms(getApplication())
            _alarms.value = alarmList
            _isEmpty.value = alarmList.isEmpty()
        }
    }

    fun cancelAlarm(eventId: Long) {
        viewModelScope.launch {
            AlarmScheduler.cancelEvent(getApplication(), eventId)
            loadAlarms() // Перезагрузить список
        }
    }
}