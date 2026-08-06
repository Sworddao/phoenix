package com.sworddao.phoenix.feature.passport.data

import org.junit.Assert.*
import org.junit.Test

class PassportModelsTest {

    @Test
    fun `Passport has correct default values`() {
        val passport = Passport()
        assertEquals("player_passport", passport.id)
        assertEquals("", passport.playerName)
        assertTrue(passport.createdAt > 0)
        assertTrue(passport.lastUpdated > 0)
        assertEquals(0, passport.totalStamps)
        assertEquals(0, passport.totalCollectibles)
        assertEquals(0, passport.totalDiscoveries)
        assertEquals(1, passport.currentChapter)
        assertTrue(passport.regions.isEmpty())
        assertTrue(passport.collectibles.isEmpty())
        assertTrue(passport.timeline.isEmpty())
    }

    @Test
    fun `Passport completionPercentage calculates correctly`() {
        val passport = Passport(
            regions = mapOf(
                "region1" to PassportRegion(regionId = "region1", regionName = "Region 1", regionNameCn = "区域1", isCompleted = true),
                "region2" to PassportRegion(regionId = "region2", regionName = "Region 2", regionNameCn = "区域2", isCompleted = false)
            )
        )
        assertEquals(0.5f, passport.completionPercentage, 0.01f)
    }

    @Test
    fun `Passport completionPercentage returns zero for empty regions`() {
        val passport = Passport()
        assertEquals(0f, passport.completionPercentage, 0.01f)
    }

    @Test
    fun `Passport collectedCount returns count of collected items`() {
        val passport = Passport(
            collectibles = mapOf(
                "c1" to Collectible(id = "c1", name = "Item 1", nameCn = "物品1", category = CollectibleCategory.TEA, regionId = "r1", description = "Desc", isCollected = true),
                "c2" to Collectible(id = "c2", name = "Item 2", nameCn = "物品2", category = CollectibleCategory.TEA, regionId = "r1", description = "Desc", isCollected = false),
                "c3" to Collectible(id = "c3", name = "Item 3", nameCn = "物品3", category = CollectibleCategory.TEA, regionId = "r1", description = "Desc", isCollected = true)
            )
        )
        assertEquals(2, passport.collectedCount)
    }

    @Test
    fun `Passport totalCount returns total number of items`() {
        val passport = Passport(
            collectibles = mapOf(
                "c1" to Collectible(id = "c1", name = "Item 1", nameCn = "物品1", category = CollectibleCategory.TEA, regionId = "r1", description = "Desc"),
                "c2" to Collectible(id = "c2", name = "Item 2", nameCn = "物品2", category = CollectibleCategory.TEA, regionId = "r1", description = "Desc"),
                "c3" to Collectible(id = "c3", name = "Item 3", nameCn = "物品3", category = CollectibleCategory.TEA, regionId = "r1", description = "Desc")
            )
        )
        assertEquals(3, passport.totalCount)
    }

    @Test
    fun `Passport stampCount returns count of regions with stamps`() {
        val passport = Passport(
            regions = mapOf(
                "r1" to PassportRegion(regionId = "r1", regionName = "Region 1", regionNameCn = "区域1", stampEarned = true),
                "r2" to PassportRegion(regionId = "r2", regionName = "Region 2", regionNameCn = "区域2", stampEarned = false),
                "r3" to PassportRegion(regionId = "r3", regionName = "Region 3", regionNameCn = "区域3", stampEarned = true)
            )
        )
        assertEquals(2, passport.stampCount)
    }

    @Test
    fun `PassportRegion has correct default values`() {
        val region = PassportRegion(regionId = "test", regionName = "Test", regionNameCn = "测试")
        assertEquals("test", region.regionId)
        assertEquals("Test", region.regionName)
        assertEquals("测试", region.regionNameCn)
        assertFalse(region.isDiscovered)
        assertFalse(region.isCompleted)
        assertFalse(region.stampEarned)
        assertEquals(StampRarity.BRONZE, region.stampRarity)
        assertEquals(0f, region.completionPercentage, 0.01f)
        assertEquals(0, region.totalPlayTimeMinutes)
        assertEquals("", region.notes)
    }

    @Test
    fun `PassportRegion collectibleProgress calculates correctly`() {
        val region = PassportRegion(
            regionId = "test",
            regionName = "Test",
            regionNameCn = "测试",
            collectiblesFound = 3,
            collectiblesTotal = 10
        )
        assertEquals(0.3f, region.collectibleProgress, 0.01f)
    }

    @Test
    fun `PassportRegion isFullyExplored returns true when 100 percent complete`() {
        val region = PassportRegion(regionId = "test", regionName = "Test", regionNameCn = "测试", completionPercentage = 100f, isCompleted = true, collectiblesFound = 5, collectiblesTotal = 5)
        assertTrue(region.isFullyExplored)
    }

    @Test
    fun `PassportRegion isFullyExplored returns false when less than 100 percent`() {
        val region = PassportRegion(regionId = "test", regionName = "Test", regionNameCn = "测试", completionPercentage = 99f)
        assertFalse(region.isFullyExplored)
    }

    @Test
    fun `CollectibleCategory has all expected entries`() {
        assertEquals(20, CollectibleCategory.entries.size)
        assertTrue(CollectibleCategory.entries.contains(CollectibleCategory.TEA))
        assertTrue(CollectibleCategory.entries.contains(CollectibleCategory.JADE))
        assertTrue(CollectibleCategory.entries.contains(CollectibleCategory.STAMP))
    }

    @Test
    fun `CollectibleCategory has correct displayName and icon`() {
        assertEquals("茶艺", CollectibleCategory.TEA.displayName)
        assertEquals("🍵", CollectibleCategory.TEA.icon)
        assertEquals("玉石", CollectibleCategory.JADE.displayName)
        assertEquals("💚", CollectibleCategory.JADE.icon)
    }

