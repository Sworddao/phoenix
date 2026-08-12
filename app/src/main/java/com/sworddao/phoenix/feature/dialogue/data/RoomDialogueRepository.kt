package com.sworddao.phoenix.feature.dialogue.data

import com.sworddao.phoenix.data.seed.DialogueSeedData
import com.sworddao.phoenix.feature.dialogue.domain.DialogueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDialogueRepository @Inject constructor(
    private val dao: DialogueDao
) : DialogueRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()
    private val conversationStates = mutableMapOf<String, ConversationState>()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countDialogues() == 0) {
                dao.upsertAll(DialogueSeedData.loadDialogues().map { it.toEntity() })
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getDialogueByNpcId(npcId: String): Flow<Dialogue?> =
        seededFlow { dao.getDialogueByNpcId(npcId).map { it?.toDomain() } }

    override fun getAllDialogues(): Flow<List<Dialogue>> =
        seededFlow { dao.getAllDialogues().map { list -> list.map { it.toDomain() } } }

    override suspend fun startConversation(dialogueId: String): DialogueResult {
        ensureSeeded()
        val dialogue = dao.getDialogueById(dialogueId).first()?.toDomain()
            ?: return DialogueResult.Error("Dialogue not found")

        val (state, result) = DialogueFlow.startConversation(dialogue)
        if (state != null) conversationStates[dialogueId] = state
        return result
    }

    override suspend fun selectChoice(dialogueId: String, choiceId: String): DialogueResult {
        ensureSeeded()
        val state = conversationStates[dialogueId]
            ?: return DialogueResult.Error("No active conversation")
        val dialogue = dao.getDialogueById(dialogueId).first()?.toDomain()
            ?: return DialogueResult.Error("Dialogue not found")

        val (newState, result) = DialogueFlow.selectChoice(dialogue, state, choiceId)
        if (newState != null) conversationStates[dialogueId] = newState
        return result
    }

    override suspend fun advanceDialogue(dialogueId: String): DialogueResult {
        ensureSeeded()
        val state = conversationStates[dialogueId]
            ?: return DialogueResult.Error("No active conversation")
        val dialogue = dao.getDialogueById(dialogueId).first()?.toDomain()
            ?: return DialogueResult.Error("Dialogue not found")

        val (newState, result) = DialogueFlow.advanceDialogue(dialogue, state)
        if (newState != null) conversationStates[dialogueId] = newState
        return result
    }
}
