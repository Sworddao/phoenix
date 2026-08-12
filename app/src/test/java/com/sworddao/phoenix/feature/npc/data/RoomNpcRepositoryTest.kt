package com.sworddao.phoenix.feature.npc.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.data.seed.NpcSeedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomNpcRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomNpcRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = createRepository()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createRepository(): RoomNpcRepository = RoomNpcRepository(database.npcDao())

    @Test
    fun `first access seeds npcs from seed data`() = runBlocking {
        val npcs = repository.getAllNpcs().first()

        assertEquals(NpcSeedData.loadMockNpcs().size, npcs.size)
        assertTrue(npcs.any { it.id == "grandma_mei" })
        assertTrue(npcs.any { it.id == "restaurant_owner_lin" })
        assertTrue(npcs.any { it.id == "taxi_driver_chen" })
        assertTrue(npcs.any { it.id == "university_student_wei" })
        assertTrue(npcs.all { it.schedule.entries.isNotEmpty() })
    }

    @Test
    fun `getNpcById returns the matching npc`() = runBlocking {
        val npc = repository.getNpcById("grandma_mei").first()

        assertNotNull(npc)
        assertEquals("Grandma Mei", npc?.displayName)
        assertEquals("Retired Baker", npc?.occupation)
        assertEquals("Grandma Mei's Bakery", npc?.currentLocation)
        assertEquals(listOf("Greetings", "Family", "Food", "Daily Conversation"), npc?.vocabularyCategories)
        assertEquals(listOf("bakery_intro", "bakery_food", "bakery_family"), npc?.dialogueReferences)
    }

    @Test
    fun `getNpcById returns null for unknown npc`() = runBlocking {
        val npc = repository.getNpcById("unknown_npc").first()

        assertNull(npc)
    }

    @Test
    fun `getNpcsByLocation filters by current location`() = runBlocking {
        val restaurantNpcs = repository.getNpcsByLocation("Restaurant").first()
        val squareNpcs = repository.getNpcsByLocation("Village Square").first()
        val teaHouseNpcs = repository.getNpcsByLocation("Tea House").first()

        assertEquals(listOf("restaurant_owner_lin"), restaurantNpcs.map { it.id })
        assertEquals(listOf("taxi_driver_chen"), squareNpcs.map { it.id })
        assertEquals(listOf("university_student_wei"), teaHouseNpcs.map { it.id })
    }

    @Test
    fun `updateFriendship accumulates persisted friendship xp`() = runBlocking {
        repository.updateFriendship("grandma_mei", 25)
        repository.updateFriendship("grandma_mei", 15)

        val npc = repository.getNpcById("grandma_mei").first()
        assertEquals(40, npc?.friendshipXp)
    }

    @Test
    fun `updateFriendship for unknown npc is a no-op`() = runBlocking {
        repository.updateFriendship("unknown_npc", 100)

        val npcs = repository.getAllNpcs().first()
        assertTrue(npcs.all { it.friendshipXp == 0 })
    }

    @Test
    fun `friendship xp persists across repository instances`() = runBlocking {
        repository.updateFriendship("restaurant_owner_lin", 30)

        val freshRepository = createRepository()
        val npc = freshRepository.getNpcById("restaurant_owner_lin").first()
        assertEquals(30, npc?.friendshipXp)
    }

    @Test
    fun `schedule and list fields survive entity round trip`() = runBlocking {
        val original = NpcSeedData.loadMockNpcs().first()

        val entity = original.toEntity()
        val restored = entity.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.schedule.entries, restored.schedule.entries)
        assertEquals(original.vocabularyCategories, restored.vocabularyCategories)
        assertEquals(original.dialogueReferences, restored.dialogueReferences)
        assertEquals(original.avatarEmoji, restored.avatarEmoji)
    }

    @Test
    fun `npc schedule json round trips through RoomJson`() {
        val npc = NpcSeedData.loadMockNpcs().first()
        val json = RoomJson.toJsonList(npc.schedule.entries)
        val entries: List<NpcScheduleEntry> = RoomJson.fromJsonList(json)

        assertEquals(npc.schedule.entries, entries)
        assertEquals(4, entries.size)
    }

    @Test
    fun `dao round trip persists a custom npc`() = runBlocking {
        database.npcDao().upsert(
            NpcEntity(
                id = "custom_npc",
                displayName = "Custom",
                occupation = "Guide",
                personality = "Helpful",
                currentLocation = "Jade Forest",
                avatarEmoji = "🙂",
                idleAnimationState = "IDLE",
                interactionAvailability = "AVAILABLE",
            )
        )

        val npc = repository.getNpcById("custom_npc").first()
        assertNotNull(npc)
        assertEquals("Custom", npc?.displayName)
        assertEquals(IdleAnimationState.IDLE, npc?.idleAnimationState)
        assertTrue(npc?.schedule?.entries?.isEmpty() == true)
    }
}
