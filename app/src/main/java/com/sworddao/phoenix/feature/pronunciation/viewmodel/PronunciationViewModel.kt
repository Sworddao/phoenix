package com.sworddao.phoenix.feature.pronunciation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationAttempt
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationEngine
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationProgress
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationResult
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationResultStatus
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationSession
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationSessionConfig
import com.sworddao.phoenix.feature.pronunciation.data.RecognitionConfig
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingDifficulty
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingExercise
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingStatistics
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PronunciationUiState(
    val session: PronunciationSession? = null,
    val exercises: List<SpeakingExercise> = emptyList(),
    val currentExercise: SpeakingExercise? = null,
    val currentProgress: PronunciationProgress? = null,
    val statistics: SpeakingStatistics = SpeakingStatistics(),
    val lastResult: PronunciationResult? = null,
    val isSessionComplete: Boolean = false,
    val isSpeaking: Boolean = false,
    val isRecording: Boolean = false,
    val isListening: Boolean = false,
    val transcript: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class PronunciationViewModel @Inject constructor(
    private val repository: PronunciationRepository,
    private val engine: PronunciationEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PronunciationUiState())
    val uiState: StateFlow<PronunciationUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            repository.getSpeakingStatistics().collect { statistics ->
                _uiState.update { it.copy(statistics = statistics) }
            }
        }
    }

    fun startPractice(wordId: String? = null) {
        viewModelScope.launch {
            val config = if (wordId != null) {
                PronunciationSessionConfig(
                    exerciseType = com.sworddao.phoenix.feature.pronunciation.data.SpeakingExerciseType.VOCABULARY_WORD,
                    difficulty = SpeakingDifficulty.BEGINNER,
                    exerciseCount = 3,
                    wordIds = listOf(wordId),
                )
            } else {
                PronunciationSessionConfig(exerciseCount = 5)
            }

            val session = repository.startSession(config)
            val allExercises = repository.getAllExercises().first()
            val exercises = session.exerciseIds.mapNotNull { id -> allExercises.find { it.id == id } }

            _uiState.value = PronunciationUiState(
                session = session,
                exercises = exercises,
                currentExercise = exercises.firstOrNull(),
                statistics = _uiState.value.statistics,
                isSessionComplete = exercises.isEmpty(),
                isLoading = false,
            )
            loadWordProgress(exercises.firstOrNull()?.wordId)
        }
    }

    fun demonstrate() {
        if (_uiState.value.isSpeaking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true) }
            delay(1600)
            _uiState.update { it.copy(isSpeaking = false) }
        }
    }

    fun startRecording() {
        val exercise = _uiState.value.currentExercise ?: return
        if (_uiState.value.isRecording) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecording = true,
                    isListening = true,
                    transcript = "",
                    lastResult = null,
                )
            }

            val startedAt = System.currentTimeMillis()
            val finalTranscript = runCatching {
                var lastText = exercise.expectedPinyin
                engine.startListening(
                    RecognitionConfig(
                        expectedPhrase = exercise.expectedPinyin,
                        expectedPinyin = exercise.expectedPinyin,
                    )
                ).collect { partial ->
                    lastText = partial.text
                    _uiState.update {
                        it.copy(isListening = !partial.isFinal, transcript = partial.text)
                    }
                }
                lastText
            }.getOrElse { exercise.expectedPinyin }

            val durationMs = System.currentTimeMillis() - startedAt

            val attempt = repository.evaluatePronunciationOffline(
                expectedText = exercise.expectedText,
                expectedPinyin = exercise.expectedPinyin,
                spokenText = finalTranscript,
            ).copy(
                exerciseId = exercise.id,
                wordId = exercise.wordId,
                phraseId = exercise.phraseId,
                durationMs = durationMs,
            )

            _uiState.update { it.copy(isRecording = false, isListening = false) }
            submitAttempt(attempt)
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
            val next = state.exercises[nextIndex]
            _uiState.update {
                it.copy(
                    currentExercise = next,
                    lastResult = null,
                    transcript = "",
                )
            }
            loadWordProgress(next.wordId)
        }
    }

    fun repeatExercise() {
        _uiState.update { it.copy(lastResult = null, transcript = "") }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun submitAttempt(attempt: PronunciationAttempt) {
        when (val status = repository.submitAttempt(attempt)) {
            is PronunciationResultStatus.ExerciseCompleted -> {
                val result = status.result
                val currentWordId = _uiState.value.currentExercise?.wordId
                _uiState.update {
                    it.copy(
                        lastResult = result,
                        session = it.session?.copy(
                            attempts = it.session.attempts + attempt,
                            totalXpEarned = it.session.totalXpEarned + result.xpEarned,
                            totalFriendshipBonus = it.session.totalFriendshipBonus + result.friendshipBonusEarned,
                        ),
                        statistics = it.statistics.copy(
                            totalAttempts = it.statistics.totalAttempts + 1,
                            successfulAttempts = it.statistics.successfulAttempts +
                                (if (attempt.wasSuccessful) 1 else 0),
                        ),
                    )
                }
                loadWordProgress(currentWordId)
            }
            is PronunciationResultStatus.Error -> {
                _uiState.update { it.copy(error = status.message) }
            }
            else -> {}
        }
    }

    private fun completeSession() {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            when (val status = repository.completeSession(session)) {
                is PronunciationResultStatus.SessionCompleted -> {
                    _uiState.update {
                        it.copy(
                            session = status.session,
                            statistics = status.statistics,
                            isSessionComplete = true,
                            lastResult = null,
                        )
                    }
                }
                is PronunciationResultStatus.Error -> {
                    _uiState.update { it.copy(error = status.message) }
                }
                else -> {}
            }
        }
    }

    private fun loadWordProgress(wordId: String?) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            val flow = if (wordId != null) {
                repository.getPronunciationProgress(wordId)
            } else {
                flowOf(null)
            }
            flow.collect { progress ->
                _uiState.update { it.copy(currentProgress = progress) }
            }
        }
    }
}