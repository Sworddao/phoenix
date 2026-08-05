package com.sworddao.phoenix

import com.sworddao.phoenix.data.model.AccessibilityPreferences
import com.sworddao.phoenix.data.model.ExperienceLevel
import com.sworddao.phoenix.data.model.LearningPace
import com.sworddao.phoenix.data.model.PlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerModelsTest {

    @Test
    fun `PlayerProfile default values are correct`() {
        val profile = PlayerProfile()
        assertEquals("", profile.displayName)
        assertEquals("", profile.nativeLanguage)
        assertEquals(ExperienceLevel.BEGINNER, profile.experienceLevel)
        assertEquals(LearningPace.STANDARD, profile.learningPace)
        assertFalse(profile.isOnboardingCompleted)
    }

    @Test
    fun `PlayerProfile with custom values`() {
        val profile = PlayerProfile(
            displayName = "TestUser",
            nativeLanguage = "English",
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            learningPace = LearningPace.RELAXED,
            isOnboardingCompleted = true
        )
        assertEquals("TestUser", profile.displayName)
        assertEquals("English", profile.nativeLanguage)
        assertEquals(ExperienceLevel.INTERMEDIATE, profile.experienceLevel)
        assertEquals(LearningPace.RELAXED, profile.learningPace)
        assertTrue(profile.isOnboardingCompleted)
    }

    @Test
    fun `AccessibilityPreferences default values are correct`() {
        val prefs = AccessibilityPreferences()
        assertFalse(prefs.dadMode)
        assertFalse(prefs.reducedMotion)
        assertFalse(prefs.largeText)
        assertFalse(prefs.highContrast)
        assertFalse(prefs.slowAudio)
        assertFalse(prefs.showHanzi)
    }

    @Test
    fun `AccessibilityPreferences with custom values`() {
        val prefs = AccessibilityPreferences(
            dadMode = true,
            reducedMotion = true,
            largeText = true,
            highContrast = false,
            slowAudio = true,
            showHanzi = true
        )
        assertTrue(prefs.dadMode)
        assertTrue(prefs.reducedMotion)
        assertTrue(prefs.largeText)
        assertFalse(prefs.highContrast)
        assertTrue(prefs.slowAudio)
        assertTrue(prefs.showHanzi)
    }

    @Test
    fun `ExperienceLevel has correct entries`() {
        val entries = ExperienceLevel.entries
        assertEquals(3, entries.size)
        assertTrue(entries.contains(ExperienceLevel.BEGINNER))
        assertTrue(entries.contains(ExperienceLevel.SOME_MANDARIN))
        assertTrue(entries.contains(ExperienceLevel.INTERMEDIATE))
    }

    @Test
    fun `LearningPace has correct entries`() {
        val entries = LearningPace.entries
        assertEquals(3, entries.size)
        assertTrue(entries.contains(LearningPace.RELAXED))
        assertTrue(entries.contains(LearningPace.STANDARD))
        assertTrue(entries.contains(LearningPace.INTENSIVE))
    }
}
