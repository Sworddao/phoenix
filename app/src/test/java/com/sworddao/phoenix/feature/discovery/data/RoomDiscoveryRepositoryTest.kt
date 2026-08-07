package com.sworddao.phoenix.feature.discovery.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDiscoveryRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomDiscoveryRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = RoomDiscoveryRepository(
            database.discoveryDao(),
            database.vocabularyDao(),
            database.appMetadataDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getAllDiscoveries returns initial discoveries`() = runBlocking {
        val discoveries = repository.getAllDiscoveries().first()
        assertTrue(discoveries.isNotEmpty())
        assertEquals(8, discoveries.size)
    }

    @Test
    fun `getDiscoveryById returns correct discovery`() = runBlocking {
        val discovery = repository.getDiscoveryById("disc_001").first()
        assertNotNull(discovery)
        assertEquals("greet_001", discovery?.wordId)
        assertEquals(DiscoverySourceType.NPC, discovery?.source)
    }

    @Test
    fun `getDiscoveryById returns null for non-existent`() = runBlocking {
        val discovery = repository.getDiscoveryById("non_existent").first()
        assertNull(discovery)
    }

    @Test
    fun `getDiscoveriesByWord returns correct discoveries`() = runBlocking {
        val discoveries = repository.getDiscoveriesByWord("greet_001").first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.wordId == "greet_001" })
    }

    @Test
    fun `getDiscoveriesBySource returns correct discoveries`() = runBlocking {
        val discoveries = repository.getDiscoveriesBySource(DiscoverySourceType.NPC).first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.source == DiscoverySourceType.NPC })
    }

    @Test
    fun `getDiscoveriesByNpc returns correct discoveries`() = runBlocking {
        val discoveries = repository.getDiscoveriesByNpc("grandma_mei").first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.relatedNpcId == "grandma_mei" })
    }

    @Test
    fun `getRecentDiscoveries returns limited results`() = runBlocking {
        val discoveries = repository.getRecentDiscoveries(3).first()
        assertEquals(3, discoveries.size)
    }

    @Test
    fun `getStreakDays returns streak`() = runBlocking {
        val streak = repository.getStreakDays().first()
        assertTrue(streak >= 0)
    }

    @Test
    fun `discoverWord succeeds for undiscovered word`() = runBlocking {
        val result = repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.NPC,
            sourceId = "grandma_mei",
            sourceName = "Grandma Mei",
            relatedNpcId = "grandma_mei",
            relatedRegionId = "qingyuan_village",
        )

        assertTrue(result is DiscoveryResult.WordDiscovered)
        val discovered = result as DiscoveryResult.WordDiscovered
        assertEquals("greet_002", discovered.word.id)
        assertTrue(discovered.isFirstDiscovery)
        assertTrue(discovered.reward.xp > 0)
    }

    @Test
    fun `discoverWord returns already discovered for known word`() = runBlocking {
        val result = repository.discoverWord(
            wordId = "greet_001",
            source = DiscoverySourceType.NPC,
            sourceId = "grandma_mei",
            sourceName = "Grandma Mei",
        )

        assertTrue(result is DiscoveryResult.WordAlreadyDiscovered)
    }

    @Test
    fun `discoverWords returns batch result`() = runBlocking {
        val result = repository.discoverWords(
            wordIds = listOf("greet_002", "greet_004"),
            source = DiscoverySourceType.QUEST,
            sourceId = "quest_001",
            sourceName = "Test Quest",
        )

        assertTrue(result is DiscoveryResult.BatchDiscovered)
        val batch = result as DiscoveryResult.BatchDiscovered
        assertTrue(batch.words.isNotEmpty())
        assertTrue(batch.totalXp > 0)
    }

    @Test
    fun `isWordDiscovered returns true for discovered word`() = runBlocking {
        val discovered = repository.isWordDiscovered("greet_001")
        assertTrue(discovered)
    }

    @Test
    fun `isWordDiscovered returns false for undiscovered word`() = runBlocking {
        val discovered = repository.isWordDiscovered("greet_002")
        assertFalse(discovered)
    }

    @Test
    fun `getDiscoveryCount returns correct count`() = runBlocking {
        val count = repository.getDiscoveryCount()
        assertEquals(8, count)
    }

    @Test
    fun `getDiscoveryCountBySource returns correct count`() = runBlocking {
        val count = repository.getDiscoveryCountBySource(DiscoverySourceType.NPC)
        assertTrue(count > 0)
    }

    @Test
    fun `discovery increases count`() = runBlocking {
        val initialCount = repository.getDiscoveryCount()

        repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.NPC,
            sourceId = "grandma_mei",
            sourceName = "Grandma Mei",
        )

        val newCount = repository.getDiscoveryCount()
        assertEquals(initialCount + 1, newCount)
    }

    @Test
    fun `hidden discovery has highest xp`() = runBlocking {
        val result = repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.HIDDEN,
            sourceId = "hidden_001",
            sourceName = "Hidden Discovery",
        )

        assertTrue(result is DiscoveryResult.WordDiscovered)
        val discovered = result as DiscoveryResult.WordDiscovered
        assertTrue(discovered.reward.xp >= 25)
        assertTrue(discovered.reward.categoryBonus)
        assertTrue(discovered.reward.regionBonus)
    }

    @Test
    fun `recordDiscoverySession adds session`() = runBlocking {
        val session = DiscoverySession(
            id = "session_001",
            startTime = System.currentTimeMillis(),
            discoveries = emptyList(),
            source = DiscoverySourceType.DIALOGUE,
            sourceId = "dialogue_001",
        )

        repository.recordDiscoverySession(session)

        val sessions = repository.getDiscoverySessions().first()
        assertTrue(sessions.any { it.id == "session_001" })
    }

    @Test
    fun `clearDiscoveryHistory clears all data`() = runBlocking {
        repository.clearDiscoveryHistory()

        val count = repository.getDiscoveryCount()
        assertEquals(0, count)
    }
}
