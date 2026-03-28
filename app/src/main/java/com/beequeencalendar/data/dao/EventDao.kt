package com.beequeencalendar.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.beequeencalendar.data.entity.Event

data class CalendarEvent(
    val eventId: Long,
    val eventDt: String,
    val eventDesc: String,
    val eventNote: String,
    val graftingId: Long,
    val graftingDt: String,
    val graftingDesc: String,
    val graftingShift: Int,
    val graftingTp: Int
)

@Dao
interface EventDao {
    @Insert
    suspend fun insertAll(events: List<Event>)

    @Query("DELETE FROM events WHERE grafting_id = :graftingId")
    suspend fun deleteByGraftingId(graftingId: Long)

    @Query("""
        SELECT events.id AS eventId, events.dt AS eventDt, events.desc AS eventDesc,
               events.note AS eventNote,
               grafting.id AS graftingId, grafting.dt AS graftingDt,
               grafting.desc AS graftingDesc, grafting.shift AS graftingShift,
               grafting.tp AS graftingTp
        FROM events
        INNER JOIN grafting ON events.grafting_id = grafting.id
        WHERE events.dt BETWEEN date('now') AND date('now','+32 days')
        ORDER BY events.dt, grafting.dt
    """)
    fun getUpcomingEvents(): LiveData<List<CalendarEvent>>

    @Query("UPDATE events SET note = :note WHERE id = :eventId")
    suspend fun updateNote(eventId: Long, note: String)

    @Query("SELECT * FROM events WHERE grafting_id = :graftingId AND note != '' ORDER BY dt")
    fun getNotedEventsByGrafting(graftingId: Long): LiveData<List<Event>>

    @Query("SELECT * FROM events WHERE dt >= date('now') ORDER BY dt")
    suspend fun getFutureEvents(): List<Event>

    @Query("SELECT id FROM events WHERE grafting_id = :graftingId")
    suspend fun getIdsByGraftingId(graftingId: Long): List<Long>
}
