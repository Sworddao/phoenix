package com.sworddao.phoenix.feature.gameplay.viewmodel

import androidx.lifecycle.ViewModel
import com.sworddao.phoenix.feature.gameplay.data.DialogueResultHolder
import com.sworddao.phoenix.feature.gameplay.ui.CelebrationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CelebrationViewModel @Inject constructor(
    private val dialogueResultHolder: DialogueResultHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(CelebrationUiState())
    val uiState: StateFlow<CelebrationUiState> = _uiState.asStateFlow()

    fun loadResults() {
        val holder = dialogueResultHolder
        _uiState.value = CelebrationUiState(
            npcId = holder.lastNpcId,
            npcName = holder.lastNpcId.replace("_", " ").replaceFirstChar { it.uppercase() },
            dialogueTitle = holder.lastDialogueId.replace("_", " ").replaceFirstChar { it.uppercase() },
            processedActions = holder.lastProcessedActions,
            xpEarned = holder.lastXpEarned,
            baoMessage = getBaoMessage(holder.lastProcessedActions.size),
            milestonesUnlocked = getMilestones(holder.lastProcessedActions)
        )
        dialogueResultHolder.clear()
    }

    private fun getBaoMessage(actionCount: Int): String {
        return when {
            actionCount == 0 -> "继续加油！你做得很好！"
            actionCount == 1 -> "不错的对话！继续保持！"
            else -> "太棒了！你获得了好多奖励！"
        }
    }

    private fun getMilestones(actions: List<com.sworddao.phoenix.feature.dialogue.viewmodel.ProcessedAction>): List<String> {
        val milestones = mutableListOf<String>()
        val successfulActions = actions.filter { it.success }

        if (successfulActions.any { it.type == com.sworddao.phoenix.feature.dialogue.data.ActionType.ADD_FRIENDSHIP_XP }) {
            milestones.add("友谊提升")
        }
        if (successfulActions.any { it.type == com.sworddao.phoenix.feature.dialogue.data.ActionType.UNLOCK_VOCABULARY }) {
            milestones.add("新词汇解锁")
        }
        if (successfulActions.any { it.type == com.sworddao.phoenix.feature.dialogue.data.ActionType.COMPLETE_QUEST }) {
            milestones.add("任务完成")
        }
        if (successfulActions.any { it.type == com.sworddao.phoenix.feature.dialogue.data.ActionType.PRACTICE_SPEAKING }) {
            milestones.add("口语练习解锁")
        }
        if (successfulActions.any { it.type == com.sworddao.phoenix.feature.dialogue.data.ActionType.PRACTICE_LISTENING }) {
            milestones.add("聆听练习解锁")
        }
        if (successfulActions.any { it.type == com.sworddao.phoenix.feature.dialogue.data.ActionType.PRACTICE_READING }) {
            milestones.add("阅读练习解锁")
        }
        return milestones
    }
}
