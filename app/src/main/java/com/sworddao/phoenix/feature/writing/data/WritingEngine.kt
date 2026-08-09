package com.sworddao.phoenix.feature.writing.data

import kotlinx.serialization.Serializable

@Serializable
data class EngineSessionState(
    val sessionId: String,
    val characterId: String,
    val hanzi: String,
    val expectedStrokeCount: Int = 0,
    val nextStrokeIndex: Int = 0,
    val strokesCompleted: List<Int> = emptyList(),
    val correctOrderCount: Int = 0,
    val correctDirectionCount: Int = 0,
    val errorCount: Int = 0,
    val isComplete: Boolean = false,
) {
    val totalStrokes: Int
        get() = strokesCompleted.size

    val progress: Float
        get() = if (expectedStrokeCount > 0) (strokesCompleted.size.toFloat()) / expectedStrokeCount else 0f
}

@Serializable
data class WritingEngineInfo(
    val name: String,
    val version: String,
    val supportedTypes: List<WritingExerciseType>,
    val supportsStrokeAnimation: Boolean = true,
)

@Serializable
data class EngineStrokeFeedback(
    val sessionId: String,
    val strokeIndex: Int,
    val expectedType: StrokeType,
    val expectedDirection: StrokeDirection,
    val receivedDirection: StrokeDirection? = null,
    val wasOrderCorrect: Boolean = false,
    val wasDirectionCorrect: Boolean = false,
    val wasCorrect: Boolean = false,
    val message: String = "",
)

interface WritingEngine {
    val name: String
    val isAvailable: Boolean
    fun startSession(character: HanziCharacter): EngineSessionState
    fun expectedStroke(sessionId: String): HanziStroke?
    fun recordStroke(sessionId: String, strokeIndex: Int, direction: StrokeDirection): EngineStrokeFeedback
    fun isComplete(sessionId: String): Boolean
    fun progress(sessionId: String): Float
    fun correctOrderCount(sessionId: String): Int
    fun correctDirectionCount(sessionId: String): Int
    fun reset(sessionId: String)
    fun endSession(sessionId: String)
    fun getEngineInfo(): WritingEngineInfo
}
