package com.sworddao.phoenix.feature.listening.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class AudioPlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR,
}

data class AudioPlaybackStateInfo(
    val state: AudioPlaybackState = AudioPlaybackState.IDLE,
    val clipId: String? = null,
    val playbackRate: Float = 1f,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

data class AudioEngineInfo(
    val name: String,
    val version: String,
    val requiresNetwork: Boolean,
    val supportedFeatures: List<AudioEngineFeature> = emptyList(),
)

enum class AudioEngineFeature {
    OFFLINE_PLAYBACK,
    SLOW_PLAYBACK,
    LOOP_REPLAY,
    STREAMING,
    VOLUME_CONTROL,
}

sealed class AudioEngineResult {
    data class Success(val message: String = "") : AudioEngineResult()
    data class Error(val message: String) : AudioEngineResult()
    data class NotAvailable(val reason: String) : AudioEngineResult()
}

interface AudioEngine {
    val name: String
    val isAvailable: Boolean
    val supportedFormats: List<String>

    suspend fun initialize(): AudioEngineResult
    suspend fun play(clip: AudioClip, playbackRate: Float = 1f): AudioEngineResult
    suspend fun pause(): AudioEngineResult
    suspend fun resume(): AudioEngineResult
    suspend fun stop(): AudioEngineResult
    fun getPlaybackState(): Flow<AudioPlaybackStateInfo>
    fun getEngineInfo(): AudioEngineInfo
    suspend fun shutdown()
}

@Singleton
class MockAudioEngine @Inject constructor() : AudioEngine {

    override val name: String = "MockAudioEngine"
    override val isAvailable: Boolean = true
    override val supportedFormats: List<String> = listOf("mock")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var completionJob: Job? = null
    private var currentClip: AudioClip? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackStateInfo())
    val playbackState: StateFlow<AudioPlaybackStateInfo> = _playbackState

    override suspend fun initialize(): AudioEngineResult =
        AudioEngineResult.Success("Mock audio engine initialized")

    override suspend fun play(clip: AudioClip, playbackRate: Float): AudioEngineResult {
        completionJob?.cancel()
        currentClip = clip
        _playbackState.value = AudioPlaybackStateInfo(
            state = AudioPlaybackState.PLAYING,
            clipId = clip.id,
            playbackRate = playbackRate,
            positionMs = 0,
            durationMs = clip.durationMs,
        )
        val scaledDuration = (clip.durationMs / playbackRate.coerceAtLeast(0.1f)).toLong()
        completionJob = scope.launch {
            delay(scaledDuration)
            _playbackState.value = AudioPlaybackStateInfo(
                state = AudioPlaybackState.COMPLETED,
                clipId = clip.id,
                playbackRate = playbackRate,
                positionMs = clip.durationMs,
                durationMs = clip.durationMs,
            )
        }
        return AudioEngineResult.Success("Playing ${clip.id} at ${playbackRate}x")
    }

    override suspend fun pause(): AudioEngineResult {
        if (_playbackState.value.state == AudioPlaybackState.PLAYING) {
            completionJob?.cancel()
            _playbackState.value = _playbackState.value.copy(
                state = AudioPlaybackState.PAUSED,
                positionMs = 0,
            )
            return AudioEngineResult.Success("Playback paused")
        }
        return AudioEngineResult.Error("Nothing is playing")
    }

    override suspend fun resume(): AudioEngineResult {
        val clip = currentClip ?: return AudioEngineResult.Error("No clip loaded")
        val rate = _playbackState.value.playbackRate
        _playbackState.value = _playbackState.value.copy(
            state = AudioPlaybackState.PLAYING,
            positionMs = 0,
        )
        val scaledDuration = (clip.durationMs / rate.coerceAtLeast(0.1f)).toLong()
        completionJob = scope.launch {
            delay(scaledDuration)
            _playbackState.value = _playbackState.value.copy(state = AudioPlaybackState.COMPLETED)
        }
        return AudioEngineResult.Success("Playback resumed")
    }

    override suspend fun stop(): AudioEngineResult {
        completionJob?.cancel()
        currentClip = null
        _playbackState.value = AudioPlaybackStateInfo()
        return AudioEngineResult.Success("Playback stopped")
    }

    override fun getPlaybackState(): Flow<AudioPlaybackStateInfo> = _playbackState

    override fun getEngineInfo(): AudioEngineInfo = AudioEngineInfo(
        name = "Mock Audio Engine",
        version = "1.0.0",
        requiresNetwork = false,
        supportedFeatures = listOf(
            AudioEngineFeature.OFFLINE_PLAYBACK,
            AudioEngineFeature.SLOW_PLAYBACK,
            AudioEngineFeature.LOOP_REPLAY,
        ),
    )

    override suspend fun shutdown() {
        completionJob?.cancel()
        currentClip = null
        _playbackState.value = AudioPlaybackStateInfo()
    }
}
