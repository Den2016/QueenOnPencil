package com.beequeencalendar.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [Index("grafting_id")],
    foreignKeys = [ForeignKey(
        entity = Grafting::class,
        parentColumns = ["id"],
        childColumns = ["grafting_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "grafting_id") val graftingId: Long = 0,
    @ColumnInfo(name = "dt") val dt: String = "",
    @ColumnInfo(name = "desc") val desc: String = "",
    @ColumnInfo(name = "note", defaultValue = "") val note: String = ""
)
