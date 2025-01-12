package com.example.todoapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TodoModel::class, User::class, Reminder::class], // Removed Category
    version = 8 // Incremented version number
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao
    abstract fun userDao(): UserDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        private const val DB_NAME = "todo_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 4 to 5
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new TodoModel table without the categoryId column
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS TodoModel_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        priority TEXT NOT NULL DEFAULT 'Low',
                        date INTEGER NOT NULL,
                        time INTEGER NOT NULL,
                        isFinished INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        userId INTEGER NOT NULL,
                        completed INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Migrate data from the old table to the new one
                database.execSQL(
                    """
                    INSERT INTO TodoModel_new (id, title, description, priority, date, time, isFinished, isDeleted, userId, completed)
                    SELECT id, title, description, priority, date, time, isFinished, isDeleted, userId, completed
                    FROM TodoModel
                    """.trimIndent()
                )

                // Drop the old TodoModel table
                database.execSQL("DROP TABLE TodoModel")

                // Rename the new table to the original table name
                database.execSQL("ALTER TABLE TodoModel_new RENAME TO TodoModel")
            }
        }

        // Migration from version 5 to 6
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the Reminder table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS Reminder (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        reminderTime INTEGER NOT NULL,
                        FOREIGN KEY (taskId) REFERENCES TodoModel(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Add an index for taskId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_Reminder_taskId ON Reminder(taskId)")
            }
        }

        // Migration from version 6 to 7
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the 'completed' column to the TodoModel table
                database.execSQL("ALTER TABLE TodoModel ADD COLUMN completed INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from version 7 to 8
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove categoryId column from TodoModel (as we're no longer using Category)
                database.execSQL("CREATE TABLE IF NOT EXISTS TodoModel_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "title TEXT NOT NULL," +
                        "description TEXT NOT NULL," +
                        "priority TEXT NOT NULL DEFAULT 'Low'," +
                        "date INTEGER NOT NULL," +
                        "time INTEGER NOT NULL," +
                        "isFinished INTEGER NOT NULL DEFAULT 0," +
                        "isDeleted INTEGER NOT NULL DEFAULT 0," +
                        "userId INTEGER NOT NULL," +
                        "completed INTEGER NOT NULL DEFAULT 0" +
                        ")"
                )

                // Copy the data from the old TodoModel table to the new one (without categoryId)
                database.execSQL(
                    """
                    INSERT INTO TodoModel_new (id, title, description, priority, date, time, isFinished, isDeleted, userId, completed)
                    SELECT id, title, description, priority, date, time, isFinished, isDeleted, userId, completed
                    FROM TodoModel
                    """.trimIndent()
                )

                // Drop the old TodoModel table
                database.execSQL("DROP TABLE TodoModel")

                // Rename the new table to TodoModel
                database.execSQL("ALTER TABLE TodoModel_new RENAME TO TodoModel")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration() // Forces clearing the database on version mismatch
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()

                INSTANCE = instance
                return instance
            }
        }

    }
}