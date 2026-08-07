package com.sworddao.phoenix.feature.dialogue.data

import com.sworddao.phoenix.data.seed.DialogueSeedData

import com.sworddao.phoenix.feature.dialogue.domain.DialogueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDialogueRepository @Inject constructor() : DialogueRepository {

    private val dialogues = MutableStateFlow(loadDialogues())
    private val conversationStates = mutableMapOf<String, ConversationState>()

    override fun getDialogueByNpcId(npcId: String): Flow<Dialogue?> {
        return dialogues.map { list -> list.firstOrNull { it.npcId == npcId } }
    }

    override fun getAllDialogues(): Flow<List<Dialogue>> = dialogues

    override suspend fun startConversation(dialogueId: String): DialogueResult {
        val dialogue = dialogues.value.firstOrNull { it.id == dialogueId }
            ?: return DialogueResult.Error("Dialogue not found")

        var currentNode = dialogue.getNodeById(dialogue.startNodeId)
            ?: return DialogueResult.Error("Start node not found")

        val history = mutableListOf<DialogueHistoryEntry>()
        val allActions = mutableListOf<DialogueAction>()

        while (currentNode.type == DialogueNodeType.NPC_SPEAKS && currentNode.nextNodeId != null) {
            history.add(
                DialogueHistoryEntry(
                    speaker = currentNode.speaker,
                    speakerName = currentNode.speakerName,
                    text = currentNode.text,
                    pinyin = currentNode.pinyin,
                    hanzi = currentNode.hanzi
                )
            )
            allActions.addAll(currentNode.actions)
            currentNode = dialogue.getNodeById(currentNode.nextNodeId!!)
                ?: break
        }

        if (history.isEmpty()) {
            history.add(
                DialogueHistoryEntry(
                    speaker = currentNode.speaker,
                    speakerName = currentNode.speakerName,
                    text = currentNode.text,
                    pinyin = currentNode.pinyin,
                    hanzi = currentNode.hanzi
                )
            )
            allActions.addAll(currentNode.actions)
        }

        val state = ConversationState(
            dialogueId = dialogueId,
            npcId = dialogue.npcId,
            currentNodeId = currentNode.id,
            phase = ConversationPhase.IN_PROGRESS,
            history = history,
            availableChoices = currentNode.choices,
            completedActions = allActions
        )
        conversationStates[dialogueId] = state

        return DialogueResult.NodeLoaded(
            node = currentNode,
            history = history,
            choices = currentNode.choices
        )
    }

    override suspend fun selectChoice(dialogueId: String, choiceId: String): DialogueResult {
        val state = conversationStates[dialogueId]
            ?: return DialogueResult.Error("No active conversation")

        val dialogue = dialogues.value.firstOrNull { it.id == dialogueId }
            ?: return DialogueResult.Error("Dialogue not found")

        val currentNode = dialogue.getNodeById(state.currentNodeId)
            ?: return DialogueResult.Error("Current node not found")

        val choice = currentNode.choices.firstOrNull { it.id == choiceId }
            ?: return DialogueResult.Error("Choice not found")

        val nextNode = dialogue.getNodeById(choice.nextNodeId)
            ?: return DialogueResult.Error("Next node not found")

        val newHistory = state.history + DialogueHistoryEntry(
            speaker = Speaker.PLAYER,
            speakerName = "Player",
            text = choice.text,
            pinyin = choice.pinyin
        ) + DialogueHistoryEntry(
            speaker = nextNode.speaker,
            speakerName = nextNode.speakerName,
            text = nextNode.text,
            pinyin = nextNode.pinyin,
            hanzi = nextNode.hanzi
        )

        val allActions = state.completedActions + choice.actions + nextNode.actions

        if (nextNode.type == DialogueNodeType.CONVERSATION_END) {
            val completedState = state.copy(
                currentNodeId = nextNode.id,
                phase = ConversationPhase.COMPLETED,
                history = newHistory,
                availableChoices = emptyList(),
                completedActions = allActions
            )
            conversationStates[dialogueId] = completedState

            return DialogueResult.ConversationEnded(
                actions = allActions,
                history = newHistory
            )
        }

        val newState = state.copy(
            currentNodeId = nextNode.id,
            history = newHistory,
            availableChoices = nextNode.choices,
            completedActions = allActions
        )
        conversationStates[dialogueId] = newState

        return DialogueResult.NodeLoaded(
            node = nextNode,
            history = newHistory,
            choices = nextNode.choices
        )
    }

    override suspend fun advanceDialogue(dialogueId: String): DialogueResult {
        val state = conversationStates[dialogueId]
            ?: return DialogueResult.Error("No active conversation")

        val dialogue = dialogues.value.firstOrNull { it.id == dialogueId }
            ?: return DialogueResult.Error("Dialogue not found")

        val currentNode = dialogue.getNodeById(state.currentNodeId)
            ?: return DialogueResult.Error("Current node not found")

        val nextNodeId = currentNode.nextNodeId
            ?: return DialogueResult.Error("No next node defined")

        val nextNode = dialogue.getNodeById(nextNodeId)
            ?: return DialogueResult.Error("Next node not found")

        val newHistory = state.history + DialogueHistoryEntry(
            speaker = nextNode.speaker,
            speakerName = nextNode.speakerName,
            text = nextNode.text,
            pinyin = nextNode.pinyin,
            hanzi = nextNode.hanzi
        )

        val allActions = state.completedActions + nextNode.actions

        if (nextNode.type == DialogueNodeType.CONVERSATION_END) {
            val completedState = state.copy(
                currentNodeId = nextNode.id,
                phase = ConversationPhase.COMPLETED,
                history = newHistory,
                availableChoices = emptyList(),
                completedActions = allActions
            )
            conversationStates[dialogueId] = completedState

            return DialogueResult.ConversationEnded(
                actions = allActions,
                history = newHistory
            )
        }

        val newState = state.copy(
            currentNodeId = nextNode.id,
            history = newHistory,
            availableChoices = nextNode.choices,
            completedActions = allActions
        )
        conversationStates[dialogueId] = newState

        return DialogueResult.NodeLoaded(
            node = nextNode,
            history = newHistory,
            choices = nextNode.choices
        )
    }

        private fun loadDialogues(): List<Dialogue> =
        DialogueSeedData.loadDialogues()

    
}
