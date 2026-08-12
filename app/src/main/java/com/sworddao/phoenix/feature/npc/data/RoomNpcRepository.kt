package com.sworddao.phoenix.feature.npc.data

import com.sworddao.phoenix.data.seed.NpcSeedData
import com.sworddao.phoenix.feature.npc.domain.NpcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomNpcRepository @Inject constructor(
    private val dao: NpcDao
) : NpcRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countNpcs() == 0) {
                dao.upsertAll(NpcSeedData.loadMockNpcs().map { it.toEntity() })
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getAllNpcs(): Flow<List<Npc>> =
        seededFlow { dao.getAllNpcs().map { list -> list.map { it.toDomain() } } }

    override fun getNpcById(id: String): Flow<Npc?> =
        seededFlow { dao.getNpcById(id).map { it?.toDomain() } }

    override fun getNpcsByLocation(locationName: String): Flow<List<Npc>> =
        seededFlow { dao.getNpcsByLocation(locationName).map { list -> list.map { it.toDomain() } } }

    override suspend fun updateFriendship(npcId: String, xpGain: Int) {
        ensureSeeded()
        dao.updateFriendshipXp(npcId, xpGain)
    }
}
