package com.rudy.stoic.domain.repository

import com.rudy.stoic.domain.model.QuickSettingTile
import kotlinx.coroutines.flow.StateFlow

interface QuickSettingsRepository {
    val tiles: StateFlow<List<QuickSettingTile>>
    fun refreshStates()
    fun toggle(tileId: String): Boolean
}
