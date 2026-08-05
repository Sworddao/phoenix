package com.sworddao.phoenix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.data.model.AccessibilityPreferences
import com.sworddao.phoenix.data.model.PlayerProfile
import com.sworddao.phoenix.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _playerProfile = MutableStateFlow(PlayerProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()

    private val _accessibilityPreferences = MutableStateFlow(AccessibilityPreferences())
    val accessibilityPreferences: StateFlow<AccessibilityPreferences> = _accessibilityPreferences.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.playerProfile.collect { profile ->
                _playerProfile.value = profile
            }
        }

        viewModelScope.launch {
            preferencesManager.accessibilityPreferences.collect { prefs ->
                _accessibilityPreferences.value = prefs
            }
        }
    }

    fun saveProfile(profile: PlayerProfile) {
        viewModelScope.launch {
            preferencesManager.savePlayerProfile(profile)
        }
    }

    fun saveAccessibilityPreferences(prefs: AccessibilityPreferences) {
        viewModelScope.launch {
            preferencesManager.saveAccessibilityPreferences(prefs)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.completeOnboarding()
        }
    }
}
