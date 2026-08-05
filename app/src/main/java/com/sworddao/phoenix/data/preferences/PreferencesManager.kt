package com.sworddao.phoenix.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sworddao.phoenix.data.model.AccessibilityPreferences
import com.sworddao.phoenix.data.model.ExperienceLevel
import com.sworddao.phoenix.data.model.LearningPace
import com.sworddao.phoenix.data.model.PlayerProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "phoenix_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val NATIVE_LANGUAGE = stringPreferencesKey("native_language")
        val EXPERIENCE_LEVEL = stringPreferencesKey("experience_level")
        val LEARNING_PACE = stringPreferencesKey("learning_pace")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DAD_MODE = booleanPreferencesKey("dad_mode")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val SLOW_AUDIO = booleanPreferencesKey("slow_audio")
        val SHOW_HANZI = booleanPreferencesKey("show_hanzi")
    }

    val playerProfile: Flow<PlayerProfile> = context.dataStore.data.map { preferences ->
        PlayerProfile(
            displayName = preferences[Keys.DISPLAY_NAME] ?: "",
            nativeLanguage = preferences[Keys.NATIVE_LANGUAGE] ?: "",
            experienceLevel = try {
                ExperienceLevel.valueOf(preferences[Keys.EXPERIENCE_LEVEL] ?: ExperienceLevel.BEGINNER.name)
            } catch (e: Exception) {
                ExperienceLevel.BEGINNER
            },
            learningPace = try {
                LearningPace.valueOf(preferences[Keys.LEARNING_PACE] ?: LearningPace.STANDARD.name)
            } catch (e: Exception) {
                LearningPace.STANDARD
            },
            isOnboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    val accessibilityPreferences: Flow<AccessibilityPreferences> = context.dataStore.data.map { preferences ->
        AccessibilityPreferences(
            dadMode = preferences[Keys.DAD_MODE] ?: false,
            reducedMotion = preferences[Keys.REDUCED_MOTION] ?: false,
            largeText = preferences[Keys.LARGE_TEXT] ?: false,
            highContrast = preferences[Keys.HIGH_CONTRAST] ?: false,
            slowAudio = preferences[Keys.SLOW_AUDIO] ?: false,
            showHanzi = preferences[Keys.SHOW_HANZI] ?: false
        )
    }

    suspend fun savePlayerProfile(profile: PlayerProfile) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DISPLAY_NAME] = profile.displayName
            preferences[Keys.NATIVE_LANGUAGE] = profile.nativeLanguage
            preferences[Keys.EXPERIENCE_LEVEL] = profile.experienceLevel.name
            preferences[Keys.LEARNING_PACE] = profile.learningPace.name
            preferences[Keys.ONBOARDING_COMPLETED] = profile.isOnboardingCompleted
        }
    }

    suspend fun saveAccessibilityPreferences(prefs: AccessibilityPreferences) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DAD_MODE] = prefs.dadMode
            preferences[Keys.REDUCED_MOTION] = prefs.reducedMotion
            preferences[Keys.LARGE_TEXT] = prefs.largeText
            preferences[Keys.HIGH_CONTRAST] = prefs.highContrast
            preferences[Keys.SLOW_AUDIO] = prefs.slowAudio
            preferences[Keys.SHOW_HANZI] = prefs.showHanzi
        }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}
