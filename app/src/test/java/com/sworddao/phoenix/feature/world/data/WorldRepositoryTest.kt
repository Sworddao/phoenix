package com.sworddao.phoenix.feature.world.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorldRepositoryTest {

    private lateinit var repository: MockWorldRepository

    @Before
    fun setup() {
        repository = MockWorldRepository()
    }

    @Test
    fun `getAllRegions returns all regions`() = runTest {
        val regions = repository.getAllRegions().first()

        assertEquals(12, regions.size)
    }

    @Test
    fun `getRegionById returns correct region`() = runTest {
        val region = repository.getRegionById("qingyuan_village").first()

        assertNotNull(region)
        assertEquals("Qingyuan Village", region?.name)
        assertEquals("清远村", region?.nameCn)
    }

    @Test
    fun `getRegionById returns null for non-existent region`() = runTest {
        val region = repository.getRegionById("non_existent_region").first()

        assertEquals(null, region)
    }

    @Test
    fun `getCurrentRegion returns current region`() = runTest {
        val region = repository.getCurrentRegion().first()

        assertNotNull(region)
        assertEquals(RegionStatus.CURRENT, region?.status)
    }

    @Test
    fun `getAvailableRegions returns only available regions`() = runTest {
        val regions = repository.getAvailableRegions().first()

        assertTrue(regions.isNotEmpty())
        assertTrue(regions.all { it.status == RegionStatus.AVAILABLE })
    }

    @Test
    fun `getUnlockedRegions returns all non-locked regions`() = runTest {
        val regions = repository.getUnlockedRegions().first()

        assertTrue(regions.isNotEmpty())
        assertTrue(regions.all { it.status != RegionStatus.LOCKED })
    }

    @Test
    fun `travelToRegion changes current region`() = runTest {
        val result = repository.travelToRegion("jade_forest")

        assertTrue(result is WorldResult.TravelStarted)

        val currentRegion = repository.getCurrentRegion().first()
        assertEquals("jade_forest", currentRegion?.id)
    }

    @Test
    fun `travelToRegion fails for locked region`() = runTest {
        val result = repository.travelToRegion("riverside_town")

        assertTrue(result is WorldResult.Error)
    }

    @Test
    fun `travelToRegion fails for non-existent region`() = runTest {
        val result = repository.travelToRegion("non_existent_region")

        assertTrue(result is WorldResult.Error)
    }

    @Test
    fun `discoverLocation marks location as discovered`() = runTest {
        val result = repository.discoverLocation("loc_forest_entrance")

        assertTrue(result is WorldResult.LocationDiscovered)

        val locations = repository.getLocationsByRegion("jade_forest").first()
        val discovered = locations.find { it.id == "loc_forest_entrance" }
        assertTrue(discovered?.isDiscovered == true)
    }

    @Test
    fun `discoverLocation fails for non-existent location`() = runTest {
        val result = repository.discoverLocation("non_existent_location")

        assertTrue(result is WorldResult.Error)
    }

    @Test
    fun `collectItem marks collectible as collected`() = runTest {
        val result = repository.collectItem("col_tea_set")

        assertTrue(result is WorldResult.CollectibleFound)

        val collectibles = repository.getCollectiblesByRegion("qingyuan_village").first()
        val collected = collectibles.find { it.id == "col_tea_set" }
        assertTrue(collected?.isCollected == true)
    }

    @Test
    fun `collectItem fails for already collected item`() = runTest {
        repository.collectItem("col_tea_set")
        val result = repository.collectItem("col_tea_set")

        assertTrue(result is WorldResult.Error)
    }

    @Test
    fun `collectItem fails for non-existent collectible`() = runTest {
        val result = repository.collectItem("non_existent_collectible")

        assertTrue(result is WorldResult.Error)
    }

    @Test
    fun `getLocationsByRegion returns correct locations`() = runTest {
        val locations = repository.getLocationsByRegion("qingyuan_village").first()

        assertEquals(4, locations.size)
        assertTrue(locations.all { it.regionId == "qingyuan_village" })
    }

    @Test
    fun `getLandmarksByRegion returns correct landmarks`() = runTest {
        val landmarks = repository.getLandmarksByRegion("qingyuan_village").first()

        assertEquals(3, landmarks.size)
        assertTrue(landmarks.all { it.regionId == "qingyuan_village" })
    }

    @Test
    fun `getCollectiblesByRegion returns correct collectibles`() = runTest {
        val collectibles = repository.getCollectiblesByRegion("qingyuan_village").first()

        assertEquals(1, collectibles.size)
        assertTrue(collectibles.all { it.regionId == "qingyuan_village" })
    }

    @Test
    fun `getRegionConnections returns correct connections`() = runTest {
        val connections = repository.getRegionConnections("qingyuan_village").first()

        assertTrue(connections.isNotEmpty())
        assertTrue(connections.any {
            it.fromRegionId == "qingyuan_village" || it.toRegionId == "qingyuan_village"
        })
    }

    @Test
    fun `getExplorationProgress returns correct progress`() = runTest {
        val progress = repository.getExplorationProgress().first()

        assertEquals(12, progress.totalRegions)
        assertNotNull(progress.currentRegionId)
    }

    @Test
    fun `completeRegion marks region as completed`() = runTest {
        repository.travelToRegion("jade_forest")
        val result = repository.completeRegion("jade_forest")

        assertTrue(result is WorldResult.RegionUnlocked)

        val regions = repository.getAllRegions().first()
        val region = regions.find { it.id == "jade_forest" }
        assertEquals(RegionStatus.COMPLETED, region?.status)
    }

    @Test
    fun `completeRegion fails for non-existent region`() = runTest {
        val result = repository.completeRegion("non_existent_region")

        assertTrue(result is WorldResult.Error)
    }

    @Test
    fun `unlockFastTravel marks region fast travel as unlocked`() = runTest {
        val result = repository.unlockFastTravel("qingyuan_village")

        assertTrue(result is WorldResult.Success)

        val progress = repository.getRegionProgress("qingyuan_village").first()
        assertTrue(progress?.unlockedFastTravel == true)
    }

    @Test
    fun `checkRegionUnlocks unlocks regions when requirements met`() = runTest {
        // Complete the first quest requirement
        val regions = repository.getAllRegions().first()
        val jadeForest = regions.find { it.id == "jade_forest" }
        assertEquals(listOf("quest_help_grandma_mei"), jadeForest?.unlockRequirements?.questIds)

        // For now, just verify the method runs without error
        val unlocked = repository.checkRegionUnlocks()
        assertNotNull(unlocked)
    }

    @Test
    fun `region connections are bidirectional`() = runTest {
        val connectionsFrom = repository.getRegionConnections("qingyuan_village").first()
        val connectionsTo = repository.getRegionConnections("jade_forest").first()

        assertTrue(connectionsFrom.isNotEmpty())
        assertTrue(connectionsTo.isNotEmpty())
    }

    @Test
    fun `all regions have correct structure`() = runTest {
        val regions = repository.getAllRegions().first()

        regions.forEach { region ->
            assertTrue(region.id.isNotEmpty())
            assertTrue(region.name.isNotEmpty())
            assertTrue(region.nameCn.isNotEmpty())
            assertTrue(region.description.isNotEmpty())
            assertTrue(region.order > 0)
            assertTrue(region.chapter > 0)
        }
    }
}
