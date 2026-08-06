package com.sworddao.phoenix.feature.dialogue.domain

import com.sworddao.phoenix.feature.dialogue.data.Dialogue
import com.sworddao.phoenix.feature.dialogue.data.DialogueResult
import kotlinx.coroutines.flow.Flow

interface DialogueRepository {
    fun getDialogueByNpcId(npcId: String): Flow<Dialogue?>
    fun getAllDialogues(): Flow<List<Dialogue>>
    suspend fun startConversation(dialogueId: String): DialogueResult
    suspend fun selectChoice(dialogueId: String, choiceId: String): DialogueResult
    suspend fun advanceDialogue(dialogueId: String): DialogueResult
}
