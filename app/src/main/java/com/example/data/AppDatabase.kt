package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE family_members ADD COLUMN firestoreId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE savings_goals ADD COLUMN firestoreId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE contributions ADD COLUMN firestoreId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE calendar_events ADD COLUMN firestoreId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE family_tasks ADD COLUMN firestoreId TEXT DEFAULT NULL")

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_family_members_firestoreId ON family_members(firestoreId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_savings_goals_firestoreId ON savings_goals(firestoreId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contributions_firestoreId ON contributions(firestoreId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_calendar_events_firestoreId ON calendar_events(firestoreId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_family_tasks_firestoreId ON family_tasks(firestoreId)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `polls` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `question` TEXT NOT NULL,
                `options` TEXT NOT NULL,
                `createdByMemberId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `isClosed` INTEGER NOT NULL,
                `firestoreId` TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_polls_firestoreId ON polls(firestoreId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `poll_votes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `pollId` INTEGER NOT NULL,
                `memberId` INTEGER NOT NULL,
                `optionIndex` INTEGER NOT NULL,
                `votedAt` INTEGER NOT NULL,
                `firestoreId` TEXT,
                FOREIGN KEY(`pollId`) REFERENCES `polls`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_poll_votes_pollId_memberId ON poll_votes(pollId, memberId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_poll_votes_firestoreId ON poll_votes(firestoreId)")
    }
}

@Database(
    entities = [FamilyMember::class, SavingsGoal::class, Contribution::class, CalendarEvent::class, FamilyTask::class, ChatMessage::class, Poll::class, PollVote::class],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savingsDao(): SavingsDao
    abstract fun calendarDao(): CalendarDao
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao
    abstract fun pollDao(): PollDao

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
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
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
