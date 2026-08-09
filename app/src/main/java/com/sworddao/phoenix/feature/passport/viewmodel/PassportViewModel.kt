package com.sworddao.phoenix.feature.passport.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.passport.data.AchievementProgress
import com.sworddao.phoenix.feature.passport.data.Collectible
import com.sworddao.phoenix.feature.passport.data.CollectibleCategory
import com.sworddao.phoenix.feature.passport.data.CollectionProgress
import com.sworddao.phoenix.feature.passport.data.DiscoveryEvent
import com.sworddao.phoenix.feature.passport.data.Passport
import com.sworddao.phoenix.feature.passport.data.PassportRegion
import com.sworddao.phoenix.feature.passport.data.PassportResult
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.passport.domain.PassportStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassportUiState(
    val passport: Passport = Passport(),
    val regions: List<PassportRegion> = emptyList(),
    val collectibles: List<Collectible> = emptyList(),
    val collectionProgress: CollectionProgress = CollectionProgress(),
    val timeline: List<DiscoveryEvent> = emptyList(),
    val achievements: List<AchievementProgress> = emptyList(),
    val stats: PassportStats? = null,
    val selectedRegion: PassportRegion? = null,
    val selectedCollectible: Collectible? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRegionDetail: Boolean = false,
    val showCollectibleDetail: Boolean = false,
    val showCollectionGrid: Boolean = false,
    val selectedCategory: CollectibleCategory? = null,
    val showAchievementUnlocked: Boolean = false,
    val unlockedAchievement: AchievementProgress? = null,
)

@HiltViewModel
class PassportViewModel @Inject constructor(
    private val passportRepository: PassportRepository,
    private val gameProgressRepository: GameProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    init {
        loadPassportData()
    }

    private fun loadPassportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                combine(
                    passportRepository.getPassport(),
                    passportRepository.getAllRegions(),
                    passportRepository.getCollectibles(),
                    passportRepository.getCollectionProgress(),
                    passportRepository.getDiscoveryTimeline(),
                    passportRepository.getAchievements(),
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val passport = values[0] as Passport
                    val regions = values[1] as List<PassportRegion>
                    val collectibles = values[2] as List<Collectible>
                    val collectionProgress = values[3] as CollectionProgress
                    val timeline = values[4] as List<DiscoveryEvent>
                    val achievements = values[5] as List<AchievementProgress>

                    val stats = passportRepository.getPassportStats()

                    PassportUiState(
                        passport = passport,
                        regions = regions,
                        collectibles = collectibles,
                        collectionProgress = collectionProgress,
                        timeline = timeline,
                        achievements = achievements,
                        stats = stats,
                        isLoading = false,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load passport data",
                )
            }
        }
    }

    fun selectRegion(region: PassportRegion) {
        _uiState.value = _uiState.value.copy(
            selectedRegion = region,
            showRegionDetail = true,
        )
    }

    fun clearSelectedRegion() {
        _uiState.value = _uiState.value.copy(
            selectedRegion = null,
            showRegionDetail = false,
        )
    }

    fun selectCollectible(collectible: Collectible) {
        _uiState.value = _uiState.value.copy(
            selectedCollectible = collectible,
            showCollectibleDetail = true,
        )
    }

    fun clearSelectedCollectible() {
        _uiState.value = _uiState.value.copy(
            selectedCollectible = null,
            showCollectibleDetail = false,
        )
    }

    fun showCollectionGrid(category: CollectibleCategory? = null) {
        _uiState.value = _uiState.value.copy(
            showCollectionGrid = true,
            selectedCategory = category,
        )
    }

    fun hideCollectionGrid() {
        _uiState.value = _uiState.value.copy(
            showCollectionGrid = false,
            selectedCategory = null,
        )
    }

    fun discoverRegion(regionId: String) {
        viewModelScope.launch {
            when (val result = passportRepository.discoverRegion(regionId)) {
                is PassportResult.Success -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is PassportResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun completeRegion(regionId: String) {
        viewModelScope.launch {
            when (val result = passportRepository.completeRegion(regionId)) {
                is PassportResult.RegionCompleted -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is PassportResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun earnStamp(regionId: String) {
        viewModelScope.launch {
            when (val result = passportRepository.earnStamp(regionId)) {
                is PassportResult.StampEarned -> {
                    gameProgressRepository.recordPassportStampEarned(regionId)
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is PassportResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun collectItem(collectibleId: String) {
        viewModelScope.launch {
            when (val result = passportRepository.collectItem(collectibleId)) {
                is PassportResult.CollectibleFound -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is PassportResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun checkAchievements() {
        viewModelScope.launch {
            val unlocked = passportRepository.checkAchievements()
            if (unlocked.isNotEmpty()) {
                val achievement = _uiState.value.achievements.find { it.id in unlocked }
                if (achievement != null) {
                    _uiState.value = _uiState.value.copy(
                        showAchievementUnlocked = true,
                        unlockedAchievement = achievement,
                    )
                }
            }
        }
    }

    fun dismissAchievementNotification() {
        _uiState.value = _uiState.value.copy(
            showAchievementUnlocked = false,
            unlockedAchievement = null,
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
