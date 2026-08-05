package com.sworddao.phoenix.data.model

import kotlinx.serialization.Serializable

enum class ExperienceLevel {
    BEGINNER,
    SOME_MANDARIN,
    INTERMEDIATE
}

enum class LearningPace {
    RELAXED,
    STANDARD,
    INTENSIVE
}

@Serializable
data class PlayerProfile(
    val displayName: String = "",
    val nativeLanguage: String = "",
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val learningPace: LearningPace = LearningPace.STANDARD,
    val isOnboardingCompleted: Boolean = false
)

@Serializable
data class AccessibilityPreferences(
    val dadMode: Boolean = false,
    val reducedMotion: Boolean = false,
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val slowAudio: Boolean = false,
    val showHanzi: Boolean = false
)
