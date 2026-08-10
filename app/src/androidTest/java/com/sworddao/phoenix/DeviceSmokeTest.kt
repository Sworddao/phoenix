package com.sworddao.phoenix

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sworddao.phoenix.data.local.AppMetadataEntity
import com.sworddao.phoenix.data.local.MIGRATION_2_3
import com.sworddao.phoenix.data.local.MIGRATION_3_4
import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.feature.friendship.data.FriendshipEntity
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyEntity
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyProgressEntity
import com.sworddao.phoenix.feature.writing.data.WritingExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceSmokeTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE_NAME)
        context.deleteDatabase(MIGRATION_DB_NAME)
    }

    @Test
    fun appLaunchesToResumedState() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val reachedResumed = runCatching {
            runBlocking {
                withTimeout(90_000L) {
                    while (scenario.state != Lifecycle.State.RESUMED) {
                        kotlinx.coroutines.delay(250)
                    }
                }
            }
            true
        }.getOrDefault(false)
        runCatching { scenario.close() }
        assertTrue("MainActivity never reached RESUMED", reachedResumed)
    }

    @Test
    fun realDatabaseOpensAtLatestSchemaWithAllTables() {
        context.deleteDatabase(DATABASE_NAME)
        val database = Room.databaseBuilder(
            context,
            PhoenixDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()
        try {
            val sqlite = database.openHelper.readableDatabase
            assertEquals(4, sqlite.version)
            val missing = EXPECTED_TABLES - tableNames(sqlite)
            assertTrue("Missing tables on device: $missing", missing.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationFromV2PreservesDataOnDevice() {
        context.deleteDatabase(MIGRATION_DB_NAME)
        val helper = FrameworkSQLiteOpenHelperFactory()
            .create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(MIGRATION_DB_NAME)
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
        val db = helper.writableDatabase
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS `placeholder` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `friendship_state` (`npcId` TEXT NOT NULL, `friendshipXp` INTEGER NOT NULL, `friendshipLevel` TEXT NOT NULL, `totalConversations` INTEGER NOT NULL, `firstMeetingTimestamp` INTEGER NOT NULL, `lastInteractionTimestamp` INTEGER NOT NULL, `unlockedTopics` TEXT NOT NULL, `recentGifts` TEXT NOT NULL, `completedQuests` TEXT NOT NULL, PRIMARY KEY(`npcId`))"
            )
            db.execSQL(
                "INSERT INTO `friendship_state` (`npcId`, `friendshipXp`, `friendshipLevel`, `totalConversations`, `firstMeetingTimestamp`, `lastInteractionTimestamp`, `unlockedTopics`, `recentGifts`, `completedQuests`) VALUES ('v2_npc', 42, 'FRIEND', 3, 1000, 2000, '[]', '[]', '[]')"
            )
            MIGRATION_2_3.migrate(db)
            MIGRATION_3_4.migrate(db)
            val missing = EXPECTED_TABLES - tableNames(db)
            assertTrue("Missing tables after migration on device: $missing", missing.isEmpty())
            val cursor = db.query(
                "SELECT friendshipXp, friendshipLevel, totalConversations FROM friendship_state WHERE npcId = 'v2_npc'"
            )
            assertTrue(cursor.moveToFirst())
            assertEquals(42, cursor.getInt(0))
            assertEquals("FRIEND", cursor.getString(1))
            assertEquals(3, cursor.getInt(2))
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun roomRoundTripPersistsOnDevice() {
        val database = Room.inMemoryDatabaseBuilder(context, PhoenixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                database.vocabularyDao().upsert(
                    VocabularyEntity(
                        id = "w_ni_hao",
                        mandarin = "你好",
                        pinyin = "nǐ hǎo",
                        english = "hello",
                        hanzi = "你好",
                        category = "greeting",
                        difficulty = "BEGINNER",
                        exampleSentence = "你好！",
                        exampleTranslation = "Hello!",
                        examplePinyin = "nǐ hǎo",
                        mastery = "NEW"
                    )
                )
                database.vocabularyDao().upsertProgress(
                    VocabularyProgressEntity(wordId = "w_ni_hao", mastery = "NEW")
                )
                assertEquals(1, database.vocabularyDao().countWords())
                assertTrue(database.vocabularyDao().getAllWords().first().isNotEmpty())
                assertTrue(database.vocabularyDao().searchWords("你好").first().isNotEmpty())

                database.friendshipDao().upsertFriendshipState(FriendshipEntity(npcId = "npc_1"))
                assertTrue(database.friendshipDao().getFriendshipState("npc_1").first() != null)

                database.appMetadataDao().setValue(AppMetadataEntity("smoke_test", "1"))
                assertEquals("1", database.appMetadataDao().getValue("smoke_test"))

                database.writingDao().upsertExercise(
                    WritingExerciseEntity(
                        id = "write_ni_hao",
                        type = "TRACE_STROKES",
                        difficulty = "BEGINNER",
                        wordId = "w_ni_hao",
                        characterJson = "{}",
                        exerciseJson = "{}",
                    )
                )
                assertTrue(database.writingDao().getExerciseById("write_ni_hao").first() != null)
            }
        } finally {
            database.close()
        }
    }

    private fun tableNames(db: SupportSQLiteDatabase): Set<String> {
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

    companion object {
        private const val DATABASE_NAME = "phoenix_database"
        private const val MIGRATION_DB_NAME = "smoke_migration.db"

        private val EXPECTED_TABLES = setOf(
            "placeholder",
            "app_metadata",
            "friendship_state",
            "conversation_memory",
            "friendship_event",
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
            "progression_objectives",
            "writing_exercise",
            "writing_progress_doc",
            "writing_statistics",
            "writing_badges",
            "writing_sessions",
            "writing_state"
        )
    }
}
