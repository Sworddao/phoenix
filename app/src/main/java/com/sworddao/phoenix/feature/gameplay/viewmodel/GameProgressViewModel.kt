package com.sworddao.phoenix.feature.gameplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.gameplay.data.GameMilestone
import com.sworddao.phoenix.feature.gameplay.data.GameProgress
import com.sworddao.phoenix.feature.gameplay.data.SessionSummary
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameProgressUiState(
    val gameProgress: GameProgress = GameProgress(),
    val sessionSummary: SessionSummary = SessionSummary(),
    val baoMessage: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class GameProgressViewModel @Inject constructor(
    private val gameProgressRepository: GameProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameProgressUiState())
    val uiState: StateFlow<GameProgressUiState> = _uiState.asStateFlow()

    init {
        loadGameProgress()
        loadSessionSummary()
    }

    private fun loadGameProgress() {
        viewModelScope.launch {
            gameProgressRepository.getGameProgress()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { progress ->
                    _uiState.value = _uiState.value.copy(
                        gameProgress = progress,
                        isLoading = false
                    )
                }
        }
    }

    private fun loadSessionSummary() {
        viewModelScope.launch {
            gameProgressRepository.getSessionSummary()
                .catch { }
                .collect { summary ->
                    _uiState.value = _uiState.value.copy(
                        sessionSummary = summary
                    )
                }
        }
    }

    fun recordDialogueCompleted(npcId: String) {
        viewModelScope.launch {
            gameProgressRepository.recordDialogueCompleted(npcId)
            updateBaoMessage()
        }
    }

    fun recordWordDiscovered(wordId: String) {
        viewModelScope.launch {
            gameProgressRepository.recordWordDiscovered(wordId)
            updateBaoMessage()
        }
    }

    fun recordQuestCompleted(questId: String) {
        viewModelScope.launch {
            gameProgressRepository.recordQuestCompleted(questId)
            updateBaoMessage()
        }
    }

    fun recordFriendshipLevelUp(npcId: String) {
        viewModelScope.launch {
            gameProgressRepository.recordFriendshipLevelUp(npcId)
            updateBaoMessage()
        }
    }

    fun recordPassportStampEarned(regionId: String) {
        viewModelScope.launch {
            gameProgressRepository.recordPassportStampEarned(regionId)
            updateBaoMessage()
        }
    }

    fun recordListeningPractice() {
        viewModelScope.launch {
            gameProgressRepository.recordListeningPractice()
            updateBaoMessage()
        }
    }

    fun recordReadingPractice() {
        viewModelScope.launch {
            gameProgressRepository.recordReadingPractice()
            updateBaoMessage()
        }
    }

    fun unlockMilestone(milestone: GameMilestone) {
        viewModelScope.launch {
            gameProgressRepository.unlockMilestone(milestone)
            updateBaoMessage()
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            gameProgressRepository.resetSession()
            _uiState.value = _uiState.value.copy(baoMessage = "")
        }
    }

    private fun updateBaoMessage() {
        val progress = _uiState.value.gameProgress
        val summary = _uiState.value.sessionSummary

        val message = when {
            summary.milestonesUnlocked.isEmpty() -> ""
            GameMilestone.FIRST_DIALOGUE in summary.milestonesUnlocked ->
                "恭喜你完成了第一次对话！你正在成为一个真正的村民！"
            GameMilestone.FIRST_VOCABULARY in summary.milestonesUnlocked ->
                "太棒了！你学会了第一个词汇！继续努力！"
            GameMilestone.FIRST_LISTENING in summary.milestonesUnlocked ->
                "你的耳朵真厉害！学会了第一段聆听内容！"
            GameMilestone.FIRST_READING in summary.milestonesUnlocked ->
                "你的眼睛真厉害！读懂了第一段汉字！"
            GameMilestone.FIRST_QUEST in summary.milestonesUnlocked ->
                "了不起！你完成了第一个任务！村民们都很高兴！"
            GameMilestone.VILLAGE_EXPLORER in summary.milestonesUnlocked ->
                "你已经认识了村里的所有人！你真是个社交高手！"
            GameMilestone.WORD_COLLECTOR in summary.milestonesUnlocked ->
                "你已经学会了10个词汇！你的中文越来越好了！"
            GameMilestone.QUEST_MASTER in summary.milestonesUnlocked ->
                "你已经完成了5个任务！你是村里的英雄！"
            else -> "继续加油！你做得很好！"
        }

        _uiState.value = _uiState.value.copy(baoMessage = message)
    }

    fun dismissBaoMessage() {
        _uiState.value = _uiState.value.copy(baoMessage = "")
    }
}
