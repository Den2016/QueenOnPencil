package com.beequeencalendar.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grafting")
data class Grafting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "tp") val tp: Int = 0,
    @ColumnInfo(name = "dt") val dt: String = "",
    @ColumnInfo(name = "shift") val shift: Int = 0,
    @ColumnInfo(name = "desc") val desc: String = "",
    @ColumnInfo(name = "schedule_id") val scheduleId: Long = 1L // По умолчанию базовый шаблон
)