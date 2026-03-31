package com.rudy.stoic.ui.quicksettings

import androidx.lifecycle.ViewModel
import com.rudy.stoic.domain.model.QuickSettingTile
import com.rudy.stoic.domain.repository.QuickSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class QuickSettingsViewModel @Inject constructor(
    private val quickSettingsRepository: QuickSettingsRepository
) : ViewModel() {

    val tiles: StateFlow<List<QuickSettingTile>> = quickSettingsRepository.tiles

    fun toggleTile(tileId: String) {
        quickSettingsRepository.toggle(tileId)
    }

    fun refreshStates() {
        quickSettingsRepository.refreshStates()
    }
}
