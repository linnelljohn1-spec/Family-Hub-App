package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_messages` (
                `clientMessageId` TEXT NOT NULL,
                `senderMemberId` INTEGER NOT NULL,
                `senderName` TEXT NOT NULL,
                `senderAvatarColorHex` TEXT NOT NULL,
                `text` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `syncGroupCode` TEXT NOT NULL,
                PRIMARY KEY(`clientMessageId`)
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [FamilyMember::class, SavingsGoal::class, Contribution::class, CalendarEvent::class, FamilyTask::class, ChatMessage::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savingsDao(): SavingsDao
    abstract fun calendarDao(): CalendarDao
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "family_savings_db"
                )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
