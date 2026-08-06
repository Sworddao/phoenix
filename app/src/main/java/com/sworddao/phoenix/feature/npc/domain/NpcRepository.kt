package com.sworddao.phoenix.feature.npc.domain

import com.sworddao.phoenix.feature.npc.data.Npc
import kotlinx.coroutines.flow.Flow

interface NpcRepository {
    fun getAllNpcs(): Flow<List<Npc>>
    fun getNpcById(id: String): Flow<Npc?>
    fun getNpcsByLocation(locationName: String): Flow<List<Npc>>
    suspend fun updateFriendship(npcId: String, xpGain: Int)
}
