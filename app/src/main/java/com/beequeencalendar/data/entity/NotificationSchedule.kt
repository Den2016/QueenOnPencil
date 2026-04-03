package com.beequeencalendar.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_schedule")
data class NotificationSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    // ✅ Новое: время по умолчанию для этого расписания
    @ColumnInfo(name = "default_hour")
    val defaultHour: Int = 8,

    @ColumnInfo(name = "default_minute")
    val defaultMinute: Int = 0
)