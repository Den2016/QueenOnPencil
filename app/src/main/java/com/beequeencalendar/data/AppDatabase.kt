package com.beequeencalendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.beequeencalendar.data.dao.EventDao
import com.beequeencalendar.data.dao.GraftingDao
import com.beequeencalendar.data.dao.NotificationScheduleDao // ✅ Добавить импорт
import com.beequeencalendar.data.entity.Event
import com.beequeencalendar.data.entity.Grafting
import com.beequeencalendar.data.entity.NotificationSchedule // ✅ Добавить импорт
import com.beequeencalendar.data.entity.NotificationRule // ✅ Добавить импорт

@Database(
    // ✅ 1. Добавить новые сущности в список
    entities = [
        Grafting::class,
        Event::class,
        NotificationSchedule::class,
        NotificationRule::class
    ],
    version = 6, // ✅ 2. Поднять версию с 3 до 5
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun graftingDao(): GraftingDao
    abstract fun eventDao(): EventDao
    abstract fun notificationScheduleDao(): NotificationScheduleDao // ✅ 3. Добавить DAO

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
                db.execSQL("""
                    CREATE TABLE grafting_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shift INTEGER NOT NULL,
                        dt TEXT NOT NULL,
                        tp INTEGER NOT NULL,
                        desc TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO grafting_new (id, shift, dt, tp, desc)
                    SELECT id, tp, dt, shift, desc FROM grafting
                """.trimIndent())
                db.execSQL("DROP TABLE grafting")
                db.execSQL("ALTER TABLE grafting_new RENAME TO grafting")
            }
        }

        // ✅ 4. Добавить миграцию 3 -> 4 (создание таблиц расписаний)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Таблица шаблонов
                db.execSQL("""
                    CREATE TABLE notification_schedule (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        is_default INTEGER NOT NULL DEFAULT 0,
                        is_active INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                // Таблица правил
                db.execSQL("""
                    CREATE TABLE notification_rule (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        schedule_id INTEGER NOT NULL,
                        event_type TEXT NOT NULL,
                        is_enabled INTEGER NOT NULL DEFAULT 1,
                        time_hour INTEGER NOT NULL DEFAULT 8,
                        time_minute INTEGER NOT NULL DEFAULT 0,
                        advance_days TEXT NOT NULL DEFAULT '0',
                        repeat_count INTEGER NOT NULL DEFAULT 0,
                        repeat_interval_min INTEGER NOT NULL DEFAULT 5,
                        FOREIGN KEY (schedule_id) REFERENCES notification_schedule(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Индекс
                db.execSQL("CREATE INDEX index_notification_rule_schedule_id ON notification_rule(schedule_id)")

                // Создаём дефолтный шаблон
                db.execSQL("""
                    INSERT INTO notification_schedule (id, name, is_default, is_active) 
                    VALUES (1, 'Базовый', 1, 1)
                """.trimIndent())
            }
        }

        // ✅ 5. Добавить миграцию 4 -> 5 (добавление schedule_id в grafting)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE grafting ADD COLUMN schedule_id INTEGER NOT NULL DEFAULT 1")
            }
        }

        // В конец companion object, перед fun getInstance():
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_schedule ADD COLUMN default_hour INTEGER NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE notification_schedule ADD COLUMN default_minute INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bqc.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                    .build().also { INSTANCE = it }
            }
    }
}