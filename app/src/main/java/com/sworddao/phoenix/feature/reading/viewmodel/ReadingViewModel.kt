package com.sworddao.phoenix.feature.reading.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.reading.data.CharacterRevealState
import com.sworddao.phoenix.feature.reading.data.HanziRenderer
import com.sworddao.phoenix.feature.reading.data.ReadingAttempt
import com.sworddao.phoenix.feature.reading.data.ReadingExercise
import com.sworddao.phoenix.feature.reading.data.ReadingProgress
import com.sworddao.phoenix.feature.reading.data.ReadingResult
import com.sworddao.phoenix.feature.reading.data.ReadingResultStatus
import com.sworddao.phoenix.feature.reading.data.ReadingSession
import com.sworddao.phoenix.feature.reading.data.ReadingSessionConfig
import com.sworddao.phoenix.feature.reading.data.ReadingStatistics
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingUiState(
    val session: ReadingSession? = null,
    val exercises: List<ReadingExercise> = emptyList(),
    val currentExercise: ReadingExercise? = null,
    val currentProgress: ReadingProgress? = null,
    val statistics: ReadingStatistics = ReadingStatistics(),
    val lastResult: ReadingResult? = null,
    val selectedChoiceId: String? = null,
    val revealMode: CharacterRevealState = CharacterRevealState.PINYIN_ONLY,
    val isHanziRevealed: Boolean = false,
    val autoRevealDelayMs: Long = 2000L,
    val isSessionComplete: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val repository: ReadingRepository,
    val renderer: HanziRenderer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private var exerciseStartedAt: Long = System.currentTimeMillis()
    private var autoRevealJob: Job? = null

    fun startPractice(wordId: String = "", showHanzi: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    revealMode = if (showHanzi) {
                        CharacterRevealState.HANZI_AND_PINYIN
                    } else {
                        CharacterRevealState.PINYIN_ONLY
                    },
                )
            }
            val config = if (wordId.isNotEmpty()) {
                ReadingSessionConfig(wordIds = listOf(wordId), exerciseCount = 5)
            } else {
                ReadingSessionConfig(exerciseCount = 5)
            }

            runCatching {
                val session = repository.startSession(config)
                val allExercises = repository.getAllExercises().first()
                val exercises = session.exerciseIds.mapNotNull { id ->
                    allExercises.find { it.id == id }
                }
                session to exercises
            }.onSuccess { (session, exercises) ->
                _uiState.update {
                    it.copy(
                        session = session,
                        exercises = exercises,
                        isLoading = false,
                    )
                }
                loadFirstExercise()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "无法开始阅读练习",
                    )
                }
            }
        }
    }

    fun revealHanzi() {
        val state = _uiState.value
        if (state.isHanziRevealed) return
        val exercise = state.currentExercise ?: return

        _uiState.update { it.copy(isHanziRevealed = true) }
        viewModelScope.launch {
            val wordId = exercise.relatedWordId ?: exercise.id
            repository.recordReveal(wordId)
        }
    }

    fun setAutoRevealDelay(delayMs: Long) {
        _uiState.update { it.copy(autoRevealDelayMs = delayMs.coerceAtLeast(MIN_AUTO_REVEAL_DELAY_MS)) }
        if (_uiState.value.revealMode == CharacterRevealState.AUTO_REVEAL) {
            scheduleAutoReveal()
        }
    }

    fun selectChoice(choiceId: String) {
        val exercise = _uiState.value.currentExercise ?: return
        if (_uiState.value.lastResult?.attempt?.wasCorrect == true) return

        val chosenIndex = exercise.choices.indexOfFirst { it.id == choiceId }
        if (chosenIndex < 0) return

        val correct = chosenIndex == exercise.correctChoiceIndex
        val timeTakenMs = (System.currentTimeMillis() - exerciseStartedAt).coerceAtLeast(1)

        viewModelScope.launch {
            val revealedBeforeAnswer = _uiState.value.isHanziRevealed
            val attempt = ReadingAttempt(
                exerciseId = exercise.id,
                wordId = exercise.relatedWordId,
                chosenChoiceId = choiceId,
                wasCorrect = correct,
                revealedHanziBeforeAnswer = revealedBeforeAnswer,
                timeTakenMs = timeTakenMs,
            )

            _uiState.update { it.copy(selectedChoiceId = choiceId) }

            when (val result = repository.submitAnswer(attempt)) {
                is ReadingResultStatus.ExerciseCompleted -> {
                    _uiState.update {
                        it.copy(
                            lastResult = result.result,
                            isHanziRevealed = true,
                        )
                    }
                    refreshStatistics()
                }
                is ReadingResultStatus.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> Unit
            }
        }
    }

    fun nextExercise() {
        val state = _uiState.value
        val session = state.session ?: return
        val currentIndex = state.exercises.indexOf(state.currentExercise)
        val nextIndex = currentIndex + 1

        if (nextIndex >= state.exercises.size) {
            completeSession()
        } else {
            val next = state.exercises.getOrNull(nextIndex)
            if (next != null) {
                _uiState.update { sessionCopy ->
                    sessionCopy.copy(
                        session = sessionCopy.session?.copy(currentExerciseIndex = nextIndex),
                        selectedChoiceId = null,
                        lastResult = null,
                        isHanziRevealed = false,
                    )
                }
                loadExercise(next)
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun completeSession() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            when (val result = repository.completeSession(session)) {
                is ReadingResultStatus.SessionCompleted -> {
                    _uiState.update {
                        it.copy(
                            session = result.session,
                            statistics = result.statistics,
                            isSessionComplete = true,
                        )
                    }
                }
                is ReadingResultStatus.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> Unit
            }
        }
    }

    private suspend fun refreshStatistics() {
        val statistics = repository.getReadingStatistics().first()
        _uiState.update { it.copy(statistics = statistics) }
    }

    private fun loadFirstExercise() {
        val first = _uiState.value.exercises.firstOrNull()
        if (first != null) {
            loadExercise(first)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadExercise(exercise: ReadingExercise) {
        viewModelScope.launch {
            exerciseStartedAt = System.currentTimeMillis()
            val itemId = exercise.relatedWordId ?: exercise.id
            val progress = repository.getReadingProgress(itemId).first()
            val statistics = repository.getReadingStatistics().first()
            _uiState.update {
                it.copy(
                    currentExercise = exercise,
                    currentProgress = progress,
                    statistics = statistics,
                    selectedChoiceId = null,
                    lastResult = null,
                    isHanziRevealed = false,
                    isLoading = false,
                )
            }
            if (_uiState.value.revealMode == CharacterRevealState.AUTO_REVEAL) {
                scheduleAutoReveal()
            }
        }
    }

    private fun scheduleAutoReveal() {
        autoRevealJob?.cancel()
        autoRevealJob = viewModelScope.launch {
            delay(_uiState.value.autoRevealDelayMs)
            if (!_uiState.value.isHanziRevealed && _uiState.value.lastResult == null) {
                revealHanzi()
            }
        }
    }

    companion object {
        const val DEFAULT_AUTO_REVEAL_DELAY_MS = 2000L
        const val MIN_AUTO_REVEAL_DELAY_MS = 500L
    }
}