package com.sworddao.phoenix.feature.dialogue.data

/**
 * Pure conversation traversal shared by [MockDialogueRepository] and
 * [RoomDialogueRepository]. Both implementations persist dialogue content
 * differently but run the same state machine, so a bug fix here applies to
 * both. Active conversation state is transient and intentionally NOT persisted.
 */
internal object DialogueFlow {

    fun startConversation(dialogue: Dialogue): Pair<ConversationState?, DialogueResult> {
        var currentNode = dialogue.getNodeById(dialogue.startNodeId)
            ?: return null to DialogueResult.Error("Start node not found")

        val history = mutableListOf<DialogueHistoryEntry>()
        val allActions = mutableListOf<DialogueAction>()

        while (currentNode.type == DialogueNodeType.NPC_SPEAKS && currentNode.nextNodeId != null) {
            history.add(currentNode.toHistoryEntry())
            allActions.addAll(currentNode.actions)
            currentNode = dialogue.getNodeById(currentNode.nextNodeId!!)
                ?: break
        }

        if (history.isEmpty()) {
            history.add(currentNode.toHistoryEntry())
            allActions.addAll(currentNode.actions)
        }

        val state = ConversationState(
            dialogueId = dialogue.id,
            npcId = dialogue.npcId,
            currentNodeId = currentNode.id,
            phase = ConversationPhase.IN_PROGRESS,
            history = history,
            availableChoices = currentNode.choices,
            completedActions = allActions
        )

        return state to DialogueResult.NodeLoaded(
            node = currentNode,
            history = history,
            choices = currentNode.choices
        )
    }

    fun selectChoice(
        dialogue: Dialogue,
        currentState: ConversationState?,
        choiceId: String
    ): Pair<ConversationState?, DialogueResult> {
        val state = currentState
            ?: return null to DialogueResult.Error("No active conversation")

        val currentNode = dialogue.getNodeById(state.currentNodeId)
            ?: return state to DialogueResult.Error("Current node not found")

        val choice = currentNode.choices.firstOrNull { it.id == choiceId }
            ?: return state to DialogueResult.Error("Choice not found")

        val nextNode = dialogue.getNodeById(choice.nextNodeId)
            ?: return state to DialogueResult.Error("Next node not found")

        val newHistory = state.history + DialogueHistoryEntry(
            speaker = Speaker.PLAYER,
            speakerName = "Player",
            text = choice.text,
            pinyin = choice.pinyin
        ) + nextNode.toHistoryEntry()

        val allActions = state.completedActions + choice.actions + nextNode.actions

        return updateState(state, nextNode, newHistory, allActions)
    }

    fun advanceDialogue(
        dialogue: Dialogue,
        currentState: ConversationState?
    ): Pair<ConversationState?, DialogueResult> {
        val state = currentState
            ?: return null to DialogueResult.Error("No active conversation")

        val currentNode = dialogue.getNodeById(state.currentNodeId)
            ?: return state to DialogueResult.Error("Current node not found")

        val nextNodeId = currentNode.nextNodeId
            ?: return state to DialogueResult.Error("No next node defined")

        val nextNode = dialogue.getNodeById(nextNodeId)
            ?: return state to DialogueResult.Error("Next node not found")

        val newHistory = state.history + nextNode.toHistoryEntry()

        val allActions = state.completedActions + nextNode.actions

        return updateState(state, nextNode, newHistory, allActions)
    }

    private fun updateState(
        state: ConversationState,
        nextNode: DialogueNode,
        newHistory: List<DialogueHistoryEntry>,
        allActions: List<DialogueAction>
    ): Pair<ConversationState?, DialogueResult> {
        if (nextNode.type == DialogueNodeType.CONVERSATION_END) {
            val completedState = state.copy(
                currentNodeId = nextNode.id,
                phase = ConversationPhase.COMPLETED,
                history = newHistory,
                availableChoices = emptyList(),
                completedActions = allActions
            )
            return completedState to DialogueResult.ConversationEnded(
                actions = allActions,
                history = newHistory
            )
        }

        val newState = state.copy(
            currentNodeId = nextNode.id,
            phase = ConversationPhase.IN_PROGRESS,
            history = newHistory,
            availableChoices = nextNode.choices,
            completedActions = allActions
        )
        return newState to DialogueResult.NodeLoaded(
            node = nextNode,
            history = newHistory,
            choices = nextNode.choices
        )
    }

    private fun DialogueNode.toHistoryEntry(): DialogueHistoryEntry = DialogueHistoryEntry(
        speaker = speaker,
        speakerName = speakerName,
        text = text,
        pinyin = pinyin,
        hanzi = hanzi
    )
}
