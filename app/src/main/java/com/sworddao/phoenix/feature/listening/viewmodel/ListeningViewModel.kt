package com.sworddao.phoenix.feature.listening.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.listening.data.AudioEngine
import com.sworddao.phoenix.feature.listening.data.AudioPlaybackState
import com.sworddao.phoenix.feature.listening.data.AudioPlaybackStateInfo
import com.sworddao.phoenix.feature.listening.data.ListeningAttempt
import com.sworddao.phoenix.feature.listening.data.ListeningExercise
import com.sworddao.phoenix.feature.listening.data.ListeningProgress
import com.sworddao.phoenix.feature.listening.data.ListeningResult
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus
import com.sworddao.phoenix.feature.listening.data.ListeningSession
import com.sworddao.phoenix.feature.listening.data.ListeningSessionConfig
import com.sworddao.phoenix.feature.listening.data.ListeningStatistics
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListeningUiState(
    val session: ListeningSession? = null,
    val exercises: List<ListeningExercise> = emptyList(),
    val currentExercise: ListeningExercise? = null,
    val currentProgress: ListeningProgress? = null,
    val statistics: ListeningStatistics = ListeningStatistics(),
    val lastResult: ListeningResult? = null,
    val selectedChoiceId: String? = null,
    val playbackState: AudioPlaybackStateInfo = AudioPlaybackStateInfo(),
    val playbackRate: Float = 1f,
    val replayCount: Int = 0,
    val isSessionComplete: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val repository: ListeningRepository,
    private val audioEngine: AudioEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListeningUiState())
    val uiState: StateFlow<ListeningUiState> = _uiState.asStateFlow()

    private var exerciseStartedAt: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            audioEngine.getPlaybackState().collect { playback ->
                _uiState.update { it.copy(playbackState = playback) }
            }
        }
    }

    fun startPractice(wordId: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val config = if (wordId.isNotEmpty()) {
                ListeningSessionConfig(wordIds = listOf(wordId), exerciseCount = 5)
            } else {
                ListeningSessionConfig(exerciseCount = 5)
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
                        error = throwable.message ?: "无法开始聆听练习",
                    )
                }
            }
        }
    }

    fun playCurrent() {
        val exercise = _uiState.value.currentExercise ?: return
        val playback = _uiState.value.playbackState

        when {
            playback.state == AudioPlaybackState.PLAYING -> pause()
            playback.state == AudioPlaybackState.PAUSED -> resume()
            else -> play(exercise)
        }
    }

    fun replay() {
        val exercise = _uiState.value.currentExercise ?: return
        viewModelScope.launch {
            audioEngine.stop()
            audioEngine.play(exercise.clip, _uiState.value.playbackRate)
            repository.recordReplay(exercise.id)
            _uiState.update {
                it.copy(
                    playbackState = AudioPlaybackStateInfo(
                        state = AudioPlaybackState.PLAYING,
                        clipId = exercise.clip.id,
                        playbackRate = it.playbackRate,
                        durationMs = exercise.clip.durationMs,
                    ),
                )
            }
        }
    }

    fun setPlaybackRate(rate: Float) {
        val exercise = _uiState.value.currentExercise ?: return
        _uiState.update { it.copy(playbackRate = rate) }
        val playback = _uiState.value.playbackState
        if (playback.state == AudioPlaybackState.PLAYING ||
            playback.state == AudioPlaybackState.PAUSED
        ) {
            viewModelScope.launch {
                audioEngine.stop()
                audioEngine.play(exercise.clip, rate)
            }
        }
    }

    fun toggleSlowPlayback() {
        val current = _uiState.value.playbackRate
        setPlaybackRate(if (current == 1f) SLOW_RATE else 1f)
    }

    fun selectChoice(choiceId: String) {
        val exercise = _uiState.value.currentExercise ?: return
        if (_uiState.value.lastResult?.attempt?.wasCorrect == true) return

        val chosenIndex = exercise.choices.indexOfFirst { it.id == choiceId }
        if (chosenIndex < 0) return

        val correct = chosenIndex == exercise.correctChoiceIndex
        val timeTakenMs = (System.currentTimeMillis() - exerciseStartedAt).coerceAtLeast(1)

        viewModelScope.launch {
            val attempt = ListeningAttempt(
                exerciseId = exercise.id,
                wordId = exercise.relatedWordId ?: exercise.clip.wordId,
                chosenChoiceId = choiceId,
                wasCorrect = correct,
                replayCount = _uiState.value.replayCount,
                timeTakenMs = timeTakenMs,
            )

            _uiState.update { it.copy(selectedChoiceId = choiceId) }

            when (val result = repository.submitAnswer(attempt)) {
                is ListeningResultStatus.ExerciseCompleted -> {
                    _uiState.update {
                        it.copy(
                            lastResult = result.result,
                            statistics = it.statistics,
                        )
                    }
                    refreshStatistics()
                }
                is ListeningResultStatus.Error -> {
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
                        replayCount = 0,
                    )
                }
                loadExercise(next)
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun play(exercise: ListeningExercise) {
        viewModelScope.launch {
            audioEngine.play(exercise.clip, _uiState.value.playbackRate)
            _uiState.update {
                it.copy(
                    playbackState = AudioPlaybackStateInfo(
                        state = AudioPlaybackState.PLAYING,
                        clipId = exercise.clip.id,
                        playbackRate = it.playbackRate,
                        durationMs = exercise.clip.durationMs,
                    ),
                )
            }
        }
    }

    private fun pause() {
        viewModelScope.launch {
            audioEngine.pause()
        }
    }

    private fun resume() {
        viewModelScope.launch {
            audioEngine.resume()
        }
    }

    private fun completeSession() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            when (val result = repository.completeSession(session)) {
                is ListeningResultStatus.SessionCompleted -> {
                    _uiState.update {
                        it.copy(
                            session = result.session,
                            statistics = result.statistics,
                            isSessionComplete = true,
                        )
                    }
                }
                is ListeningResultStatus.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> Unit
            }
        }
    }

    private suspend fun refreshStatistics() {
        val statistics = repository.getListeningStatistics().first()
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

    private fun loadExercise(exercise: ListeningExercise) {
        viewModelScope.launch {
            exerciseStartedAt = System.currentTimeMillis()
            val itemId = exercise.relatedWordId ?: exercise.clip.id
            val progress = repository.getListeningProgress(itemId).first()
            val statistics = repository.getListeningStatistics().first()
            _uiState.update {
                it.copy(
                    currentExercise = exercise,
                    currentProgress = progress,
                    statistics = statistics,
                    selectedChoiceId = null,
                    lastResult = null,
                    replayCount = 0,
                    isLoading = false,
                )
            }
        }
    }

    companion object {
        private const val SLOW_RATE = 0.75f
    }
}
