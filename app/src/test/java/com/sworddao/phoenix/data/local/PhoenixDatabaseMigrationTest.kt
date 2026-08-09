package com.sworddao.phoenix.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.sworddao.phoenix.feature.friendship.data.ConversationMemoryEntity
import com.sworddao.phoenix.feature.friendship.data.FriendshipEntity
import com.sworddao.phoenix.feature.friendship.data.FriendshipEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhoenixDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("migration_test.db")
        val helper = FrameworkSQLiteOpenHelperFactory()
            .create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name("migration_test.db")
                    .callback(
                        object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
                            override fun onUpgrade(
                                db: androidx.sqlite.db.SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int
                            ) {}
                        }
                    )
                    .build()
            )
        db = helper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS `placeholder` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `friendship_state` (`npcId` TEXT NOT NULL, `friendshipXp` INTEGER NOT NULL, `friendshipLevel` TEXT NOT NULL, `totalConversations` INTEGER NOT NULL, `firstMeetingTimestamp` INTEGER NOT NULL, `lastInteractionTimestamp` INTEGER NOT NULL, `unlockedTopics` TEXT NOT NULL, `recentGifts` TEXT NOT NULL, `completedQuests` TEXT NOT NULL, PRIMARY KEY(`npcId`))"
        )
        db.execSQL(
            "INSERT INTO `friendship_state` (`npcId`, `friendshipXp`, `friendshipLevel`, `totalConversations`, `firstMeetingTimestamp`, `lastInteractionTimestamp`, `unlockedTopics`, `recentGifts`, `completedQuests`) VALUES ('v2_npc', 42, 'FRIEND', 3, 1000, 2000, '[]', '[]', '[]')"
        )
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase("migration_test.db")
    }

    private fun tableNames(): Set<String> {
        val names = mutableSetOf<String>()
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'"
        )
        while (cursor.moveToNext()) {
            names.add(cursor.getString(0))
        }
        cursor.close()
        return names
    }

    @Test
    fun `MIGRATION_2_3 creates all feature tables`() {
        MIGRATION_2_3.migrate(db)
        val expected = setOf(
            "placeholder",
            "friendship_state",
            "conversation_memory",
            "friendship_event",
            "app_metadata",
            "vocabulary_word",
            "vocabulary_progress",
            "vocabulary_discovery",
            "discovery_session",
            "quest",
            "quest_progress",
            "game_progress",
            "session_summary",
            "passport",
            "passport_region",
            "passport_collectible",
            "passport_event",
            "passport_achievement",
            "passport_entry",
            "world_region",
            "world_region_progress",
            "world_connection",
            "world_location",
            "world_landmark",
            "world_collectible",
            "reading_exercise",
            "reading_progress_doc",
            "reading_statistics",
            "reading_badges",
            "reading_sessions",
            "reading_state",
            "listening_exercise",
            "listening_progress_doc",
            "listening_statistics",
            "listening_badges",
            "listening_sessions",
            "listening_state",
            "speaking_exercise",
            "speaking_progress_doc",
            "speaking_statistics",
            "speaking_badges",
            "speaking_sessions",
            "speaking_state",
            "review_items",
            "review_memory",
            "review_schedules",
            "review_sessions",
            "review_history",
            "review_state",
            "review_snapshot",
            "review_stats",
            "review_published",
            "progression_state",
            "progression_snapshot",
            "progression_daily",
            "progression_recent",
            "progression_features",
            "progression_player",
            "progression_learning",
            "progression_objectives"
        )
        assertTrue("Missing tables: ${expected - tableNames()}", tableNames().containsAll(expected))
    }

    @Test
    fun `MIGRATION_2_3 preserves existing v2 friendship data`() {
        MIGRATION_2_3.migrate(db)
        val cursor = db.query(
            "SELECT friendshipXp, friendshipLevel, totalConversations FROM friendship_state WHERE npcId = 'v2_npc'"
        )
        assertTrue(cursor.moveToFirst())
        assertTrue(cursor.getInt(0) == 42)
        assertTrue(cursor.getString(1) == "FRIEND")
        assertTrue(cursor.getInt(2) == 3)
        cursor.close()
    }

    @Test
    fun `MIGRATION_2_3 is idempotent`() {
        MIGRATION_2_3.migrate(db)
        val tablesAfterFirst = tableNames()
        MIGRATION_2_3.migrate(db)
        assertTrue(tableNames() == tablesAfterFirst)
    }

    @Test
    fun `MIGRATION_3_4 creates writing tables and adds timesWritten columns`() {
        MIGRATION_2_3.migrate(db)
        db.execSQL(
            "INSERT INTO `vocabulary_word` (`id`, `mandarin`, `pinyin`, `english`, `category`, `difficulty`, `exampleSentence`, `exampleTranslation`, `examplePinyin`, `mastery`, `timesReviewed`, `timesSpoken`, `timesHeard`, `timesRead`, `isFavorite`, `tagsJson`) VALUES ('v3_word', 'hǎo', 'hǎo', 'good', 'GREETINGS', 'BEGINNER', 'e', 't', 'p', 'FAMILIAR', 0, 0, 0, 0, 0, '[]')"
        )
        MIGRATION_3_4.migrate(db)

        val expectedWritingTables = setOf(
            "writing_exercise",
            "writing_progress_doc",
            "writing_statistics",
            "writing_badges",
            "writing_sessions",
            "writing_state",
        )
        assertTrue(
            "Missing writing tables: ${expectedWritingTables - tableNames()}",
            tableNames().containsAll(expectedWritingTables)
        )

        val cursor = db.query("SELECT timesWritten FROM vocabulary_word WHERE id = 'v3_word'")
        assertTrue(cursor.moveToFirst())
        assertTrue(cursor.getInt(0) == 0)
        cursor.close()
    }

    @Test
    fun `MIGRATION_3_4 preserves existing v3 data`() {
        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)
        val cursor = db.query(
            "SELECT friendshipXp, friendshipLevel, totalConversations FROM friendship_state WHERE npcId = 'v2_npc'"
        )
        assertTrue(cursor.moveToFirst())
        assertTrue(cursor.getInt(0) == 42)
        assertTrue(cursor.getString(1) == "FRIEND")
        assertTrue(cursor.getInt(2) == 3)
        cursor.close()
    }

    @Test
    fun `v3 database can persist and read app metadata`() {
        val database = RoomTestDb.create()
        val dao = database.appMetadataDao()
        kotlinx.coroutines.runBlocking {
            dao.setValue(AppMetadataEntity("discovery_streak", "5"))
            assertTrue(dao.getValue("discovery_streak") == "5")
            dao.delete("discovery_streak")
            assertTrue(dao.getValue("discovery_streak") == null)
        }
        database.close()
    }

    @Test
    fun `friendship entities map to and from rows in v3 database`() {
        val database = RoomTestDb.create()
        val dao = database.friendshipDao()
        runBlocking {
            dao.upsertFriendshipState(FriendshipEntity(npcId = "npc_1"))
            dao.insertConversationMemory(
                ConversationMemoryEntity(
                    id = "conv_1",
                    npcId = "npc_1",
                    dialogueId = "dialogue_1",
                    dialogueTitle = "Intro",
                    timestamp = 1234L
                )
            )
            dao.insertFriendshipEvent(
                FriendshipEventEntity(
                    id = "event_1",
                    type = "CHAT",
                    npcId = "npc_1",
                    description = "talked",
                    timestamp = 1234L
                )
            )
            assertTrue(dao.getFriendshipState("npc_1").first() != null)
            assertTrue(dao.getAllFriendshipStates().first().isNotEmpty())
            assertTrue(dao.getConversationHistory("npc_1").first().isNotEmpty())
            assertTrue(dao.getFriendshipEvents("npc_1").first().isNotEmpty())
        }
        database.close()
    }
}
