package com.sworddao.phoenix.feature.friendship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.friendship.data.ConversationMemory
import com.sworddao.phoenix.feature.friendship.data.FriendshipEvent
import com.sworddao.phoenix.feature.friendship.data.FriendshipState
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.npc.data.Npc
import com.sworddao.phoenix.feature.npc.domain.NpcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendshipUiState(
    val friendshipState: FriendshipState? = null,
    val conversationHistory: List<ConversationMemory> = emptyList(),
    val friendshipEvents: List<FriendshipEvent> = emptyList(),
    val allFriendshipStates: List<FriendshipState> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showLevelUpDialog: Boolean = false,
    val levelUpInfo: LevelUpInfo? = null
)

data class LevelUpInfo(
    val npcName: String,
    val newLevelTitle: String,
    val previousLevelTitle: String
)

@HiltViewModel
class FriendshipViewModel @Inject constructor(
    private val friendshipRepository: FriendshipRepository,
    private val npcRepository: NpcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendshipUiState())
    val uiState: StateFlow<FriendshipUiState> = _uiState.asStateFlow()

    private val _selectedNpcId = MutableStateFlow<String?>(null)

    init {
        loadAllFriendshipStates()
    }

    fun selectNpc(npcId: String) {
        _selectedNpcId.value = npcId
        loadFriendshipForNpc(npcId)
    }

    fun loadFriendshipForNpc(npcId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            friendshipRepository.getFriendshipState(npcId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { state ->
                    _uiState.value = _uiState.value.copy(
                        friendshipState = state,
                        isLoading = false
                    )
                }
        }

        viewModelScope.launch {
            friendshipRepository.getConversationHistory(npcId)
                .catch { }
                .collect { history ->
                    _uiState.value = _uiState.value.copy(
                        conversationHistory = history
                    )
                }
        }

        viewModelScope.launch {
            friendshipRepository.getFriendshipEvents(npcId)
                .catch { }
                .collect { events ->
                    _uiState.value = _uiState.value.copy(
                        friendshipEvents = events
                    )
                }
        }
    }

    private fun loadAllFriendshipStates() {
        viewModelScope.launch {
            friendshipRepository.getAllFriendshipStates()
                .catch { }
                .collect { states ->
                    _uiState.value = _uiState.value.copy(
                        allFriendshipStates = states
                    )
                }
        }
    }

    fun addXp(npcId: String, xpAmount: Int) {
        viewModelScope.launch {
            val previousState = _uiState.value.friendshipState
            val previousLevel = previousState?.friendshipLevel

            val newState = friendshipRepository.addFriendshipXp(npcId, xpAmount)

            if (newState != null && previousLevel != null && newState.friendshipLevel != previousLevel) {
                val npc = getNpcName(npcId)
                _uiState.value = _uiState.value.copy(
                    showLevelUpDialog = true,
                    levelUpInfo = LevelUpInfo(
                        npcName = npc,
                        newLevelTitle = newState.friendshipLevel.displayTitle,
                        previousLevelTitle = previousLevel.displayTitle
                    )
                )
            }

            loadFriendshipForNpc(npcId)
        }
    }

    fun recordConversation(
        npcId: String,
        dialogueId: String,
        dialogueTitle: String,
        xpGained: Int,
        topicsDiscussed: List<String> = emptyList(),
        choicesSummary: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            friendshipRepository.recordConversation(
                npcId = npcId,
                dialogueId = dialogueId,
                dialogueTitle = dialogueTitle,
                xpGained = xpGained,
                topicsDiscussed = topicsDiscussed,
                choicesSummary = choicesSummary
            )
            loadFriendshipForNpc(npcId)
            loadAllFriendshipStates()
        }
    }

    fun dismissLevelUpDialog() {
        _uiState.value = _uiState.value.copy(
            showLevelUpDialog = false,
            levelUpInfo = null
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun getNpcName(npcId: String): String {
        var name = "Friend"
        npcRepository.getNpcById(npcId).collect { npc ->
            name = npc?.displayName ?: "Friend"
        }
        return name
    }
}
