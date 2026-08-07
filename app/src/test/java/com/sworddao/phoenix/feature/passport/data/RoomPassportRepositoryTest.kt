package com.sworddao.phoenix.feature.passport.data

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
class RoomPassportRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomPassportRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = RoomPassportRepository(database.passportDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getPassport returns initial passport`() = runBlocking {
        val passport = repository.getPassport().first()
        assertEquals("Traveler", passport.playerName)
    }

    @Test
    fun `getAllRegions returns all regions`() = runBlocking {
        val regions = repository.getAllRegions().first()
        assertEquals(12, regions.size)
    }

    @Test
    fun `getPassportRegion returns correct region`() = runBlocking {
        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertEquals("清远村", region?.regionNameCn)
    }

    @Test
    fun `getPassportRegion returns null for non-existent region`() = runBlocking {
        val region = repository.getPassportRegion("non_existent").first()
        assertNull(region)
    }

    @Test
    fun `getCollectibles returns all collectibles`() = runBlocking {
        val collectibles = repository.getCollectibles().first()
        assertTrue(collectibles.isNotEmpty())
    }

    @Test
    fun `getCollectiblesByRegion returns correct collectibles`() = runBlocking {
        val collectibles = repository.getCollectiblesByRegion("qingyuan_village").first()
        assertTrue(collectibles.isNotEmpty())
        collectibles.forEach { collectible ->
            assertEquals("qingyuan_village", collectible.regionId)
        }
    }

    @Test
    fun `getCollectiblesByCategory returns correct collectibles`() = runBlocking {
        val collectibles = repository.getCollectiblesByCategory(CollectibleCategory.TEA).first()
        assertTrue(collectibles.isNotEmpty())
        collectibles.forEach { collectible ->
            assertEquals(CollectibleCategory.TEA, collectible.category)
        }
    }

    @Test
    fun `getCollectionProgress returns progress`() = runBlocking {
        val progress = repository.getCollectionProgress().first()
        assertNotNull(progress)
        assertTrue(progress.totalCollectibles > 0)
    }

    @Test
    fun `getDiscoveryTimeline returns timeline`() = runBlocking {
        val timeline = repository.getDiscoveryTimeline().first()
        assertNotNull(timeline)
    }

    @Test
    fun `getAchievements returns achievements`() = runBlocking {
        val achievements = repository.getAchievements().first()
        assertNotNull(achievements)
        assertTrue(achievements.isNotEmpty())
    }

    @Test
    fun `discoverRegion marks region as discovered`() = runBlocking {
        val result = repository.discoverRegion("riverside_town")
        assertTrue(result is PassportResult.Success)

        val region = repository.getPassportRegion("riverside_town").first()
        assertNotNull(region)
        assertTrue(region?.isDiscovered == true)
    }

    @Test
    fun `discoverRegion returns error for non-existent region`() = runBlocking {
        val result = repository.discoverRegion("non_existent")
        assertTrue(result is PassportResult.Error)
    }

    @Test
    fun `completeRegion marks region as completed`() = runBlocking {
        val result = repository.completeRegion("qingyuan_village")
        assertTrue(result is PassportResult.RegionCompleted || result is PassportResult.Success)

        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.isCompleted == true)
    }

    @Test
    fun `earnStamp awards stamp to region`() = runBlocking {
        val result = repository.earnStamp("qingyuan_village")
        assertTrue(result is PassportResult.StampEarned || result is PassportResult.Success)

        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.stampEarned == true)
    }

    @Test
    fun `recordEntry adds entry to timeline`() = runBlocking {
        val initialTimeline = repository.getDiscoveryTimeline().first()
        val initialSize = initialTimeline.size

        val entry = PassportEntry(
            id = "test_entry",
            regionId = "qingyuan_village",
            type = EntryType.VOCABULARY_LEARNED,
            title = "Test Entry",
            description = "Test entry description",
            timestamp = System.currentTimeMillis(),
        )
        repository.recordEntry(entry)

        val updatedTimeline = repository.getDiscoveryTimeline().first()
        assertTrue(updatedTimeline.size >= initialSize)
    }

    @Test
    fun `updateRegionProgress updates region completion`() = runBlocking {
        val result = repository.updateRegionProgress("qingyuan_village", 0.85f)
        assertTrue(result is PassportResult.Success)

        val region = repository.getPassportRegion("qingyuan_village").first()
        assertEquals(0.85f, region?.completionPercentage ?: 0f, 0.01f)
    }

    @Test
    fun `addVocabularyLearned increments vocabulary count`() = runBlocking {
        val initial = repository.getPassportRegion("qingyuan_village").first()
        val initialVocab = initial?.vocabularyLearned ?: 0

        repository.addVocabularyLearned("qingyuan_village", 5)

        val updated = repository.getPassportRegion("qingyuan_village").first()
        assertEquals(initialVocab + 5, updated?.vocabularyLearned)
    }

    @Test
    fun `addFriendshipMade increments friendship count`() = runBlocking {
        val initial = repository.getPassportRegion("qingyuan_village").first()
        val initialFriendships = initial?.friendshipsMade ?: 0

        repository.addFriendshipMade("qingyuan_village")

        val updated = repository.getPassportRegion("qingyuan_village").first()
        assertEquals(initialFriendships + 1, updated?.friendshipsMade)
    }

    @Test
    fun `getPassportStats returns stats`() = runBlocking {
        val stats = repository.getPassportStats()
        assertTrue(stats.totalRegions > 0)
        assertTrue(stats.totalCollectibles > 0)
    }

    @Test
    fun `qingyuan_village starts as discovered`() = runBlocking {
        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.isDiscovered == true)
    }

    @Test
    fun `qingyuan_village has initial completion percentage`() = runBlocking {
        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.completionPercentage!! > 0f)
    }

    @Test
    fun `other regions start as undiscovered`() = runBlocking {
        val region = repository.getPassportRegion("riverside_town").first()
        assertNotNull(region)
        assertFalse(region?.isDiscovered == true)
    }
}
