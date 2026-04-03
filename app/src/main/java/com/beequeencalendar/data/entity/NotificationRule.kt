package com.beequeencalendar.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_rule",
    indices = [Index("schedule_id")],
    foreignKeys = [ForeignKey(
        entity = NotificationSchedule::class,
        parentColumns = ["id"],
        childColumns = ["schedule_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class NotificationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "schedule_id") val scheduleId: Long,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "time_hour") val timeHour: Int = 8,
    @ColumnInfo(name = "time_minute") val timeMinute: Int = 0,
    @ColumnInfo(name = "advance_days") val advanceDays: String = "0", // JSON: "[0,1,3]"
    @ColumnInfo(name = "repeat_count") val repeatCount: Int = 0,
    @ColumnInfo(name = "repeat_interval_min") val repeatIntervalMin: Int = 5
)