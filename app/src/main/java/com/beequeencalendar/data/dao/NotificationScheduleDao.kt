package com.beequeencalendar.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.beequeencalendar.data.entity.NotificationSchedule
import com.beequeencalendar.data.entity.NotificationRule
import androidx.room.Transaction

@Dao
interface NotificationScheduleDao {
    @Insert
    suspend fun insert(schedule: NotificationSchedule): Long

    @Update
    suspend fun update(schedule: NotificationSchedule)

    @Delete
    suspend fun delete(schedule: NotificationSchedule)

    // ✅ LiveData для UI
    @Query("SELECT * FROM notification_schedule WHERE is_active = 1 ORDER BY is_default DESC, name")
    fun getAllActive(): LiveData<List<NotificationSchedule>>

    // ✅ Обычный список для работы в корутинах
    @Query("SELECT * FROM notification_schedule WHERE is_active = 1 ORDER BY is_default DESC, name")
    suspend fun getAllActiveList(): List<NotificationSchedule>

    @Query("SELECT * FROM notification_schedule WHERE id = :id")
    suspend fun getById(id: Long): NotificationSchedule?

    @Query("SELECT * FROM notification_schedule WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): NotificationSchedule?

    // Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: NotificationRule): Long

    @Query("SELECT * FROM notification_rule WHERE schedule_id = :scheduleId")
    suspend fun getRulesByScheduleId(scheduleId: Long): List<NotificationRule>

    @Query("DELETE FROM notification_rule WHERE schedule_id = :scheduleId")
    suspend fun deleteRulesByScheduleId(scheduleId: Long)

    @Transaction
    suspend fun replenishDefaultRulesIfMissing(
        scheduleId: Long,
        allEventTypes: List<String>,
        defaultHour: Int = 8,
        defaultMinute: Int = 0
    ) {
        val existingRules = getRulesByScheduleId(scheduleId)
        val existingTypes = existingRules.map { it.eventType }.toSet()
        val missingTypes = allEventTypes.filter { it !in existingTypes }

        if (missingTypes.isNotEmpty()) {
            missingTypes.forEach { eventType ->
                insertRule(
                    NotificationRule(
                        id = 0L,
                        scheduleId = scheduleId,
                        eventType = eventType,
                        isEnabled = true,
                        timeHour = defaultHour,
                        timeMinute = defaultMinute,
                        advanceDays = "[0]",
                        repeatCount = 0,
                        repeatIntervalMin = 5
                    )
                )
            }
        }
    }
    @Transaction
    suspend fun saveRulesAtomic(scheduleId: Long, rules: List<NotificationRule>) {
        android.util.Log.d("DB_DEBUG", "🗑️ Deleting rules for schedule $scheduleId")
        deleteRulesByScheduleId(scheduleId)

        rules.forEachIndexed { index, rule ->
            android.util.Log.d("DB_DEBUG", "💾 Inserting rule $index: ${rule.eventType} @ ${rule.timeHour}:${rule.timeMinute} ${rule.advanceDays}")
            insertRule(rule.copy(id = 0L, scheduleId = scheduleId))
        }
        android.util.Log.d("DB_DEBUG", "✅ Rules saved successfully")
    }
}
