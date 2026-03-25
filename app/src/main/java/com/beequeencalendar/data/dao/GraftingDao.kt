package com.beequeencalendar.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.beequeencalendar.data.entity.Grafting

@Dao
interface GraftingDao {
    @Insert
    suspend fun insert(grafting: Grafting): Long

    @Update
    suspend fun update(grafting: Grafting)

    @Query("DELETE FROM grafting WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM grafting WHERE id = :id")
    suspend fun getById(id: Long): Grafting?

    @Query("SELECT * FROM grafting ORDER BY dt DESC")
    fun getAllDesc(): LiveData<List<Grafting>>
}
