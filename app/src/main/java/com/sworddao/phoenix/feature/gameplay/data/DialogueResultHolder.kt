package com.sworddao.phoenix.feature.gameplay.data

import com.sworddao.phoenix.feature.dialogue.viewmodel.ProcessedAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogueResultHolder @Inject constructor() {

    private var _lastProcessedActions: List<ProcessedAction> = emptyList()
    private var _lastNpcId: String = ""
    private var _lastDialogueId: String = ""
    private var _lastXpEarned: Int = 0

    val lastProcessedActions: List<ProcessedAction> get() = _lastProcessedActions
    val lastNpcId: String get() = _lastNpcId
    val lastDialogueId: String get() = _lastDialogueId
    val lastXpEarned: Int get() = _lastXpEarned

    fun storeResults(
        dialogueId: String,
        npcId: String,
        processedActions: List<ProcessedAction>
    ) {
        _lastDialogueId = dialogueId
        _lastNpcId = npcId
        _lastProcessedActions = processedActions
        _lastXpEarned = processedActions
            .filter { it.type == com.sworddao.phoenix.feature.dialogue.data.ActionType.ADD_FRIENDSHIP_XP && it.success }
            .sumOf { it.value.toIntOrNull() ?: 0 }
    }

    fun clear() {
        _lastProcessedActions = emptyList()
        _lastNpcId = ""
        _lastDialogueId = ""
        _lastXpEarned = 0
    }
}
