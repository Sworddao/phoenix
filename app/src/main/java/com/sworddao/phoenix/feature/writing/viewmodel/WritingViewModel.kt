package com.sworddao.phoenix.feature.writing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.writing.data.EngineStrokeFeedback
import com.sworddao.phoenix.feature.writing.data.StrokeDirection
import com.sworddao.phoenix.feature.writing.data.WritingAttempt
import com.sworddao.phoenix.feature.writing.data.WritingEngine
import com.sworddao.phoenix.feature.writing.data.WritingExercise
import com.sworddao.phoenix.feature.writing.data.WritingResult
import com.sworddao.phoenix.feature.writing.data.WritingResultStatus
import com.sworddao.phoenix.feature.writing.data.WritingSession
import com.sworddao.phoenix.feature.writing.data.WritingSessionConfig
import com.sworddao.phoenix.feature.writing.data.WritingStatistics
import com.sworddao.phoenix.feature.writing.data.WritingStrokeAnswer
import com.sworddao.phoenix.feature.writing.domain.WritingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WritingUiState(
    val session: WritingSession? = null,
    val exercises: List<WritingExercise> = emptyList(),
    val currentExercise: WritingExercise? = null,
    val strokesCompleted: List<Int> = emptyList(),
    val expectedStrokeIndex: Int = 0,
    val pendingStrokeAttempts: Int = 0,
    val strokeAnswers: List<WritingStrokeAnswer> = emptyList(),
    val isExerciseComplete: Boolean = false,
    val lastStrokeFeedback: EngineStrokeFeedback? = null,
    val lastResult: WritingResult? = null,
    val statistics: WritingStatistics = WritingStatistics(),
    val isSessionComplete: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val progress: Float
        get() = session?.progress ?: 0f

    val strokeProgress: Float
        get() {
            val total = currentExercise?.strokeCount ?: 0
            return if (total > 0) strokesCompleted.size.toFloat() / total else 0f
        }
}

@HiltViewModel
class WritingViewModel @Inject constructor(
    private val repository: WritingRepository,
    private val engine: WritingEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WritingUiState())
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    private var exerciseStartedAt: Long = System.currentTimeMillis()
    private var engineSessionId: String? = null

    fun startPractice(wordId: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val config = if (wordId.isNotEmpty()) {
                WritingSessionConfig(characterIds = listOf(wordId), exerciseCount = 5)
            } else {
                WritingSessionConfig(exerciseCount = 5)
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
                        error = throwable.message ?: "无法开始书写练习",
                    )
                }
            }
        }
    }

    fun recordStroke(direction: StrokeDirection) {
        val sessionId = engineSessionId ?: return
        val pendingIndex = _uiState.value.expectedStrokeIndex
        val feedback = engine.recordStroke(sessionId, pendingIndex, direction)
        val wasCorrect = feedback.wasCorrect

        _uiState.update { state ->
            val attempts = state.pendingStrokeAttempts + 1
            if (wasCorrect) {
                state.copy(
                    lastStrokeFeedback = feedback,
                    strokesCompleted = state.strokesCompleted + pendingIndex,
                    expectedStrokeIndex = pendingIndex + 1,
                    pendingStrokeAttempts = 0,
                    strokeAnswers = state.strokeAnswers + WritingStrokeAnswer(
                        strokeIndex = pendingIndex,
                        expectedType = feedback.expectedType,
                        expectedDirection = feedback.expectedDirection,
                        wasCorrect = true,
                        attempts = attempts,
                    ),
                )
            } else {
                state.copy(
                    lastStrokeFeedback = feedback,
                    pendingStrokeAttempts = attempts,
                )
            }
        }

        if (wasCorrect && engine.isComplete(sessionId)) {
            val state = _uiState.value
            if (state.expectedStrokeIndex >= (state.currentExercise?.strokeCount ?: 0)) {
                _uiState.update { it.copy(isExerciseComplete = true) }
                submitCurrentExercise()
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
                        currentExercise = next,
                        strokesCompleted = emptyList(),
                        expectedStrokeIndex = 0,
                        pendingStrokeAttempts = 0,
                        strokeAnswers = emptyList(),
                        isExerciseComplete = false,
                        lastStrokeFeedback = null,
                        lastResult = null,
                    )
                }
                startEngineFor(next)
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadFirstExercise() {
        val first = _uiState.value.exercises.firstOrNull()
        if (first != null) {
            startEngineFor(first)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun startEngineFor(exercise: WritingExercise) {
        exerciseStartedAt = System.currentTimeMillis()
        val state = engine.startSession(exercise.character)
        engineSessionId = state.sessionId
        _uiState.update {
            it.copy(
                currentExercise = exercise,
                strokesCompleted = emptyList(),
                expectedStrokeIndex = 0,
                pendingStrokeAttempts = 0,
                strokeAnswers = emptyList(),
                isExerciseComplete = false,
                lastStrokeFeedback = null,
                lastResult = null,
                isLoading = false,
            )
        }
    }

    private fun submitCurrentExercise() {
        val state = _uiState.value
        val exercise = state.currentExercise ?: return
        val timeTakenMs = (System.currentTimeMillis() - exerciseStartedAt).coerceAtLeast(1)

        val strokeAnswers = state.strokeAnswers.ifEmpty {
            exercise.character.strokes.mapIndexed { index, stroke ->
                WritingStrokeAnswer(
                    strokeIndex = index,
                    expectedType = stroke.type,
                    expectedDirection = stroke.direction,
                    wasCorrect = true,
                    attempts = 1,
                )
            }
        }

        val attempt = WritingAttempt(
            exerciseId = exercise.id,
            wordId = exercise.character.wordId,
            hanzi = exercise.hanzi,
            strokeAnswers = strokeAnswers,
            timeTakenMs = timeTakenMs,
        )

        viewModelScope.launch {
            when (val result = repository.submitAnswer(attempt)) {
                is WritingResultStatus.ExerciseCompleted -> {
                    _uiState.update { it.copy(lastResult = result.result) }
                    refreshStatistics()
                }
                is WritingResultStatus.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> Unit
            }
        }
    }

    private fun completeSession() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            when (val result = repository.completeSession(session)) {
                is WritingResultStatus.SessionCompleted -> {
                    _uiState.update {
                        it.copy(
                            session = result.session,
                            statistics = result.statistics,
                            isSessionComplete = true,
                        )
                    }
                }
                is WritingResultStatus.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> Unit
            }
        }
    }

    private suspend fun refreshStatistics() {
        val statistics = repository.getWritingStatistics().first()
        _uiState.update { it.copy(statistics = statistics) }
    }
}
