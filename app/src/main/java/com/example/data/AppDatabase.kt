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

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE calendar_events ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE calendar_events ADD COLUMN seriesId TEXT DEFAULT NULL")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shopping_lists` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdByMemberId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `firestoreId` TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shopping_lists_firestoreId ON shopping_lists(firestoreId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shops` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `listId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `firestoreId` TEXT,
                FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_shops_listId ON shops(listId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shops_firestoreId ON shops(firestoreId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shopping_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shopId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `isChecked` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `firestoreId` TEXT,
                FOREIGN KEY(`shopId`) REFERENCES `shops`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_items_shopId ON shopping_items(shopId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shopping_items_firestoreId ON shopping_items(firestoreId)")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shopping_items ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")

        db.execSQL("ALTER TABLE polls ADD COLUMN mode TEXT NOT NULL DEFAULT 'VOTE'")
        db.execSQL("ALTER TABLE polls ADD COLUMN spinResultIndex INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE polls ADD COLUMN spinStartedAt INTEGER DEFAULT NULL")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `poll_options` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `pollId` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                `createdByMemberId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `firestoreId` TEXT,
                FOREIGN KEY(`pollId`) REFERENCES `polls`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_poll_options_pollId ON poll_options(pollId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_poll_options_firestoreId ON poll_options(firestoreId)")
    }
}

@Database(
    entities = [FamilyMember::class, SavingsGoal::class, Contribution::class, CalendarEvent::class, FamilyTask::class, ChatMessage::class, Poll::class, PollVote::class, PollOption::class, ShoppingList::class, Shop::class, ShoppingItem::class],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savingsDao(): SavingsDao
    abstract fun calendarDao(): CalendarDao
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao
    abstract fun pollDao(): PollDao
    abstract fun shoppingListDao(): ShoppingListDao

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
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
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