    @Test
    fun `CollectibleRarity has correct drop chances`() {
        assertEquals(0.5f, CollectibleRarity.COMMON.dropChance, 0.01f)
        assertEquals(0.3f, CollectibleRarity.UNCOMMON.dropChance, 0.01f)
        assertEquals(0.15f, CollectibleRarity.RARE.dropChance, 0.01f)
        assertEquals(0.04f, CollectibleRarity.EPIC.dropChance, 0.01f)
        assertEquals(0.01f, CollectibleRarity.LEGENDARY.dropChance, 0.01f)
    }

    @Test
    fun `StampRarity has correct values in order`() {
        val rarities = StampRarity.entries
        assertEquals(5, rarities.size)
        assertEquals(StampRarity.BRONZE, rarities[0])
        assertEquals(StampRarity.SILVER, rarities[1])
        assertEquals(StampRarity.GOLD, rarities[2])
        assertEquals(StampRarity.PLATINUM, rarities[3])
        assertEquals(StampRarity.DIAMOND, rarities[4])
    }

    @Test
    fun `Collectible has correct default values`() {
        val collectible = Collectible(id = "c1", name = "Test", nameCn = "测试", category = CollectibleCategory.TEA, regionId = "r1", description = "Description")
        assertEquals("c1", collectible.id)
        assertEquals("Test", collectible.name)
        assertEquals("测试", collectible.nameCn)
        assertEquals(CollectibleCategory.TEA, collectible.category)
        assertEquals(CollectibleRarity.COMMON, collectible.rarity)
        assertEquals(CollectibleSource.EXPLORATION, collectible.source)
        assertEquals("Description", collectible.description)
        assertNull(collectible.culturalNote)
        assertEquals("r1", collectible.regionId)
        assertFalse(collectible.isCollected)
        assertFalse(collectible.isHidden)
        assertFalse(collectible.isDisplayed)
        assertTrue(collectible.tradeable)
        assertEquals(10, collectible.xpValue)
    }

    @Test
    fun `EntryType has all expected entries`() {
        assertEquals(13, EntryType.entries.size)
        assertTrue(EntryType.entries.contains(EntryType.STAMP_EARNED))
        assertTrue(EntryType.entries.contains(EntryType.COLLECTIBLE_FOUND))
        assertTrue(EntryType.entries.contains(EntryType.ACHIEVEMENT_UNLOCKED))
        assertTrue(EntryType.entries.contains(EntryType.SPEAKING_PRACTICE))
        assertTrue(EntryType.entries.contains(EntryType.LISTENING_PRACTICE))
        assertTrue(EntryType.entries.contains(EntryType.READING_PRACTICE))
    }

    @Test
    fun `CollectionProgress has correct default values`() {
        val progress = CollectionProgress()
        assertEquals(0, progress.totalCollectibles)
        assertEquals(0, progress.collectedCount)
        assertTrue(progress.categoryProgress.isEmpty())
        assertTrue(progress.rarityProgress.isEmpty())
        assertTrue(progress.regionProgress.isEmpty())
    }

    @Test
    fun `AchievementProgress has correct default values`() {
        val achievement = AchievementProgress(id = "ach1", name = "Test", nameCn = "测试", description = "Description", icon = "⭐")
        assertEquals("ach1", achievement.id)
        assertEquals("Test", achievement.name)
        assertEquals("测试", achievement.nameCn)
        assertEquals("Description", achievement.description)
        assertEquals("⭐", achievement.icon)
        assertFalse(achievement.isUnlocked)
        assertEquals(0, achievement.currentCount)
        assertEquals(1, achievement.requiredCount)
        assertNull(achievement.unlockedAt)
    }

    @Test
    fun `DiscoveryEvent has correct default values`() {
        val event = DiscoveryEvent(id = "evt1", type = EntryType.REGION_DISCOVERED, title = "Test Event", description = "Description", regionId = "r1")
        assertEquals("evt1", event.id)
        assertEquals(EntryType.REGION_DISCOVERED, event.type)
        assertEquals("r1", event.regionId)
        assertEquals("Test Event", event.title)
        assertEquals("Description", event.description)
        assertTrue(event.timestamp > 0)
    }

    @Test
    fun `PassportResult Success contains message`() {
        val result = PassportResult.Success("Operation successful")
        assertTrue(result is PassportResult.Success)
        assertEquals("Operation successful", result.message)
    }

    @Test
    fun `PassportResult Error contains message`() {
        val result = PassportResult.Error("Something went wrong")
        assertTrue(result is PassportResult.Error)
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun `PassportResult StampEarned contains region and rarity`() {
        val result = PassportResult.StampEarned("region1", StampRarity.GOLD)
        assertTrue(result is PassportResult.StampEarned)
        assertEquals("region1", result.regionId)
        assertEquals(StampRarity.GOLD, result.rarity)
    }

    @Test
    fun `PassportResult CollectibleFound contains collectibleId`() {
        val result = PassportResult.CollectibleFound("c1")
        assertTrue(result is PassportResult.CollectibleFound)
        assertEquals("c1", result.collectibleId)
    }

    @Test
    fun `PassportResult AchievementUnlocked contains achievementId`() {
        val result = PassportResult.AchievementUnlocked("ach1")
        assertTrue(result is PassportResult.AchievementUnlocked)
        assertEquals("ach1", result.achievementId)
    }

    @Test
    fun `PassportResult RegionCompleted contains regionId`() {
        val result = PassportResult.RegionCompleted("region1")
        assertTrue(result is PassportResult.RegionCompleted)
        assertEquals("region1", result.regionId)
    }
}
