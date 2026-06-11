package com.adam.habituator.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, HabitItemEntity::class, LogEntryEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun habitItemDao(): HabitItemDao
    abstract fun logEntryDao(): LogEntryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Adds the optional category-level weekly goal. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN weeklyGoalCount INTEGER")
            }
        }

        /** Makes the per-habit weekly goal optional, since a habit's category may set its own goal instead. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE habit_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        weeklyGoalCount INTEGER,
                        tracksQuantity INTEGER NOT NULL,
                        quantityUnit TEXT,
                        quantityGoalPerSession REAL,
                        reminderEnabled INTEGER NOT NULL,
                        archived INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO habit_items_new (id, categoryId, name, weeklyGoalCount, tracksQuantity,
                        quantityUnit, quantityGoalPerSession, reminderEnabled, archived, createdAt)
                    SELECT id, categoryId, name, weeklyGoalCount, tracksQuantity,
                        quantityUnit, quantityGoalPerSession, reminderEnabled, archived, createdAt
                    FROM habit_items
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE habit_items")
                db.execSQL("ALTER TABLE habit_items_new RENAME TO habit_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_items_categoryId ON habit_items(categoryId)")
            }
        }

        /** Adds per-habit reminder scheduling: which days of the week, and an optional time of day. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE habit_items ADD COLUMN reminderDaysOfWeek INTEGER NOT NULL DEFAULT ${HabitItemEntity.ALL_DAYS_MASK}"
                )
                db.execSQL("ALTER TABLE habit_items ADD COLUMN reminderTimeMinutes INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habituator.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
