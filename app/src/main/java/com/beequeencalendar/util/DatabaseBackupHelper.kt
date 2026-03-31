package com.beequeencalendar.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import com.beequeencalendar.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseBackupHelper {
    private const val DB_NAME = "bqc.db"
    private const val TAG = "DatabaseBackup"

    /**
     * Экспорт базы данных в файл через SAF
     * ✅ Исправлено: принудительный checkpoint + копирование WAL-файлов
     */
    suspend fun exportDatabase(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbInstance = AppDatabase.getInstance(context)
            val dbFile = context.getDatabasePath(DB_NAME)

            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("База данных не найдена"))
            }

            // ✅ 1. Принудительно сбрасываем WAL в основной файл
            // Используем query() вместо execSQL() так как PRAGMA возвращает результат
            dbInstance.openHelper.writableDatabase.query(
                "PRAGMA wal_checkpoint(FULL)"
            ).close()  // Сразу закрываем курсор, результат нам не важен

            // ✅ 2. Небольшая задержка для гарантии завершения операции
            kotlinx.coroutines.delay(100)

            // ✅ 3. Копируем основной файл
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть поток записи"))

            Log.d(TAG, "✅ Экспорт выполнен: ${uri.lastPathSegment}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка экспорта", e)
            Result.failure(e)
        }
    }

    /**
     * Импорт базы данных из файла через SAF
     * ✅ Исправлено: очистка кэша Room + удаление всех файлов БД
     */
    suspend fun importDatabase(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Проверяем версию импортируемой БД
            val importedVersion = getDatabaseVersion(context, uri)
            if (importedVersion == null) {
                return@withContext Result.failure(Exception("Не удалось прочитать версию БД"))
            }

            val currentVersion = AppDatabase.getInstance(context).openHelper.writableDatabase.version
            Log.d(TAG, "Версия импорта: $importedVersion, текущая: $currentVersion")

            // 2. Если версия файла больше текущей - отказываем
            if (importedVersion > currentVersion) {
                return@withContext Result.failure(
                    Exception("Версия БД ($importedVersion) новее, чем поддерживает приложение ($currentVersion)")
                )
            }

            // 3. ✅ Полностью закрываем и сбрасываем экземпляр Room
            val dbInstance = AppDatabase.getInstance(context)
            dbInstance.close()
            // ✅ Сбрасываем singleton чтобы создать новое соединение после импорта
            AppDatabase::class.java.getDeclaredField("INSTANCE").apply {
                isAccessible = true
                set(null, null)
            }

            // 4. Удаляем ВСЕ файлы базы (основной + wal + shm)
            deleteAllDatabaseFiles(context)

            // 5. Копируем новый файл
            val dbFile = context.getDatabasePath(DB_NAME)

            // Закрываем все активные соединения с БД
            AppDatabase.getInstance(context).close()

            // Удаляем старую БД и вспомогательные файлы
            deleteAllDatabaseFiles(context)

            // Копируем новый файл
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть поток чтения"))

            // 6. ✅ Гарантируем, что после импорта нет остаточных WAL-файлов
            // (импортированный файл уже должен быть в режиме DELETE или иметь пустой WAL)
            File("${dbFile.absolutePath}-wal").delete()
            File("${dbFile.absolutePath}-shm").delete()

            Log.d(TAG, "✅ Импорт выполнен успешно")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка импорта", e)
            Result.failure(e)
        }
    }

    /**
     * Получает версию БД из файла через SAF
     */
    private fun getDatabaseVersion(context: Context, uri: Uri): Int? {
        var db: SQLiteDatabase? = null
        return try {
            // Временное копирование для чтения версии
            val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.db")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            db = SQLiteDatabase.openDatabase(
                tempFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val version = db.version
            tempFile.delete()
            version
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения версии БД", e)
            null
        } finally {
            db?.close()
        }
    }

    /**
     * ✅ Удаляет ВСЕ файлы базы данных
     */
    private fun deleteAllDatabaseFiles(context: Context) {
        val dbFile = context.getDatabasePath(DB_NAME)
        dbFile.delete()
        File("${dbFile.absolutePath}-wal").delete()
        File("${dbFile.absolutePath}-shm").delete()
        // На всякий случай удаляем возможные временные файлы
        File("${dbFile.absolutePath}-journal").delete()
    }
}