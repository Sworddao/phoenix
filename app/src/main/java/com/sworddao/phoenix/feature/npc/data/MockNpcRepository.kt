package com.sworddao.phoenix.feature.npc.data

import com.sworddao.phoenix.data.seed.NpcSeedData

import com.sworddao.phoenix.feature.npc.domain.NpcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockNpcRepository @Inject constructor() : NpcRepository {

    private val npcs = MutableStateFlow(loadMockNpcs())

    override fun getAllNpcs(): Flow<List<Npc>> = npcs

    override fun getNpcById(id: String): Flow<Npc?> {
        return npcs.map { list -> list.firstOrNull { it.id == id } }
    }

    override fun getNpcsByLocation(locationName: String): Flow<List<Npc>> {
        return npcs.map { list -> list.filter { it.currentLocation == locationName } }
    }

    override suspend fun updateFriendship(npcId: String, xpGain: Int) {
        npcs.update { currentNpcs ->
            currentNpcs.map { npc ->
                if (npc.id == npcId) {
                    npc.copy(friendshipXp = npc.friendshipXp + xpGain)
                } else {
                    npc
                }
            }
        }
    }

    
    private fun loadMockNpcs(): List<Npc> =
        NpcSeedData.loadMockNpcs()
}
