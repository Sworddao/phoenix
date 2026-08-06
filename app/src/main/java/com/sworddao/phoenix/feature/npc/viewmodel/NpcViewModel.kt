package com.sworddao.phoenix.feature.npc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.npc.data.Npc
import com.sworddao.phoenix.feature.npc.data.TimeOfDay
import com.sworddao.phoenix.feature.npc.domain.NpcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NpcUiState(
    val npcs: List<Npc> = emptyList(),
    val selectedNpc: Npc? = null,
    val currentLocation: String = "Qingyuan Village",
    val currentTimeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NpcViewModel @Inject constructor(
    private val npcRepository: NpcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NpcUiState())
    val uiState: StateFlow<NpcUiState> = _uiState.asStateFlow()

    init {
        loadNpcs()
    }

    private fun loadNpcs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            npcRepository.getAllNpcs()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { npcs ->
                    _uiState.value = _uiState.value.copy(
                        npcs = npcs,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun selectNpc(npc: Npc?) {
        _uiState.value = _uiState.value.copy(selectedNpc = npc)
    }

    fun dismissNpc() {
        _uiState.value = _uiState.value.copy(selectedNpc = null)
    }

    fun getNpcsForLocation(locationName: String): List<Npc> {
        return _uiState.value.npcs.filter { it.currentLocation == locationName }
    }

    fun getAvailableNpcs(): List<Npc> {
        return _uiState.value.npcs.filter {
            it.interactionAvailability == com.sworddao.phoenix.feature.npc.data.InteractionAvailability.AVAILABLE
        }
    }

    fun updateTimeOfDay(timeOfDay: TimeOfDay) {
        _uiState.value = _uiState.value.copy(currentTimeOfDay = timeOfDay)
    }
}
