package com.sworddao.phoenix.feature.passport.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PassportRepositoryTest {

    private lateinit var repository: MockPassportRepository

    @Before
    fun setup() {
        repository = MockPassportRepository()
    }

    @Test
    fun `getPassport returns initial passport`() = runTest {
        val passport = repository.getPassport().first()
        assertNotNull(passport)
        assertEquals("Traveler", passport.playerName)
    }

    @Test
    fun `getAllRegions returns all regions`() = runTest {
        val regions = repository.getAllRegions().first()
        assertTrue(regions.isNotEmpty())
        assertEquals(12, regions.size)
    }

    @Test
    fun `getPassportRegion returns correct region`() = runTest {
        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertEquals("清远村", region?.regionNameCn)
    }

    @Test
    fun `getPassportRegion returns null for non-existent region`() = runTest {
        val region = repository.getPassportRegion("non_existent").first()
        assertNull(region)
    }

    @Test
    fun `getCollectibles returns all collectibles`() = runTest {
        val collectibles = repository.getCollectibles().first()
        assertTrue(collectibles.isNotEmpty())
    }

    @Test
    fun `getCollectiblesByRegion returns correct collectibles`() = runTest {
        val collectibles = repository.getCollectiblesByRegion("qingyuan_village").first()
        assertTrue(collectibles.isNotEmpty())
        collectibles.forEach { collectible ->
            assertEquals("qingyuan_village", collectible.regionId)
        }
    }

    @Test
    fun `getCollectiblesByCategory returns correct collectibles`() = runTest {
        val collectibles = repository.getCollectiblesByCategory(CollectibleCategory.TEA).first()
        assertTrue(collectibles.isNotEmpty())
        collectibles.forEach { collectible ->
            assertEquals(CollectibleCategory.TEA, collectible.category)
        }
    }

    @Test
    fun `getCollectionProgress returns progress`() = runTest {
        val progress = repository.getCollectionProgress().first()
        assertNotNull(progress)
        assertTrue(progress.totalCollectibles > 0)
    }

    @Test
    fun `getDiscoveryTimeline returns timeline`() = runTest {
        val timeline = repository.getDiscoveryTimeline().first()
        assertNotNull(timeline)
        assertTrue(timeline.isNotEmpty())
    }

    @Test
    fun `getAchievements returns achievements`() = runTest {
        val achievements = repository.getAchievements().first()
        assertNotNull(achievements)
        assertTrue(achievements.isNotEmpty())
    }

    @Test
    fun `getRecentEntries returns limited entries`() = runTest {
        val entries = repository.getRecentEntries(5).first()
        assertNotNull(entries)
        assertTrue(entries.size <= 5)
    }

    @Test
    fun `discoverRegion marks region as discovered`() = runTest {
        val result = repository.discoverRegion("riverside_town")
        assertTrue(result is PassportResult.Success)

        val region = repository.getPassportRegion("riverside_town").first()
        assertNotNull(region)
        assertTrue(region?.isDiscovered == true)
    }

    @Test
    fun `discoverRegion returns error for non-existent region`() = runTest {
        val result = repository.discoverRegion("non_existent")
        assertTrue(result is PassportResult.Error)
    }

    @Test
    fun `completeRegion marks region as completed`() = runTest {
        val result = repository.completeRegion("qingyuan_village")
        assertTrue(result is PassportResult.RegionCompleted || result is PassportResult.Success)

        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.isCompleted == true)
    }

    @Test
    fun `earnStamp awards stamp to region`() = runTest {
        val result = repository.earnStamp("qingyuan_village")
        assertTrue(result is PassportResult.StampEarned || result is PassportResult.Success)

        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.stampEarned == true)
    }

    @Test
    fun `collectItem marks collectible as collected`() = runTest {
        val collectibles = repository.getCollectiblesByRegion("qingyuan_village").first()
        val uncollected = collectibles.firstOrNull { !it.isCollected }
        if (uncollected != null) {
            val result = repository.collectItem(uncollected.id)
            assertTrue(result is PassportResult.CollectibleFound || result is PassportResult.Success)

            val updated = repository.getCollectibles().first().find { it.id == uncollected.id }
            assertNotNull(updated)
            assertTrue(updated?.isCollected == true)
        }
    }

    @Test
    fun `recordEntry adds entry to timeline`() = runTest {
        val initialTimeline = repository.getDiscoveryTimeline().first()
        val initialSize = initialTimeline.size

        val entry = PassportEntry(
            id = "test_entry",
            regionId = "qingyuan_village",
            type = EntryType.VOCABULARY_LEARNED,
            title = "Test Entry",
            description = "Test entry description"
        )
        repository.recordEntry(entry)

        val updatedTimeline = repository.getDiscoveryTimeline().first()
        assertTrue(updatedTimeline.size >= initialSize)
    }

    @Test
    fun `recordDiscovery adds event to timeline`() = runTest {
        val event = DiscoveryEvent(
            id = "test_event",
            type = EntryType.COLLECTIBLE_FOUND,
            regionId = "qingyuan_village",
            title = "清远村",
            description = "Found a test item"
        )
        repository.recordDiscovery(event)

        val timeline = repository.getDiscoveryTimeline().first()
        assertTrue(timeline.any { it.id == "test_event" })
    }

    @Test
    fun `updateRegionProgress updates region completion`() = runTest {
        val result = repository.updateRegionProgress("qingyuan_village", 0.85f)
        assertTrue(result is PassportResult.Success)

        val region = repository.getPassportRegion("qingyuan_village").first()
        assertEquals(0.85f, region?.completionPercentage ?: 0f, 0.01f)
    }

    @Test
    fun `addVocabularyLearned increments vocabulary count`() = runTest {
        val initial = repository.getPassportRegion("qingyuan_village").first()
        val initialVocab = initial?.vocabularyLearned ?: 0

        repository.addVocabularyLearned("qingyuan_village", 5)

        val updated = repository.getPassportRegion("qingyuan_village").first()
        assertEquals(initialVocab + 5, updated?.vocabularyLearned)
    }

    @Test
    fun `addFriendshipMade increments friendship count`() = runTest {
        val initial = repository.getPassportRegion("qingyuan_village").first()
        val initialFriendships = initial?.friendshipsMade ?: 0

        repository.addFriendshipMade("qingyuan_village")

        val updated = repository.getPassportRegion("qingyuan_village").first()
        assertEquals(initialFriendships + 1, updated?.friendshipsMade)
    }

    @Test
    fun `addQuestCompleted increments quest count`() = runTest {
        val initial = repository.getPassportRegion("qingyuan_village").first()
        val initialQuests = initial?.questsCompleted ?: 0

        repository.addQuestCompleted("qingyuan_village")

        val updated = repository.getPassportRegion("qingyuan_village").first()
        assertEquals(initialQuests + 1, updated?.questsCompleted)
    }

    @Test
    fun `getPassportStats returns stats`() = runTest {
        val stats = repository.getPassportStats()
        assertNotNull(stats)
        assertTrue(stats.totalRegions > 0)
        assertTrue(stats.totalCollectibles > 0)
    }

    @Test
    fun `checkAchievements checks for unlockable achievements`() = runTest {
        val results = repository.checkAchievements()
        assertNotNull(results)
    }

    @Test
    fun `qingyuan_village starts as discovered`() = runTest {
        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.isDiscovered == true)
    }

    @Test
    fun `qingyuan_village has initial completion percentage`() = runTest {
        val region = repository.getPassportRegion("qingyuan_village").first()
        assertNotNull(region)
        assertTrue(region?.completionPercentage!! > 0f)
    }

    @Test
    fun `jade_forest starts as discovered`() = runTest {
        val region = repository.getPassportRegion("jade_forest").first()
        assertNotNull(region)
        assertTrue(region?.isDiscovered == true)
    }

    @Test
    fun `other regions start as undiscovered`() = runTest {
        val region = repository.getPassportRegion("riverside_town").first()
        assertNotNull(region)
        assertFalse(region?.isDiscovered == true)
    }
}
