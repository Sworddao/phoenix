package com.sworddao.phoenix.feature.writing.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockWritingEngine @Inject constructor() : WritingEngine {

    override val name: String = "MockWritingEngine"
    override val isAvailable: Boolean = true

    private val sessions = mutableMapOf<String, EngineSessionState>()
    private val characters = mutableMapOf<String, HanziCharacter>()

    override fun startSession(character: HanziCharacter): EngineSessionState {
        characters[character.id] = character
        val state = EngineSessionState(
            sessionId = "writing_session_${character.id}",
            characterId = character.id,
            hanzi = character.hanzi,
            expectedStrokeCount = character.strokeCount,
            nextStrokeIndex = 0,
            strokesCompleted = emptyList(),
            correctOrderCount = 0,
            correctDirectionCount = 0,
            errorCount = 0,
            isComplete = character.strokeCount == 0,
        )
        sessions[state.sessionId] = state
        return state
    }

    override fun expectedStroke(sessionId: String): HanziStroke? {
        val state = sessions[sessionId] ?: return null
        val character = characters[state.characterId] ?: return null
        return character.strokes.getOrNull(state.nextStrokeIndex)
    }

    override fun recordStroke(sessionId: String, strokeIndex: Int, direction: StrokeDirection): EngineStrokeFeedback {
        val state = sessions[sessionId] ?: return EngineStrokeFeedback(
            sessionId = sessionId,
            strokeIndex = strokeIndex,
            expectedType = StrokeType.HORIZONTAL,
            expectedDirection = StrokeDirection.LEFT_TO_RIGHT,
            message = "Session not found",
        )
        val character = characters[state.characterId] ?: return EngineStrokeFeedback(
            sessionId = sessionId,
            strokeIndex = strokeIndex,
            expectedType = StrokeType.HORIZONTAL,
            expectedDirection = StrokeDirection.LEFT_TO_RIGHT,
            message = "Character not found",
        )
        val expected = character.strokes.getOrNull(state.nextStrokeIndex)
        val expectedType = expected?.type ?: StrokeType.HORIZONTAL
        val expectedDirection = expected?.direction ?: StrokeDirection.LEFT_TO_RIGHT

        val isExpectedIndex = strokeIndex == state.nextStrokeIndex
        val wasOrderCorrect = isExpectedIndex
        val wasDirectionCorrect = direction == expectedDirection
        val wasCorrect = wasOrderCorrect && wasDirectionCorrect

        val feedback = EngineStrokeFeedback(
            sessionId = sessionId,
            strokeIndex = strokeIndex,
            expectedType = expectedType,
            expectedDirection = expectedDirection,
            receivedDirection = direction,
            wasOrderCorrect = wasOrderCorrect,
            wasDirectionCorrect = wasDirectionCorrect,
            wasCorrect = wasCorrect,
            message = buildMessage(expectedType, expectedDirection, wasOrderCorrect, wasDirectionCorrect),
        )

        if (wasCorrect) {
            sessions[sessionId] = state.copy(
                nextStrokeIndex = state.nextStrokeIndex + 1,
                strokesCompleted = state.strokesCompleted + strokeIndex,
                correctOrderCount = state.correctOrderCount + 1,
                correctDirectionCount = state.correctDirectionCount + 1,
                errorCount = state.errorCount,
                isComplete = state.nextStrokeIndex + 1 >= state.expectedStrokeCount,
            )
        } else {
            sessions[sessionId] = state.copy(
                correctOrderCount = state.correctOrderCount + if (wasOrderCorrect) 1 else 0,
                correctDirectionCount = state.correctDirectionCount + if (wasDirectionCorrect) 1 else 0,
                errorCount = state.errorCount + 1,
            )
        }

        return feedback
    }

    override fun isComplete(sessionId: String): Boolean {
        return sessions[sessionId]?.isComplete ?: false
    }

    override fun progress(sessionId: String): Float {
        return sessions[sessionId]?.progress ?: 0f
    }

    override fun correctOrderCount(sessionId: String): Int {
        return sessions[sessionId]?.correctOrderCount ?: 0
    }

    override fun correctDirectionCount(sessionId: String): Int {
        return sessions[sessionId]?.correctDirectionCount ?: 0
    }

    override fun reset(sessionId: String) {
        val state = sessions[sessionId] ?: return
        sessions[sessionId] = state.copy(
            nextStrokeIndex = 0,
            strokesCompleted = emptyList(),
            correctOrderCount = 0,
            correctDirectionCount = 0,
            errorCount = 0,
            isComplete = false,
        )
    }

    override fun endSession(sessionId: String) {
        val characterId = sessions[sessionId]?.characterId
        sessions.remove(sessionId)
        if (characterId != null) characters.remove(characterId)
    }

    override fun getEngineInfo(): WritingEngineInfo {
        return WritingEngineInfo(
            name = name,
            version = "1.0",
            supportedTypes = WritingExerciseType.entries,
            supportsStrokeAnimation = true,
        )
    }

    private fun buildMessage(
        expectedType: StrokeType,
        expectedDirection: StrokeDirection,
        wasOrderCorrect: Boolean,
        wasDirectionCorrect: Boolean,
    ): String {
        return when {
            !wasOrderCorrect -> "笔顺不对，先写第 ${expectedType.displayName} 之后再写这一笔"
            !wasDirectionCorrect -> "方向不对，${expectedType.displayNameCn}（${expectedDirection.displayName}）"
            else -> "写对了！${expectedType.displayNameCn}（${expectedDirection.displayName}）"
        }
    }
}
