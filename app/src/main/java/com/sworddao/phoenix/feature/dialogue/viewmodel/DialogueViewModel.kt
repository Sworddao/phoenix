package com.sworddao.phoenix.feature.dialogue.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.dialogue.data.ActionType
import com.sworddao.phoenix.feature.dialogue.data.ConversationPhase
import com.sworddao.phoenix.feature.dialogue.data.ConversationState
import com.sworddao.phoenix.feature.dialogue.data.Dialogue
import com.sworddao.phoenix.feature.dialogue.data.DialogueAction
import com.sworddao.phoenix.feature.dialogue.data.DialogueChoice
import com.sworddao.phoenix.feature.dialogue.data.DialogueHistoryEntry
import com.sworddao.phoenix.feature.dialogue.data.DialogueNode
import com.sworddao.phoenix.feature.dialogue.data.DialogueResult
import com.sworddao.phoenix.feature.dialogue.domain.DialogueRepository
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.DialogueResultHolder
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import com.sworddao.phoenix.feature.writing.domain.WritingRepository
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessedAction(
    val type: ActionType,
    val targetId: String,
    val value: String,
    val success: Boolean
)

data class DialogueUiState(
    val dialogue: Dialogue? = null,
    val currentNode: DialogueNode? = null,
    val currentSpeaker: String = "",
    val history: List<DialogueHistoryEntry> = emptyList(),
    val availableChoices: List<DialogueChoice> = emptyList(),
    val isConversationComplete: Boolean = false,
    val completedActions: List<DialogueAction> = emptyList(),
    val processedActions: List<ProcessedAction> = emptyList(),
    val isPracticeAvailable: Boolean = false,
    val isListeningPracticeAvailable: Boolean = false,
    val isReadingPracticeAvailable: Boolean = false,
    val isWritingPracticeAvailable: Boolean = false,
    val isLoading: Boolean = true,
    val isProcessingActions: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DialogueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dialogueRepository: DialogueRepository,
    private val friendshipRepository: FriendshipRepository,
    private val questRepository: QuestRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val pronunciationRepository: PronunciationRepository,
    private val listeningRepository: ListeningRepository,
    private val readingRepository: ReadingRepository,
    private val writingRepository: WritingRepository,
    private val dialogueResultHolder: DialogueResultHolder
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
                    onConversationEnded(result.actions, result.history)
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
                    onConversationEnded(result.actions, result.history)
                }
                is DialogueResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                }
            }
        }
    }

    private suspend fun onConversationEnded(
        actions: List<DialogueAction>,
        history: List<DialogueHistoryEntry>
    ) {
        _uiState.value = _uiState.value.copy(
            isConversationComplete = true,
            history = history,
            completedActions = actions,
            availableChoices = emptyList(),
            isProcessingActions = actions.isNotEmpty(),
            error = null
        )

        if (actions.isNotEmpty()) {
            val processed = processActions(actions)
            _uiState.value = _uiState.value.copy(
                processedActions = processed,
                isProcessingActions = false
            )
            dialogueResultHolder.storeResults(
                dialogueId = dialogueId,
                npcId = _uiState.value.dialogue?.npcId ?: "",
                processedActions = processed
            )
        }

        val hasPracticeAction = actions.any {
            it.type == ActionType.PRACTICE_SPEAKING
        }
        val hasListeningPracticeAction = actions.any {
            it.type == ActionType.PRACTICE_LISTENING
        }
        val hasReadingPracticeAction = actions.any {
            it.type == ActionType.PRACTICE_READING
        }
        val hasWritingPracticeAction = actions.any {
            it.type == ActionType.PRACTICE_WRITING
        }
        _uiState.value = _uiState.value.copy(
            isPracticeAvailable = hasPracticeAction,
            isListeningPracticeAvailable = hasListeningPracticeAction,
            isReadingPracticeAvailable = hasReadingPracticeAction,
            isWritingPracticeAvailable = hasWritingPracticeAction
        )
    }

    private suspend fun processActions(actions: List<DialogueAction>): List<ProcessedAction> {
        val results = mutableListOf<ProcessedAction>()

        for (action in actions) {
            val success = when (action.type) {
                ActionType.ADD_FRIENDSHIP_XP -> {
                    val xp = action.value.toIntOrNull() ?: 0
                    if (xp > 0) {
                        friendshipRepository.addFriendshipXp(action.targetId, xp) != null
                    } else false
                }
                ActionType.UNLOCK_VOCABULARY -> {
                    val wordIds = action.value.split(",").map { it.trim() }
                    var allSuccess = true
                    wordIds.forEach { wordId ->
                        val result = vocabularyRepository.discoverWord(wordId)
                        if (result is com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Error) {
                            allSuccess = false
                        }
                    }
                    allSuccess
                }
                ActionType.PRACTICE_SPEAKING -> {
                    val exerciseIds = action.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (exerciseIds.isEmpty()) {
                        false
                    } else {
                        var allSuccess = true
                        exerciseIds.forEach { exerciseId ->
                            val result = pronunciationRepository.unlockExercise(exerciseId)
                            if (result is com.sworddao.phoenix.feature.pronunciation.data.PronunciationResultStatus.Error) {
                                allSuccess = false
                            }
                        }
                        allSuccess
                    }
                }
                ActionType.PRACTICE_LISTENING -> {
                    val exerciseIds = action.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (exerciseIds.isEmpty()) {
                        false
                    } else {
                        var allSuccess = true
                        exerciseIds.forEach { exerciseId ->
                            val result = listeningRepository.unlockExercise(exerciseId)
                            if (result is com.sworddao.phoenix.feature.listening.data.ListeningResultStatus.Error) {
                                allSuccess = false
                            }
                        }
                        allSuccess
                    }
                }
                ActionType.PRACTICE_READING -> {
                    val exerciseIds = action.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (exerciseIds.isEmpty()) {
                        false
                    } else {
                        var allSuccess = true
                        exerciseIds.forEach { exerciseId ->
                            val result = readingRepository.unlockExercise(exerciseId)
                            if (result is com.sworddao.phoenix.feature.reading.data.ReadingResultStatus.Error) {
                                allSuccess = false
                            }
                        }
                        allSuccess
                    }
                }
                ActionType.PRACTICE_WRITING -> {
                    val exerciseIds = action.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (exerciseIds.isEmpty()) {
                        false
                    } else {
                        var allSuccess = true
                        exerciseIds.forEach { exerciseId ->
                            val result = writingRepository.unlockExercise(exerciseId)
                            if (result is com.sworddao.phoenix.feature.writing.data.WritingResultStatus.Error) {
                                allSuccess = false
                            }
                        }
                        allSuccess
                    }
                }
                ActionType.COMPLETE_QUEST -> {
                    val result = questRepository.completeQuest(action.targetId)
                    result is com.sworddao.phoenix.feature.quest.data.QuestResult.QuestCompleted ||
                            result is com.sworddao.phoenix.feature.quest.data.QuestResult.Success
                }
                ActionType.GIVE_ITEM -> {
                    true
                }
            }

            results.add(
                ProcessedAction(
                    type = action.type,
                    targetId = action.targetId,
                    value = action.value,
                    success = success
                )
            )
        }

        return results
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
