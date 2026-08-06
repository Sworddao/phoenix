package com.sworddao.phoenix.feature.world.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldModelsTest {

    @Test
    fun `region is unlocked when status is not locked`() {
        val region = createTestRegion(status = RegionStatus.AVAILABLE)

        assertTrue(region.isUnlocked)
    }

    @Test
    fun `region is not unlocked when status is locked`() {
        val region = createTestRegion(status = RegionStatus.LOCKED)

        assertFalse(region.isUnlocked)
    }

    @Test
    fun `region is completed when status is completed`() {
        val region = createTestRegion(status = RegionStatus.COMPLETED)

        assertTrue(region.isCompleted)
    }

    @Test
    fun `region is not completed when status is not completed`() {
        val region = createTestRegion(status = RegionStatus.AVAILABLE)

        assertFalse(region.isCompleted)
    }

    @Test
    fun `region is current when status is current`() {
        val region = createTestRegion(status = RegionStatus.CURRENT)

        assertTrue(region.isCurrent)
    }

    @Test
    fun `region is not current when status is not current`() {
        val region = createTestRegion(status = RegionStatus.AVAILABLE)

        assertFalse(region.isCurrent)
    }

    @Test
    fun `unlock requirement has requirements when quest ids present`() {
        val requirement = UnlockRequirement(questIds = listOf("quest_1"))

        assertTrue(requirement.hasRequirements)
    }

    @Test
    fun `unlock requirement has requirements when friendship level present`() {
        val requirement = UnlockRequirement(npcFriendshipLevel = 2)

        assertTrue(requirement.hasRequirements)
    }

    @Test
    fun `unlock requirement has requirements when required regions present`() {
        val requirement = UnlockRequirement(requiredRegions = listOf("region_1"))

        assertTrue(requirement.hasRequirements)
    }

    @Test
    fun `unlock requirement has no requirements when empty`() {
        val requirement = UnlockRequirement()

        assertFalse(requirement.hasRequirements)
    }

    @Test
    fun `exploration progress calculates regions remaining correctly`() {
        val progress = ExplorationProgress(
            totalRegions = 12,
            completedRegions = 3,
        )

        assertEquals(9, progress.regionsRemaining)
    }

    @Test
    fun `exploration progress calculates locations remaining correctly`() {
        val progress = ExplorationProgress(
            totalLocations = 20,
            discoveredLocations = 5,
        )

        assertEquals(15, progress.locationsRemaining)
    }

    @Test
    fun `region progress tracks discovered locations`() {
        val progress = RegionProgress(
            regionId = "test_region",
            status = RegionStatus.VISITED,
            discoveredLocations = listOf("loc_1", "loc_2", "loc_3"),
        )

        assertEquals(3, progress.discoveredLocations.size)
    }

    @Test
    fun `region progress tracks collected items`() {
        val progress = RegionProgress(
            regionId = "test_region",
            status = RegionStatus.VISITED,
            collectedItems = listOf("item_1", "item_2"),
        )

        assertEquals(2, progress.collectedItems.size)
    }

    @Test
    fun `world result success contains message`() {
        val result = WorldResult.Success("Test message")

        assertEquals("Test message", result.message)
    }

    @Test
    fun `world result error contains message`() {
        val result = WorldResult.Error("Error message")

        assertEquals("Error message", result.message)
    }

    @Test
    fun `world result travel started contains region ids`() {
        val result = WorldResult.TravelStarted("from_region", "to_region")

        assertEquals("from_region", result.fromRegionId)
        assertEquals("to_region", result.toRegionId)
    }

    @Test
    fun `world result region unlocked contains region id`() {
        val result = WorldResult.RegionUnlocked("test_region")

        assertEquals("test_region", result.regionId)
    }

    @Test
    fun `world result location discovered contains location id`() {
        val result = WorldResult.LocationDiscovered("test_location")

        assertEquals("test_location", result.locationId)
    }

    @Test
    fun `world result collectible found contains collectible id`() {
        val result = WorldResult.CollectibleFound("test_collectible")

        assertEquals("test_collectible", result.collectibleId)
    }

    @Test
    fun `world region connections are correct`() {
        val region = createTestRegion(
            connections = listOf("region_1", "region_2"),
        )

        assertEquals(2, region.connections.size)
        assertEquals("region_1", region.connections[0])
        assertEquals("region_2", region.connections[1])
    }

    @Test
    fun `world region npc ids are correct`() {
        val region = createTestRegion(
            npcIds = listOf("npc_1", "npc_2", "npc_3"),
        )

        assertEquals(3, region.npcIds.size)
    }

    @Test
    fun `world region quest ids are correct`() {
        val region = createTestRegion(
            questIds = listOf("quest_1", "quest_2"),
        )

        assertEquals(2, region.questIds.size)
    }

    @Test
    fun `landmark type enum has expected values`() {
        val types = LandmarkType.entries

        assertEquals(20, types.size)
        assertTrue(types.contains(LandmarkType.TEMPLE))
        assertTrue(types.contains(LandmarkType.MARKET))
        assertTrue(types.contains(LandmarkType.RESTAURANT))
    }

    @Test
    fun `collectible type enum has expected values`() {
        val types = CollectibleType.entries

        assertEquals(16, types.size)
        assertTrue(types.contains(CollectibleType.TEA))
        assertTrue(types.contains(CollectibleType.BAMBOO))
        assertTrue(types.contains(CollectibleType.POSTCARD))
    }

    @Test
    fun `travel method enum has expected values`() {
        val methods = TravelMethod.entries

        assertEquals(7, methods.size)
        assertTrue(methods.contains(TravelMethod.WALKING))
        assertTrue(methods.contains(TravelMethod.BUS))
        assertTrue(methods.contains(TravelMethod.TRAIN))
        assertTrue(methods.contains(TravelMethod.HIGH_SPEED_RAIL))
    }

    @Test
    fun `region status enum has expected values`() {
        val statuses = RegionStatus.entries

        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(RegionStatus.LOCKED))
        assertTrue(statuses.contains(RegionStatus.AVAILABLE))
        assertTrue(statuses.contains(RegionStatus.VISITED))
        assertTrue(statuses.contains(RegionStatus.CURRENT))
        assertTrue(statuses.contains(RegionStatus.COMPLETED))
    }

    private fun createTestRegion(
        id: String = "test_region",
        name: String = "Test Region",
        nameCn: String = "测试区域",
        status: RegionStatus = RegionStatus.AVAILABLE,
        connections: List<String> = emptyList(),
        npcIds: List<String> = emptyList(),
        questIds: List<String> = emptyList(),
    ) = WorldRegion(
        id = id,
        name = name,
        nameCn = nameCn,
        description = "A test region",
        status = status,
        order = 1,
        chapter = 1,
        connections = connections,
        npcIds = npcIds,
        questIds = questIds,
    )
}
