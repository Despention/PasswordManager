package com.example.tutor.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Основной класс базы данных приложения, использующий Room.
 * Включает таблицы для Паролей (PasswordEntity), Журнала безопасности (SecurityLogEvent)
 * и Профиля пользователя (UserEntity).
 *
 * Версия: 6 (изменена при добавлении UserEntity).
 */
@Database(
    entities = [
        PasswordEntity::class,
        SecurityLogEvent::class,
        UserEntity::class // <-- Добавлена сущность пользователя
    ],
    version = 6, // <-- Версия увеличена до 6
    exportSchema = false // Отключаем экспорт схемы для упрощения
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Предоставляет доступ к операциям с таблицей паролей.
     */
    abstract fun passwordDao(): PasswordDao

    /**
     * Предоставляет доступ к операциям с таблицей журнала безопасности.
     */
    abstract fun securityLogDao(): SecurityLogDao

    /**
     * Предоставляет доступ к операциям с таблицей профиля пользователя.
     */
    abstract fun userDao(): UserDao // <-- Добавлена функция для доступа к UserDao

    companion object {
        // Volatile гарантирует, что значение INSTANCE всегда актуально для всех потоков.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "password-tutor-db" // Имя файла БД

        // --- Миграции Базы Данных ---
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("DB_MIGRATION", "Миграция базы данных с версии 1 на 2: Добавление 'category'")
                db.execSQL("ALTER TABLE passwords ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("DB_MIGRATION", "Миграция базы данных с версии 2 на 3: Добавление 'lastModified'")
                db.execSQL("ALTER TABLE passwords ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("DB_MIGRATION", "Миграция базы данных с версии 3 на 4: Добавление мягкого удаления и favicon")
                db.execSQL("ALTER TABLE passwords ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE passwords ADD COLUMN deletionTimestamp INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE passwords ADD COLUMN websiteUrl TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE passwords ADD COLUMN faviconData BLOB DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_passwords_isDeleted ON passwords(isDeleted)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("DB_MIGRATION", "Миграция базы данных с версии 4 на 5: Добавление таблицы 'security_log'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `security_log` (
                        `eventId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `description` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_security_log_timestamp` ON `security_log` (`timestamp`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("DB_MIGRATION", "Миграция базы данных с версии 5 на 6: Добавление таблицы 'user_profile'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `userId` TEXT NOT NULL PRIMARY KEY,  -- Firebase UID
                        `displayName` TEXT,
                        `email` TEXT,
                        `avatarPath` TEXT                    -- Путь к файлу аватара
                    )
                """.trimIndent())
                // Опционально: Индекс по email, если будете искать по нему
                // db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profile_email` ON `user_profile` (`email`)")
            }
        }
        // --- Конец Миграций ---


        /**
         * Получает единственный экземпляр базы данных (Singleton).
         * Если экземпляр еще не создан, он будет создан потокобезопасно.
         *
         * @param context Контекст приложения.
         * @return Экземпляр AppDatabase.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Если INSTANCE не null, возвращаем его.
            // Если null, создаем базу данных внутри synchronized блока.
            return INSTANCE ?: synchronized(this) {
                Log.d("AppDatabase", "Создание или получение экземпляра базы данных...")
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Используем контекст приложения
                    AppDatabase::class.java,    // Класс базы данных
                    DATABASE_NAME               // Имя файла базы данных
                )
                    // Добавляем все миграции по порядку
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6 // <-- Добавлена последняя миграция
                    )
                    // Включить во время разработки для автоматического удаления и пересоздания БД при несовпадении схемы.
                    // НЕ ИСПОЛЬЗОВАТЬ В ПРОДАШЕНЕ!
                    // .fallbackToDestructiveMigration()
                    // Дополнительные опции (опционально):
                    // .allowMainThreadQueries() // НЕ РЕКОМЕНДУЕТСЯ! Только для тестов или очень простых случаев.
                    // .addCallback(roomDatabaseCallback) // Добавить Callback для действий при создании/открытии БД
                    .build()
                INSTANCE = instance // Присваиваем созданный экземпляр
                Log.i("AppDatabase", "Экземпляр базы данных '$DATABASE_NAME' готов.")
                // Возвращаем экземпляр
                instance
            }
        }

        // Опциональный Callback (пример)
        /*
        private val roomDatabaseCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.i("AppDatabase", "База данных создана впервые.")
                // Можно выполнить начальное заполнение данными здесь (в фоновом потоке)
                // CoroutineScope(Dispatchers.IO).launch {
                //     INSTANCE?.categoryDao()?.insert(CategoryEntity(categoryName = "Initial"))
                // }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d("AppDatabase", "База данных открыта.")
                // Действия при каждом открытии БД
            }
        }
        */
    }
}