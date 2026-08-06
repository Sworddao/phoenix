package com.sworddao.phoenix.feature.dialogue.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.dialogue.data.ConversationPhase
import com.sworddao.phoenix.feature.dialogue.data.ConversationState
import com.sworddao.phoenix.feature.dialogue.data.Dialogue
import com.sworddao.phoenix.feature.dialogue.data.DialogueAction
import com.sworddao.phoenix.feature.dialogue.data.DialogueChoice
import com.sworddao.phoenix.feature.dialogue.data.DialogueHistoryEntry
import com.sworddao.phoenix.feature.dialogue.data.DialogueNode
import com.sworddao.phoenix.feature.dialogue.data.DialogueResult
import com.sworddao.phoenix.feature.dialogue.domain.DialogueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DialogueUiState(
    val dialogue: Dialogue? = null,
    val currentNode: DialogueNode? = null,
    val currentSpeaker: String = "",
    val history: List<DialogueHistoryEntry> = emptyList(),
    val availableChoices: List<DialogueChoice> = emptyList(),
    val isConversationComplete: Boolean = false,
    val completedActions: List<DialogueAction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DialogueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dialogueRepository: DialogueRepository
) : ViewModel() {

    private val dialogueId: String = savedStateHandle["dialogueId"] ?: ""

    private val _uiState = MutableStateFlow(DialogueUiState())
    val uiState: StateFlow<DialogueUiState> = _uiState.asStateFlow()

    init {
        if (dialogueId.isNotEmpty()) {
            loadDialogue()
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "No dialogue ID provided"
            )
        }
    }

    private fun loadDialogue() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            dialogueRepository.getDialogueByNpcId(dialogueId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { dialogue ->
                    if (dialogue != null) {
                        _uiState.value = _uiState.value.copy(
                            dialogue = dialogue,
                            isLoading = false
                        )
                        startConversation(dialogue.id)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Dialogue not found"
                        )
                    }
                }
        }
    }

    private fun startConversation(dialogueId: String) {
        viewModelScope.launch {
            when (val result = dialogueRepository.startConversation(dialogueId)) {
                is DialogueResult.NodeLoaded -> {
                    _uiState.value = _uiState.value.copy(
                        currentNode = result.node,
                        currentSpeaker = result.node.speakerName,
                        history = result.history,
                        availableChoices = result.choices,
                        error = null
                    )
                }
                is DialogueResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun selectChoice(choiceId: String) {
        val currentDialogueId = _uiState.value.dialogue?.id ?: return

        viewModelScope.launch {
            when (val result = dialogueRepository.selectChoice(currentDialogueId, choiceId)) {
                is DialogueResult.NodeLoaded -> {
                    _uiState.value = _uiState.value.copy(
                        currentNode = result.node,
                        currentSpeaker = result.node.speakerName,
                        history = result.history,
                        availableChoices = result.choices,
                        error = null
                    )
                }
                is DialogueResult.ConversationEnded -> {
                    _uiState.value = _uiState.value.copy(
                        isConversationComplete = true,
                        history = result.history,
                        completedActions = result.actions,
                        availableChoices = emptyList(),
                        error = null
                    )
                }
                is DialogueResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                }
            }
        }
    }

    fun advanceDialogue() {
        val currentDialogueId = _uiState.value.dialogue?.id ?: return

        viewModelScope.launch {
            when (val result = dialogueRepository.advanceDialogue(currentDialogueId)) {
                is DialogueResult.NodeLoaded -> {
                    _uiState.value = _uiState.value.copy(
                        currentNode = result.node,
                        currentSpeaker = result.node.speakerName,
                        history = result.history,
                        availableChoices = result.choices,
                        error = null
                    )
                }
                is DialogueResult.ConversationEnded -> {
                    _uiState.value = _uiState.value.copy(
                        isConversationComplete = true,
                        history = result.history,
                        completedActions = result.actions,
                        availableChoices = emptyList(),
                        error = null
                    )
                }
                is DialogueResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
