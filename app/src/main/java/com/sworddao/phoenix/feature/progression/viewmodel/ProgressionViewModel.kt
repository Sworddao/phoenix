package com.sworddao.phoenix.feature.progression.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import com.sworddao.phoenix.feature.progression.data.CurrentObjective
import com.sworddao.phoenix.feature.progression.data.DailyProgress
import com.sworddao.phoenix.feature.progression.data.FeatureUnlockEntry
import com.sworddao.phoenix.feature.progression.data.LearningProgress
import com.sworddao.phoenix.feature.progression.data.PlayerProgress
import com.sworddao.phoenix.feature.progression.data.RecentUnlock
import com.sworddao.phoenix.feature.progression.data.XpSource
import com.sworddao.phoenix.feature.progression.domain.ProgressionRepository
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import com.sworddao.phoenix.feature.world.domain.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressionUiState(
    val playerProgress: PlayerProgress = PlayerProgress(),
    val learningProgress: LearningProgress = LearningProgress(),
    val dailyProgress: DailyProgress = DailyProgress(),
    val recentUnlocks: List<RecentUnlock> = emptyList(),
    val currentObjectives: List<CurrentObjective> = emptyList(),
    val featureUnlockTimeline: List<FeatureUnlockEntry> = emptyList(),
    val lastXpSource: XpSource? = null,
    val lastXpAmount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ProgressionViewModel @Inject constructor(
    private val progressionRepository: ProgressionRepository,
    private val gameProgressRepository: GameProgressRepository,
    private val worldRepository: WorldRepository,
    private val questRepository: QuestRepository,
    private val passportRepository: PassportRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val friendshipRepository: FriendshipRepository,
    private val pronunciationRepository: PronunciationRepository,
    private val listeningRepository: ListeningRepository,
    private val readingRepository: ReadingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressionUiState())
    val uiState: StateFlow<ProgressionUiState> = _uiState.asStateFlow()

    init {
        observeSourceSystems()
        refresh()
    }

    private fun observeSourceSystems() {
        viewModelScope.launch {
            combine(
                gameProgressRepository.getGameProgress(),
                worldRepository.getAllRegions(),
                questRepository.getQuestStats(),
                passportRepository.getPassport(),
                vocabularyRepository.getStatistics(),
                friendshipRepository.getAllFriendshipStates(),
                pronunciationRepository.getSpeakingStatistics(),
                listeningRepository.getListeningStatistics(),
                readingRepository.getReadingStatistics(),
            ) { _: Array<Any?> ->
                Unit
            }.collect {
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                progressionRepository.refresh()
                val player = progressionRepository.getPlayerProgress().firstOrNull() ?: _uiState.value.playerProgress
                val learning = progressionRepository.getLearningProgress().firstOrNull() ?: _uiState.value.learningProgress
                val daily = progressionRepository.getDailyProgress().firstOrNull() ?: _uiState.value.dailyProgress
                val unlocks = progressionRepository.getRecentUnlocks().firstOrNull() ?: emptyList()
                val objectives = progressionRepository.getCurrentObjectives().firstOrNull() ?: emptyList()
                val timeline = progressionRepository.getFeatureUnlockTimeline().firstOrNull() ?: emptyList()
                _uiState.value = ProgressionUiState(
                    playerProgress = player,
                    learningProgress = learning,
                    dailyProgress = daily,
                    recentUnlocks = unlocks,
                    currentObjectives = objectives,
                    featureUnlockTimeline = timeline,
                    isLoading = false,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "无法刷新进度",
                )
            }
        }
    }

    fun awardXp(source: XpSource, count: Int = 1) {
        viewModelScope.launch {
            runCatching {
                progressionRepository.awardXp(source, count)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    lastXpSource = source,
                    lastXpAmount = source.baseXp * count,
                )
                refresh()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
