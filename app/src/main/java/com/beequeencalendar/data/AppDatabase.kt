package com.beequeencalendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.beequeencalendar.data.dao.EventDao
import com.beequeencalendar.data.dao.GraftingDao
import com.beequeencalendar.data.entity.Event
import com.beequeencalendar.data.entity.Grafting

@Database(entities = [Grafting::class, Event::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun graftingDao(): GraftingDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Создаём временную таблицу с правильным порядком колонок
                db.execSQL("""
                    CREATE TABLE grafting_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shift INTEGER NOT NULL,
                        dt TEXT NOT NULL,
                        tp INTEGER NOT NULL,
                        desc TEXT NOT NULL
                    )
                """.trimIndent())

                // Копируем данные, меняя местами значения tp и shift
                db.execSQL("""
                    INSERT INTO grafting_new (id, shift, dt, tp, desc)
                    SELECT id, tp, dt, shift, desc FROM grafting
                """.trimIndent())

                // Удаляем старую таблицу и переименовываем новую
                db.execSQL("DROP TABLE grafting")
                db.execSQL("ALTER TABLE grafting_new RENAME TO grafting")

                // Пересоздаём индексы если были (в текущей версии их нет)
            }
        }


        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bqc.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
