package com.sworddao.phoenix.feature.dialogue.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DialogueNodeType {
    @SerialName("npc_speaks")
    NPC_SPEAKS,
    @SerialName("player_chooses")
    PLAYER_CHOOSES,
    @SerialName("conversation_end")
    CONVERSATION_END
}

@Serializable
enum class Speaker {
    @SerialName("npc")
    NPC,
    @SerialName("player")
    PLAYER,
    @SerialName("narrator")
    NARRATOR
}

@Serializable
data class DialogueCondition(
    val type: ConditionType,
    val targetId: String = "",
    val value: String = ""
)

@Serializable
enum class ConditionType {
    @SerialName("friendship_level")
    FRIENDSHIP_LEVEL,
    @SerialName("has_item")
    HAS_ITEM,
    @SerialName("quest_completed")
    QUEST_COMPLETED,
    @SerialName("always")
    ALWAYS
}

@Serializable
data class DialogueAction(
    val type: ActionType,
    val targetId: String = "",
    val value: String = ""
)

@Serializable
enum class ActionType {
    @SerialName("add_friendship_xp")
    ADD_FRIENDSHIP_XP,
    @SerialName("unlock_vocabulary")
    UNLOCK_VOCABULARY,
    @SerialName("complete_quest")
    COMPLETE_QUEST,
    @SerialName("practice_speaking")
    PRACTICE_SPEAKING,
    @SerialName("practice_listening")
    PRACTICE_LISTENING,
    @SerialName("give_item")
    GIVE_ITEM
}

@Serializable
data class DialogueChoice(
    val id: String,
    val text: String,
    val pinyin: String = "",
    val nextNodeId: String,
    val conditions: List<DialogueCondition> = emptyList(),
    val actions: List<DialogueAction> = emptyList()
)

@Serializable
data class DialogueNode(
    val id: String,
    val type: DialogueNodeType,
    val speaker: Speaker = Speaker.NPC,
    val speakerName: String = "",
    val text: String,
    val pinyin: String = "",
    val hanzi: String = "",
    val choices: List<DialogueChoice> = emptyList(),
    val nextNodeId: String? = null,
    val conditions: List<DialogueCondition> = emptyList(),
    val actions: List<DialogueAction> = emptyList()
)

@Serializable
data class Dialogue(
    val id: String,
    val npcId: String,
    val title: String,
    val description: String,
    val startNodeId: String,
    val nodes: List<DialogueNode>,
    val requiredFriendshipLevel: Int = 1
) {
    fun getNodeById(nodeId: String): DialogueNode? {
        return nodes.firstOrNull { it.id == nodeId }
    }
}

enum class ConversationPhase {
    IN_PROGRESS,
    COMPLETED,
    ERROR
}

data class ConversationState(
    val dialogueId: String,
    val npcId: String,
    val currentNodeId: String,
    val phase: ConversationPhase,
    val history: List<DialogueHistoryEntry>,
    val availableChoices: List<DialogueChoice>,
    val completedActions: List<DialogueAction>
)

data class DialogueHistoryEntry(
    val speaker: Speaker,
    val speakerName: String,
    val text: String,
    val pinyin: String = "",
    val hanzi: String = ""
)

sealed class DialogueResult {
    data class NodeLoaded(
        val node: DialogueNode,
        val history: List<DialogueHistoryEntry>,
        val choices: List<DialogueChoice>
    ) : DialogueResult()

    data class ConversationEnded(
        val actions: List<DialogueAction>,
        val history: List<DialogueHistoryEntry>
    ) : DialogueResult()

    data class Error(val message: String) : DialogueResult()
}
