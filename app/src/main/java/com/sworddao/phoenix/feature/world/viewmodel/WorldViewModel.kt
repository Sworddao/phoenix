package com.sworddao.phoenix.feature.world.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.world.data.ExplorationProgress
import com.sworddao.phoenix.feature.world.data.RegionProgress
import com.sworddao.phoenix.feature.world.data.RegionStatus
import com.sworddao.phoenix.feature.world.data.WorldRegion
import com.sworddao.phoenix.feature.world.data.WorldResult
import com.sworddao.phoenix.feature.world.domain.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorldUiState(
    val regions: List<WorldRegion> = emptyList(),
    val currentRegion: WorldRegion? = null,
    val availableRegions: List<WorldRegion> = emptyList(),
    val unlockedRegions: List<WorldRegion> = emptyList(),
    val explorationProgress: ExplorationProgress = ExplorationProgress(),
    val selectedRegion: WorldRegion? = null,
    val regionProgress: Map<String, RegionProgress> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showTravelDialog: Boolean = false,
    val travelTarget: WorldRegion? = null,
    val showRegionDetail: Boolean = false,
    val newlyUnlockedRegions: List<String> = emptyList(),
)

@HiltViewModel
class WorldViewModel @Inject constructor(
    private val worldRepository: WorldRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorldUiState())
    val uiState: StateFlow<WorldUiState> = _uiState.asStateFlow()

    init {
        loadWorldData()
    }

    private fun loadWorldData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                combine(
                    worldRepository.getAllRegions(),
                    worldRepository.getCurrentRegion(),
                    worldRepository.getAvailableRegions(),
                    worldRepository.getUnlockedRegions(),
                    worldRepository.getExplorationProgress(),
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val regions = values[0] as List<WorldRegion>
                    val currentRegion = values[1] as WorldRegion?
                    val availableRegions = values[2] as List<WorldRegion>
                    val unlockedRegions = values[3] as List<WorldRegion>
                    val explorationProgress = values[4] as ExplorationProgress

                    WorldUiState(
                        regions = regions,
                        currentRegion = currentRegion,
                        availableRegions = availableRegions,
                        unlockedRegions = unlockedRegions,
                        explorationProgress = explorationProgress,
                        regionProgress = explorationProgress.regions,
                        isLoading = false,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load world data",
                )
            }
        }
    }

    fun selectRegion(region: WorldRegion) {
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

    fun startTravel(region: WorldRegion) {
        _uiState.value = _uiState.value.copy(
            showTravelDialog = true,
            travelTarget = region,
        )
    }

    fun confirmTravel(regionId: String) {
        viewModelScope.launch {
            when (val result = worldRepository.travelToRegion(regionId)) {
                is WorldResult.TravelStarted -> {
                    _uiState.value = _uiState.value.copy(
                        showTravelDialog = false,
                        travelTarget = null,
                        error = null,
                    )
                }
                is WorldResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        showTravelDialog = false,
                        travelTarget = null,
                        error = result.message,
                    )
                }
                else -> {}
            }
        }
    }

    fun cancelTravel() {
        _uiState.value = _uiState.value.copy(
            showTravelDialog = false,
            travelTarget = null,
        )
    }

    fun discoverLocation(locationId: String) {
        viewModelScope.launch {
            worldRepository.discoverLocation(locationId)
        }
    }

    fun collectItem(collectibleId: String) {
        viewModelScope.launch {
            worldRepository.collectItem(collectibleId)
        }
    }

    fun checkRegionUnlocks() {
        viewModelScope.launch {
            val newlyUnlocked = worldRepository.checkRegionUnlocks()
            if (newlyUnlocked.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    newlyUnlockedRegions = newlyUnlocked,
                )
            }
        }
    }

    fun dismissUnlockedNotification() {
        _uiState.value = _uiState.value.copy(
            newlyUnlockedRegions = emptyList(),
        )
    }

    fun completeRegion(regionId: String) {
        viewModelScope.launch {
            worldRepository.completeRegion(regionId)
            checkRegionUnlocks()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
